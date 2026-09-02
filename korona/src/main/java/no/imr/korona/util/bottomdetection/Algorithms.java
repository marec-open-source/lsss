package no.imr.korona.util.bottomdetection;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.formats.missing.MissingBot0Datagram;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.Max;
import no.imr.tools.Min;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Algorithms {
   /**
    * The previous bottom found (used in the quality score for bottom candidates).
    */
   private float prevBottom;
   private boolean validBottom;
   private final float signalStrengthThreshold;
   private final float minBottomDepth;
   private final float maxBottomDepth;
   private final Map<Integer, Float> transducerRanges;
   private final DistanceToIndexFunc distanceToIndexFunc;
   private final IndexToDistanceFunc indexToDistanceFunc;
   private final DistanceToIndexFunc depthToIndexFunc;
   private final int preferredChannel;

   public Algorithms(DistanceToIndexFunc distanceToIndexFunc, IndexToDistanceFunc indexToDistanceFunc,
                     DistanceToIndexFunc depthToIndexFunc, float signalStrengthThreshold,
                     float minBottomDepth, float maxBottomDepth,
                     Map<Integer, Float> transducerRanges,
                     int preferredChannel) {
      // distanceToIndexFunc and indexToDistanceFunc should be inverse of each other. Distance is either range or depth.
      this.distanceToIndexFunc = distanceToIndexFunc;
      this.indexToDistanceFunc = indexToDistanceFunc;
      this.depthToIndexFunc = depthToIndexFunc;
      this.signalStrengthThreshold = signalStrengthThreshold;
      this.minBottomDepth = minBottomDepth;
      this.maxBottomDepth = maxBottomDepth;
      this.transducerRanges = transducerRanges;
      this.preferredChannel = preferredChannel;
   }

   private int getBeginIndex(PowerData powerData, float minRange) {
      return Max.of(0,
            powerData.rangeToSampleIndex(minRange),
            depthToIndexFunc.index(powerData, minBottomDepth));
   }

   private int getEndIndex(PowerData powerData) {
      return Min.of(powerData.getCount(),
            powerData.rangeToSampleIndex(transducerRanges.get(powerData.getChannel())),
            depthToIndexFunc.index(powerData, maxBottomDepth));
   }

   /**
    * Finds the peak value of a moving average view of a power data sv-array within an interval of +/- searchDistance from the start index.
    *
    * @param powerData      the power data
    * @param startIndex     a start sample index
    * @param stepLength     the length of the moving average window
    * @param searchDistance the distance from the start index to search for the peak
    * @return the index of the peak value
    */
   private static int getPeakIndex(PowerData powerData, int startIndex, int stepLength, float searchDistance) {
      int halfInterval = (int) Math.ceil(searchDistance / powerData.getSampleDistance());
      int maxIndex = Math.min(startIndex + halfInterval, powerData.getCount() - 1);
      int minIndex = Math.max(startIndex - halfInterval, 0);
      BackSteppingMovingAverage backSteppingMovingAverage = new BackSteppingMovingAverage(stepLength, powerData, maxIndex);
      double peakValue = backSteppingMovingAverage.currentValue();
      int peakIndex = maxIndex;
      while (backSteppingMovingAverage.getCurrentIndex() > minIndex) {
         double value = backSteppingMovingAverage.nextValue();
         if (value > peakValue) {
            peakValue = value;
            peakIndex = backSteppingMovingAverage.getCurrentIndex();
         }
      }
      return peakIndex;
   }

   /**
    * Always return an invalid zero-value bottom.
    *
    * @return the computed depth
    */
   public float findBottomByPelagic() {
      validBottom = false;
      return 0;
   }

   /**
    * Finds some bottom candidates and gives them points according to signal strength, distance from transducer,
    * and distance from previously found bottom.
    *
    * @param powerDatas an array of powerData from the same time step
    * @return the computed depth
    */
   public float findBottomByGradient(List<PowerData> powerDatas) {
      CandidateList[] bottomCandidates = new CandidateList[powerDatas.size()];
      // compute gradients and update candidates
      float pingDepth = powerDatas.getFirst().getMaxDepth();
      for (int i = 0; i < bottomCandidates.length; i++) {
         bottomCandidates[i] = new CandidateList(4);
         PowerData powerData = powerDatas.get(i);
         float sampleDistance = powerData.getSampleDistance();
         pingDepth = Math.min(pingDepth, powerData.getMaxDepth());
         float[] dbValues = powerData.getLogSv();
         //starting 20 m below transducer
         int startIndex = getBeginIndex(powerData, 20);
         int endIndex = getEndIndex(powerData);
         float prevGrad = 0;
         float maxGrad = 0;
         for (int j = startIndex; j < endIndex - 1; j++) {
            //compute gradient
            float grad = (dbValues[j + 1] - dbValues[j]) / sampleDistance;
            maxGrad = Math.max(maxGrad, grad);
            if (prevGrad > 0 && grad <= 0 && maxGrad > 20) {
               float maxDb = Math.max(dbValues[j], dbValues[j + 1]);
               bottomCandidates[i].update(maxDb, powerData.getSampleDepth(j));
               maxGrad = 0;
            }
            prevGrad = grad;
         }
         // Additional candidate around prevBottom.
         if (prevBottom != 0) {
            float maxValue = -10000;
            int maxIndex = 0;
            maxGrad = 0;
            int jMin = Math.max(powerData.depthToSampleIndex(prevBottom - 30), startIndex);
            int jMax = Math.min(powerData.depthToSampleIndex(prevBottom + 30), endIndex - 1);
            for (int j = jMin; j < jMax; j++) {
               float grad = (dbValues[j + 1] - dbValues[j]) / sampleDistance;
               maxGrad = Math.max(maxGrad, grad);
               float value = dbValues[j];
               if (maxValue < value) {
                  maxValue = value;
                  maxIndex = j;
               }
            }
            bottomCandidates[i].addCandidate(maxValue, powerData.getSampleDepth(maxIndex));
         }
      }
      // give points to candidates
      for (CandidateList bottomCandidate : bottomCandidates) {
         //get min/max depth
         float maxDepth = bottomCandidate.getDepth(0);
         float minDepth = bottomCandidate.getDepth(0);
         for (int jj = 1; jj < bottomCandidate.numberOfCandidates; jj++) {
            float depth = bottomCandidate.getDepth(jj);
            maxDepth = Math.max(maxDepth, depth);
            minDepth = Math.min(minDepth, depth);
         }
         for (int j = 0; j < bottomCandidate.numberOfCandidates; j++) {
            //only give points if signal strength is large enough
            if (bottomCandidate.getDb(j) > signalStrengthThreshold) {
               //points according to distance from transducer
               if (maxDepth != minDepth) {
                  bottomCandidate.setPoints(j, 4 * (bottomCandidate.getDepth(j) - minDepth) / (maxDepth - minDepth));
               }

               //points according to signal strength
               bottomCandidate.setPoints(j, 6 - 6 * (float) j / bottomCandidate.numberOfCandidates);

               //points according to distance from previous bottom
               if (prevBottom != 0 && validBottom) {
                  bottomCandidate.setPoints(j,
                        15 * (float) Math.exp(-Math.pow((bottomCandidate.getDepth(j) - prevBottom) / 30, 2)));
               }
            }
         }
      }
      // get candidate with max points among all frequencies
      float maxPoints = 0;
      float bottom = 0;
      float strength = 0;
      for (CandidateList bottomCandidate : bottomCandidates) {
         for (int j = 0; j < bottomCandidate.numberOfCandidates; j++) {
            if (bottomCandidate.getPoints(j) > maxPoints) {
               maxPoints = bottomCandidate.getPoints(j);
               bottom = bottomCandidate.getDepth(j);
               strength = bottomCandidate.getDb(j);
            }
         }
      }
      // accept if quality-score is above a minimum
      //if (prevBottom != 0 && maxPoints < 2) bottom = prevBottom;
      if (maxPoints == 0 || strength < signalStrengthThreshold) {
         bottom = pingDepth;
         validBottom = false;
      } else {
         prevBottom = bottom;
         validBottom = true;
      }
      return bottom;
   }

   /**
    * Returns the depth where for the first time the signal strength is above a prespecified threshold.
    *
    * @param powerDatas an array of powerData from the same time step
    * @return the computed depth
    */
   public float findBottomByThreshold(List<PowerData> powerDatas) {
      boolean foundBottom = false;
      float maxDepth = powerDatas.getFirst().getMaxDepth();
      float bottom = 0;
      for (PowerData powerData : powerDatas) {
         maxDepth = Math.min(maxDepth, powerData.getMaxDepth());
         float[] dbValues = powerData.getLogSv();

         //starting 20 m below transducer
         int startIndex = getBeginIndex(powerData, 20);
         int endIndex = getEndIndex(powerData);

         float maxPower = signalStrengthThreshold;
         for (int j = startIndex; j < endIndex; j++) {
            float currentPower = dbValues[j];
            if (currentPower > maxPower) {
               bottom = Math.max(bottom, powerData.getSampleDepth(j));
               maxPower = currentPower;
               foundBottom = true;
            }
         }
      }
      if (!foundBottom) {
         bottom = maxDepth;
         validBottom = false;
      } else {
         validBottom = true;
      }
      return bottom;
   }

   /**
    * Returns the depth from an algorithm similar to the internal EK500 algorithm.
    *
    * @param powerDatas an array of powerData from the same time step
    * @return the computed depth
    */
   public float findBottomByEK500(List<PowerData> powerDatas) {
      if (powerDatas.isEmpty()) {
         validBottom = false;
         return 0;
      }

      float[] iBottom = new float[powerDatas.size()];

      for (int i = 0; i < powerDatas.size(); i++) {
         PowerData powerData = powerDatas.get(i);
         float[] dbValues = powerData.getLogSv();

         //starting 10 m below transducer
         int startIndex = getBeginIndex(powerData, 10);
         int endIndex = getEndIndex(powerData);
         float maxPower = signalStrengthThreshold;
         for (int j = startIndex; j < endIndex; j++) {
            float currentPower = dbValues[j];
            if (currentPower > maxPower) {
               iBottom[i] = Math.max(iBottom[i], indexToDistanceFunc.distance(powerData, j));
               maxPower = currentPower;
            }
         }
      }

      //find median among non-zero detections
      Arrays.sort(iBottom);
      int firstNonZeroIndex = 0;
      for (float v : iBottom) {
         if (v == 0) {
            firstNonZeroIndex++;
         }
      }
      if (firstNonZeroIndex == iBottom.length) {
         validBottom = false;
         return 0;
      }

      float bottomMax = Max.of(iBottom);

      // Shallowest bottom-candidate in the vicinity above deepest bottom: avoid integrating bottom
      float bottomMin = bottomMax;
      for (int i = 0; i < powerDatas.size(); i++) {
         if (iBottom[i] >= bottomMax - 10 && iBottom[i] >= bottomMax * 0.99) {
            bottomMin = Math.min(bottomMin, iBottom[i]);
         }
      }

      validBottom = true;
      return bottomMin;
   }

   public float findCoordinatedBottomFromEchosounder(List<PowerData> powerDatas, Bot0Datagram bot0Datagram, float minimumDepthThresholdFactor, float minimumDepthThresholdDistance,
                                                     int channelCount) {
      if (powerDatas.isEmpty() || bot0Datagram instanceof MissingBot0Datagram) {
         validBottom = false;
         return 0;
      }
      float[] backstepDepths = new float[channelCount];
      for (PowerData powerData : powerDatas) {
         backstepDepths[powerData.getChannel() - 1] = (float) bot0Datagram.getChannelDepths()[powerData.getChannel() - 1];
      }
      CoordinatedDepths coordinatedDepths = findCoordinatedDepths(powerDatas, backstepDepths, minimumDepthThresholdFactor, minimumDepthThresholdDistance);
      validBottom = true;
      return coordinatedDepths.minDepth;
   }

   private CoordinatedDepths findCoordinatedDepths(List<PowerData> powerDatas, float[] backstepDepths, float minimumDepthThresholdFactor, float minimumDepthThresholdDistance) {
      float maxDepth = 0;
      for (PowerData powerData : powerDatas) {
         if (transducerRanges.containsKey(powerData.getChannel())) {
            maxDepth = Math.max(maxDepth, backstepDepths[powerData.getChannel() - 1]);
         }
      }
      float cutoff = Math.max(minimumDepthThresholdFactor * maxDepth, maxDepth - minimumDepthThresholdDistance);
      float minDepth = maxDepth;
      for (PowerData powerData : powerDatas) {
         float depth = backstepDepths[powerData.getChannel() - 1];
         if (transducerRanges.containsKey(powerData.getChannel())) {
            if (depth >= cutoff) {
               minDepth = Math.min(minDepth, depth);
            }
         }
      }

      if (preferredChannel > 0 && transducerRanges.containsKey(preferredChannel)
            && backstepDepths[preferredChannel - 1] >= cutoff) {
         minDepth = backstepDepths[preferredChannel - 1];
      }
      return new CoordinatedDepths(maxDepth, minDepth);
   }


   private BackstepDepths findMinimumDepth(float bottomDepth, List<PowerData> powerDatas, int channelCount, float minDepthValueFraction,
                                           float minimumDepthThresholdFactor, float minimumDepthThresholdDistance) {
      int[] startInt = backstep(distanceToIndexFunc, _ -> bottomDepth, powerDatas, minDepthValueFraction);

      float[] backstepDepths = new float[channelCount];
      for (int i = 0; i < powerDatas.size(); i++) {
         backstepDepths[powerDatas.get(i).getChannel() - 1] = powerDatas.get(i).getSampleDepth(startInt[i]);
      }

      CoordinatedDepths coordinatedDepths = findCoordinatedDepths(powerDatas, backstepDepths, minimumDepthThresholdFactor, minimumDepthThresholdDistance);

      //set any undetected depths to minDepth
      for (int i = 0; i < backstepDepths.length; i++) {
         if (backstepDepths[i] == 0) {
            backstepDepths[i] = coordinatedDepths.minDepth;
         }
      }
      return new BackstepDepths(backstepDepths, coordinatedDepths.maxDepth, coordinatedDepths.minDepth);
   }

   private static int[] backstep(DistanceToIndexFunc distanceToIndexFunc, PowerDataToDistance powerDataToDistance, List<PowerData> powerDatas, float minDistanceValueFraction) {
      int[] startInt = new int[powerDatas.size()];
      for (int i = 0; i < powerDatas.size(); i++) {
         PowerData powerData = powerDatas.get(i);
         int index = distanceToIndexFunc.index(powerData, powerDataToDistance.getDistance(powerData));
         if (index > powerData.getCount()) {
            continue;
         }
         startInt[i] = index;
         if (startInt[i] <= 0) {
            continue;
         }

         int windowLength = 1;
         float sampleDistance = powerData.getSampleDistance();
         if (sampleDistance < 0.1) {
            // Adjust moving average window length to be about 0.2 m.
            windowLength = (int) Math.ceil(0.2 / sampleDistance);
         }
         if (powerData.getCount() > 0) {
            //find the highest value within +/- 10 m of the startIndex
            startInt[i] = getPeakIndex(powerData, startInt[i], windowLength, 10);

            BackSteppingMovingAverage backSteppingMovingAverage = new BackSteppingMovingAverage(windowLength, powerData, startInt[i]);
            double bottomSv = backSteppingMovingAverage.currentValue();
            while (backSteppingMovingAverage.getCurrentIndex() > 0) {
               startInt[i]--;
               if (backSteppingMovingAverage.nextValue() < bottomSv * minDistanceValueFraction) {
                  break;
               }
            }
         }
      }
      return startInt;
   }

   public BackstepDepths backstepAndSetMinDepth(float bottomDepth, List<PowerData> powerDatas, int channelCount, float minDepthValueFraction,
                                                float minimumDepthThresholdFactor, float minimumDepthThresholdDistance) {
      if (!validBottom) {
         return new BackstepDepths(new float[channelCount], 0, 0);
      }
      return findMinimumDepth(bottomDepth, powerDatas, channelCount, minDepthValueFraction, minimumDepthThresholdFactor, minimumDepthThresholdDistance);
   }

   public BackstepRanges backstepRanges(Map<PowerData, Float> powerDataToRange, int channelCount, float minRangeValueFraction) {
      if (!validBottom) {
         return new BackstepRanges(new float[channelCount]);
      }
      List<PowerData> powerDatas = new ArrayList<>(powerDataToRange.keySet());
      int[] backstepIndices = backstep(distanceToIndexFunc, powerDataToRange::get, powerDatas, minRangeValueFraction);
      float[] channelRanges = new float[channelCount];
      int i = 0;
      for (PowerData powerData : powerDatas) {
         channelRanges[powerData.getChannel() - 1] = powerData.getSampleRange(backstepIndices[i]);
         i++;
      }
      return new BackstepRanges(channelRanges);
   }

   public boolean isValidBottom() {
      return validBottom;
   }

   /**
    * A list of bottom candidates, sorted by signal strength.
    */
   private static final class CandidateList {
      private final int numberOfCandidates;

      private static final class Candidate implements Comparable<CandidateList.Candidate> {
         private final float candidate;
         private final float dbValue;
         private float points;

         private Candidate() {
            candidate = 0;
            dbValue = -1000;
            points = 0;
         }

         private Candidate(float candidate, float dbValue) {
            this.candidate = candidate;
            this.dbValue = dbValue;
            points = 0;
         }

         @Override
         public int compareTo(CandidateList.Candidate other) {
            return Float.compare(other.dbValue, dbValue);
         }
      }

      private CandidateList.Candidate[] candidates;

      private CandidateList(int numberOfCandidates) {
         this.numberOfCandidates = numberOfCandidates;
         candidates = new CandidateList.Candidate[numberOfCandidates];
         for (int i = 0; i < candidates.length; i++) {
            candidates[i] = new CandidateList.Candidate();
         }
      }

      /**
       * Get the depth of candidate i.
       *
       * @param i the candidate number
       * @return the depth
       */
      private float getDepth(int i) {
         return candidates[i].candidate;
      }

      /**
       * Get the signal strength of candidate i.
       *
       * @param i the candidate number
       * @return the dB value
       */
      private float getDb(int i) {
         return candidates[i].dbValue;
      }

      /**
       * Get no of points for candidate i.
       *
       * @param i the candidate number
       * @return the number of points
       */
      private float getPoints(int i) {
         return candidates[i].points;
      }

      /**
       * Give points to candidate i.
       *
       * @param i      the candidate number
       * @param points a number of points to add
       */
      private void setPoints(int i, float points) {
         candidates[i].points += points;
      }

      /**
       * Update the candidate list, given a new candidate.
       *
       * @param dbValue a signal strength value in dB
       * @param depth   the depth
       */
      private void update(float dbValue, float depth) {
         CandidateList.Candidate[] tempCandidates = new CandidateList.Candidate[numberOfCandidates + 1];
         System.arraycopy(candidates, 0, tempCandidates, 0, numberOfCandidates);
         tempCandidates[numberOfCandidates] = new CandidateList.Candidate(depth, dbValue);
         Arrays.sort(tempCandidates);
         candidates = tempCandidates;
      }

      /**
       * Add a candidate to the end of the candidate list.
       *
       * @param dbValue a signal strength value in dB
       * @param depth   the depth
       */
      private void addCandidate(float dbValue, float depth) {
         candidates[numberOfCandidates - 1] = new CandidateList.Candidate(depth, dbValue);
         Arrays.sort(candidates);
      }
   }

   private static final class BackSteppingMovingAverage {
      private final int accumulationLength;
      private final float[] sv;
      private int currentIndex;
      private double currentSum;
      private int currentLength;

      private BackSteppingMovingAverage(int accumulationLength, PowerData powerData, int startIndex) {
         this.accumulationLength = accumulationLength;
         sv = powerData.getSv();
         currentIndex = startIndex;
         computeAverage();
      }

      private void computeAverage() {
         double sum = 0;
         int samples = 0;
         for (int i = currentIndex; i < Math.min(sv.length, currentIndex + accumulationLength); i++) {
            sum += sv[i];
            samples++;
         }
         currentSum = sum;
         currentLength = samples;
      }

      private double currentValue() {
         return currentLength > 0 ? currentSum / currentLength : 0;
      }

      private double nextValue() {
         currentIndex--;
         if (currentIndex < 0) {
            return 0;
         }
         double newSv = currentIndex < sv.length ? sv[currentIndex] : 0;
         if (currentLength < accumulationLength && currentIndex < sv.length) {
            currentLength++;
         }
         int purgeIndex = currentIndex + accumulationLength;
         double purgeSv = purgeIndex < sv.length ? sv[purgeIndex] : 0;
         currentSum += newSv - purgeSv;
         return currentValue();
      }

      private int getCurrentIndex() {
         return currentIndex;
      }
   }

   private record CoordinatedDepths(float maxDepth, float minDepth) {
   }

   public record BackstepDepths(float[] channelDepths, float depth, float minimumDepth) {
   }

   public record BackstepRanges(float[] channelRanges) {
   }

   @FunctionalInterface
   private interface PowerDataToDistance {
      float getDistance(PowerData powerData);
   }

   @FunctionalInterface
   public interface IndexToDistanceFunc {
      float distance(PowerData powerData, int i);
   }

   @FunctionalInterface
   public interface DistanceToIndexFunc {
      int index(PowerData powerData, float distance);
   }
}
