package no.imr.korona.computation.noise;

import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.math.Median;
import org.jspecify.annotations.Nullable;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Base class for different histogram types.
 */
abstract class BaseHistogram extends SubModuleWithLogging {
   /**
    * Limits for the intervals.
    */
   private float[] limits = Utils.EMPTY_FLOAT_ARRAY;

   /**
    * Number of hits in each interval.
    */
   private int[] hits = Utils.EMPTY_INT_ARRAY;

   /**
    * The probabilities calculated by calculateProbability.
    */
   private float[] probabilities = Utils.EMPTY_FLOAT_ARRAY;

   /**
    * Number of non-empty inputs.
    */
   private int numberOfInputs = 0;

   /**
    * Check for not calculating the probabilities when it isn't necessary.
    */
   private int numberOfInputsWhenCalculatingProbabilities = 0;

   // Other parameters:

   private float maxVal = 0;
   private boolean initialized = false;

   private final float lowLimit;

   static final int DEFAULT_INITIALIZATION_CELL_COUNT = 500;
   static final int DEFAULT_MAXIMUM_CELL_COUNT = 2000;
   static final int DEFAULT_INITIALIZATION_SAMPLE_COUNT = 1000;
   static final int DEFAULT_MINIMUM_SAMPLE_COUNT = 25000;
   static final boolean DEFAULT_SMOOTH = true;
   static final float DEFAULT_SMOOTH_FACTOR = 100;

   private int initializationCellCount = DEFAULT_INITIALIZATION_CELL_COUNT;
   private int maximumCellCount = DEFAULT_MAXIMUM_CELL_COUNT;
   private int initializationSampleCount = DEFAULT_INITIALIZATION_SAMPLE_COUNT;
   private int minimumSampleCount = DEFAULT_MINIMUM_SAMPLE_COUNT;
   private boolean smooth = DEFAULT_SMOOTH;
   private float smoothFactor = DEFAULT_SMOOTH_FACTOR;

   private int historySampleCount = 0;
   private long totalSampleCount = 0;
   private int historyCycles = 0;
   private final Deque<HistoryObject> history = new ArrayDeque<>();

   /**
    * HistoryObject.
    * For removing old input from the histogram,
    * and for calculating the median.
    */
   private static final class HistoryObject {
      private final long timeInMillis;
      private final float[] data;
      private final HistogramData.Quality quality;
      private int[] hits; // the histogram corresponding to the data

      private HistoryObject(HistogramData histogramData, int[] hits) {
         timeInMillis = histogramData.getPowerData().getTimeInMillis();
         data = histogramData.getData();
         quality = histogramData.getQuality();
         this.hits = hits;
      }
   }

   /**
    * Calculate the median by sorting all data contained in the history.
    *
    * @return the median
    */
   float findMedian() {
      int n = 0;
      for (HistoryObject historyObject : history) {
         n += historyObject.data.length;
      }
      float[] x = new float[n];
      int xi = 0;
      for (HistoryObject historyObject : history) {
         float[] data = historyObject.data;
         System.arraycopy(data, 0, x, xi, data.length);
         xi += data.length;
      }
      return Median.quickSelect(x);
   }

   /**
    * Constructs a histogram with a specified lower limit.
    *
    * @param lowLimit the specified lower limit
    */
   BaseHistogram(double lowLimit) {
      this.lowLimit = (float) lowLimit;
   }

   /**
    * Initializes this histogram cells given lower and upper limits and cell count.
    *
    * @param low       the lower limit
    * @param high      the upper limit
    * @param cellCount the cell count
    * @return an array of cell boundaries
    */
   abstract float[] makeLimits(float low, float high, int cellCount);

   /**
    * Expands this histogram given a new upper limit and a maximum cell count.
    * Existing cell limits are not changed.
    *
    * @param limits        the current limits
    * @param newUpperLimit the new upper limit
    * @param maxCellCount  the maximum allowed cell count
    * @return an array of cell boundaries
    */
   abstract float[] expandLimits(float[] limits, float newUpperLimit, int maxCellCount);

   /**
    * Add data to histogram.
    *
    * @param histogramData the new data to add
    */
   void input(@Nullable HistogramData histogramData) {
      if (histogramData == null || histogramData.isEmpty()) {
         return;
      }

      numberOfInputs++;

      float[] data = histogramData.getData();

      historySampleCount += data.length;
      totalSampleCount += data.length;

      if (totalSampleCount / minimumSampleCount > historyCycles) {
         historyCycles = (int) (totalSampleCount / minimumSampleCount);
         Log.global.finest(getLogLabel() + "Histogram history cycles = " + historyCycles);
      }

      for (float dataValue : data) {
         if (dataValue > maxVal) {
            maxVal = dataValue;
         }
      }

      if (!initialized) {
         if (historySampleCount >= initializationSampleCount && !history.isEmpty()) {
            initialize();
         } else {
            history.add(new HistoryObject(histogramData, Utils.EMPTY_INT_ARRAY));
            return;
         }
      }

      if (maxVal > limits[limits.length - 1] && hits.length < maximumCellCount) {
         setLimits(expandLimits(limits, maxVal, maximumCellCount));
         Log.global.finest(getLogLabel() + "Histogram expanding size: " + hits.length);
         if (hits.length == maximumCellCount) {
            Log.global.info(getLogLabel() + "Histogram reached max size: " + hits.length);
         }
      }

      int[] addHits = findHits(data);
      history.add(new HistoryObject(histogramData, addHits));
      for (int i = 0; i < addHits.length; i++) {
         hits[i] += addHits[i];
      }

      while (!history.isEmpty()) {
         HistoryObject historyObject = history.peek();
         int n = historyObject.data.length;
         if (historySampleCount - n < minimumSampleCount) {
            break;
         }
         historySampleCount -= n;
         int[] removeHits = historyObject.hits;
         for (int i = 0; i < removeHits.length; i++) {
            hits[i] -= removeHits[i];
         }
         history.remove();
      }
   }

   int getSampleCount() {
      return historySampleCount;
   }

   int getCellCount() {
      return hits.length;
   }

   float[] getProbabilities() {
      return probabilities;
   }

   /**
    * Returns the limits of the cells in this histogram.
    *
    * @return an array containing the limits of the cells in this histogram
    */
   float[] getLimits() {
      return limits;
   }

   private void setLimits(float[] limits) {
      this.limits = limits;
      hits = Arrays.copyOf(hits, limits.length - 1);
      //makeDiffusionWeights();
   }

   private void initialize() {
      if (initialized) {
         return;
      }
      if (history.isEmpty()) {
         return;
      }
      initialized = true;

      Log.global.finest(getLogLabel() + " initializing histogram");
      hits = new int[0];
      setLimits(makeLimits(lowLimit, findMedian(), initializationCellCount));
      setLimits(expandLimits(limits, maxVal, maximumCellCount));
      for (HistoryObject historyObject : history) {
         historyObject.hits = findHits(historyObject.data);
         int[] historyHits = historyObject.hits;
         for (int i = 0; i < historyHits.length; i++) {
            hits[i] += historyHits[i];
         }
      }
   }

   /**
    * Finds the histogram corresponding to one HistogramData.
    * This can be added and removed from the total histogram.
    *
    * @param x an array of values
    * @return an array containing the number of values in each cell
    */
   private int[] findHits(float[] x) {
      int[] newHits = new int[hits.length];
      for (float xValue : x) {
         int index = value2index(xValue);
         if (index >= 0) {
            newHits[index]++;
         }
      }
      return newHits;
   }

   /**
    * Conversion from value to index.
    *
    * @param value the value
    * @return the index, or -1 if outside bounds
    */
   private int value2index(float value) {
      int i0 = 0;
      int i1 = limits.length - 1;
      if (value < limits[i0] || value >= limits[i1]) {
         return -1;
      }
      while (i0 < i1 - 1) {
         int ii = (i0 + i1) >>> 1; // Overflow safe middle value
         if (value < limits[ii]) {
            i1 = ii;
         } else {
            i0 = ii;
         }
      }
      return i0;
   }

   /**
    * Finds the maximum probability.
    *
    * @return maximum f
    */
   float findMaximumProbability() {
      calculateProbability();
      int maxI = 0;
      for (int i = 0; i < probabilities.length; i++) {
         if (probabilities[i] > probabilities[maxI]) {
            maxI = i;
         }
      }
      return probabilities[maxI];
   }

   /**
    * Finds where the probability distribution has a global maximum.
    *
    * @return interval midpoint where f is maximum
    */
   float findMaximum() {
      calculateProbability();
      int maxI = 0;
      for (int i = 0; i < hits.length; i++) {
         if (probabilities[i] > probabilities[maxI]) {
            maxI = i;
         }
      }
      float whereMaximum = (limits[maxI] + limits[maxI + 1]) / 2;
      representativeValue = whereMaximum;
      return whereMaximum;
   }

   /**
    * Finds the probability for a given value.
    *
    * @param x the value
    * @return the probability
    */
   float f(float x) {
      calculateProbability();
      int i = value2index(x);
      if (i < 0) {
         return 0;
      }
      return probabilities[i];
   }

   /**
    * Finds the smallest value, greater than a given lower bound, with a given probability.
    *
    * @param prob       the probability
    * @param lowerBound the lower limit of the inverse
    * @return the smallest inverse, f<sup>-1</sup>(prob), larger than {@code lowerBound}
    * @throws HistogramException if an inverse could not be found
    */
   float findInverse(float prob, float lowerBound) throws HistogramException {
      calculateProbability();
      int iStart = value2index(lowerBound);
      if (iStart == -1) {
         iStart = 0;
      }
      for (int i = iStart; i < probabilities.length - 1; i++) {
         if ((probabilities[i] > prob) ^ (probabilities[i + 1] > prob)) {
            return limits[i + 1];
         }
      }
      throw new HistogramException("No inverse");
   }

   /**
    * Finds the quantile of a given fraction.
    *
    * @param fraction the fraction, between 0 and 1
    * @return the quantile corresponding to a given fraction
    */
   float findQuantile(double fraction) {
      calculateProbability();
      double sum = 0;
      for (int i = 0; i < probabilities.length; i++) {
         float dx = limits[i + 1] - limits[i];
         sum += probabilities[i] * dx;
         if (sum > fraction) {
            return (limits[i + 1] + limits[i]) / 2;
         }
      }
      Log.global.warning(getLogLabel() +
            "BaseHistogram.findQuantile: didn't get quantile");
      return limits[limits.length - 1];
   }

   /**
    * Finds the average below a given upper limit.
    *
    * @param upperLimit the upper limit
    * @return the average
    */
   float findAverage(float upperLimit) {
      double sum = 0;
      int n = 0;
      for (int i = 0; i < hits.length; i++) {
         double x = (limits[i] + limits[i + 1]) / 2;
         if (x > upperLimit) {
            break;
         }
         n += hits[i];
         sum += hits[i] * x;
      }
      return n == 0 ? 0 : (float) (sum / n);
   }

   private float representativeValue = -1;
   private float[] mL = Utils.EMPTY_FLOAT_ARRAY; // lower diagonal
   private float[] mD = Utils.EMPTY_FLOAT_ARRAY; // diagonal
   private float[] mU = Utils.EMPTY_FLOAT_ARRAY; // upper diagonal

   private void makeDiffusionWeights() {
      int n = hits.length;
      mL = new float[n];
      mD = new float[n];
      mU = new float[n];
      if (n == 1) {
         mD[0] = 1;
         return;
      }

      float[] x = new float[n];
      for (int i = 0; i < n; i++) {
         x[i] = (limits[i + 1] + limits[i]) / 2;
      }

      float[] dx = new float[n - 1];
      for (int i = 0; i < n - 1; i++) {
         dx[i] = x[i + 1] - x[i];
      }

      if (representativeValue < 0) {
         representativeValue = findMedian();
      }
      int mi = Math.min(value2index(representativeValue), dx.length - 1);
      float dxm;
      if (mi == -1) {
         // This can happen for a degenerated histogram
         dxm = representativeValue;
      } else {
         dxm = dx[mi];
      }
      float dt = smoothFactor * dxm * dxm;

      for (int i = 0; i < n; i++) {
         float dl = (i > 0) ? dx[i - 1] : dx[i];
         float du = (i < n - 1) ? dx[i] : dx[i - 1];
         mL[i] = -2 * dt / (dl * (dl + du));
         mD[i] = 1 + 2 * dt / (dl * du);
         mU[i] = -2 * dt / (du * (dl + du));
      }
      mL[0] = 0;
      mU[n - 1] = 0;
   }

   private void smoothProbabilities() {
      makeDiffusionWeights();

      int n = probabilities.length;
      float[] D = new float[n];
      System.arraycopy(mD, 0, D, 0, n);

      // forward elimination
      for (int i = 1; i < n; i++) {
         float q = -mL[i] / D[i - 1];
         D[i] += q * mU[i - 1];
         probabilities[i] += q * probabilities[i - 1];
      }

      // backwards substitution
      probabilities[n - 1] /= D[n - 1];
      for (int i = n - 2; i >= 0; i--) {
         probabilities[i] = (probabilities[i] - mU[i] * probabilities[i + 1]) / D[i];
      }

      // renormalize
      float sum = 0;
      for (int i = 1; i < n; i++) {
         sum += probabilities[i] * (limits[i + 1] - limits[i]);
      }
      ArrayMath.divide(probabilities, sum);
   }

   /**
    * Calculates probabilities from number of hits in each interval.
    */
   private void calculateProbability() {
      if (!initialized) {
         initialize();
      } else {
         if (numberOfInputs == numberOfInputsWhenCalculatingProbabilities) {
            return;
         }
      }
      numberOfInputsWhenCalculatingProbabilities = numberOfInputs;

      if (probabilities.length != hits.length) {
         probabilities = new float[hits.length];
      }

      int totalHits = 0;
      for (int hit : hits) {
         totalHits += hit;
      }
      totalHits = Math.max(1, totalHits);

      for (int i = 0; i < hits.length; i++) {
         float dx = limits[i + 1] - limits[i];
         probabilities[i] = hits[i] / (totalHits * dx);
      }

      if (smooth) {
         smoothProbabilities();
      }
   }

   /**
    * Prints each interval midpoint, number of hits and probability.
    *
    * @param out the stream to print to
    */
   void print(PrintWriter out) {
      calculateProbability();
      for (int i = 0; i < hits.length; i++) {
         float x = (limits[i] + limits[i + 1]) / 2;
         out.println(x + " " + hits[i] + " " + probabilities[i]);
      }
      out.flush();
   }

   /**
    * Returns whether this histogram is initialized.
    *
    * @return {@code true} if this histogram has been initialized,
    * {@code false} otherwise.
    */
   boolean isOK() {
      return initialized;
   }

   void setInitializationSampleCount(int initializationSampleCount) {
      this.initializationSampleCount = initializationSampleCount;
   }

   void setSmooth(boolean smooth) {
      this.smooth = smooth;
   }

   void setSmoothFactor(float smoothFactor) {
      this.smoothFactor = smoothFactor;
   }

   void setInitializationCellCount(int initializationCellCount) {
      this.initializationCellCount = initializationCellCount;
   }

   void setMinimumSampleCount(int minimumSampleCount) {
      this.minimumSampleCount = minimumSampleCount;
   }

   void setMaximumCellCount(int maximumCellCount) {
      this.maximumCellCount = maximumCellCount;
   }

   /**
    * Find the center in time between the oldest and newest inputs in the histogram.
    *
    * @return the center time in milliseconds or 0 if the histogram is empty
    */
   long getCenterTimeInMilli() {
      if (history.isEmpty()) {
         return 0;
      }
      long t0 = history.getFirst().timeInMillis;
      long t1 = history.getLast().timeInMillis;
      return (t0 + t1) / 2;
   }

   /**
    * Calculates the overall weighted quality of all the noise data in this histogram.
    *
    * @return the mean quality
    */
   float getOverallQuality() {
      double qualitySum = 0;
      for (HistoryObject historyObject : history) {
         int nSamples = historyObject.data.length;
         qualitySum += historyObject.quality.value * nSamples;
      }
      return (float) (qualitySum / historySampleCount);
   }
}
