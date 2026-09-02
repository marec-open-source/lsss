package no.imr.korona.computation.plankton;

import no.imr.korona.data.datagrams.Cad0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.util.ResampledFloatArray;
import no.imr.tools.Utils;
import no.imr.tools.time.TimeUtils;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

final class PingSubSampler implements Iterable<PingSubSampler.SubSampledBin> {
   static final class SubSampledBin {
      private final float[] subSampledSvValues; //one value per channel
      private final boolean[] validSubSampledSvValues;
      private final float depth;
      private final int startSample;
      private final int endSample;
      private final boolean[][] validPoint;

      private SubSampledBin(float[] subSampledSvValues, boolean[] validSubSampledSvValues, float depth, int startSample, int endSample, boolean[][] validPoint) {
         this.subSampledSvValues = subSampledSvValues;
         this.validSubSampledSvValues = validSubSampledSvValues;
         this.startSample = startSample;
         this.endSample = endSample;
         this.depth = depth;
         this.validPoint = validPoint;
      }

      float getDepth() {
         return depth;
      }

      float[] getSubSampledSvValues() {
         return subSampledSvValues;
      }

      boolean[] getValidSubSampledSvValues() {
         return validSubSampledSvValues;
      }

      int getStartSample() {
         return startSample;
      }

      int getEndSample() {
         return endSample;
      }

      boolean isValid(int pingNo, int sampleNo) {
         return validPoint[pingNo][sampleNo - startSample];
      }

      @Override
      public String toString() {
         return "start: " + startSample + " end: " + endSample + " values: " + Arrays.toString(subSampledSvValues);
      }
   }

   private final class PingEnsembleIterator implements Iterator<SubSampledBin> {
      private @Nullable SubSampledBin next;
      private int currentDepthSample;

      private PingEnsembleIterator() {
         currentDepthSample = 0;
         next = subSampledValues(currentDepthSample);
      }

      @Override
      public boolean hasNext() {
         return next != null;
      }

      @Override
      public SubSampledBin next() {
         SubSampledBin result = next;
         if (result == null) {
            throw new NoSuchElementException();
         }

         currentDepthSample += depthSamplesPerBin;
         next = subSampledValues(currentDepthSample);

         return result;
      }
   }

   private final List<Ping> pings;
   private final int mainChannelNumber;
   private final Instant centerTime;
   private final @Nullable PowerData centerRaw;
   private final List<Map<Integer, ResampledFloatArray>> resampledSvArrays;
   private final RawFileConfiguration rawFileConfiguration;
   private final Set<Byte> excludedCategories;
   private final int depthSamplesPerBin;
   private final int maxDepthSample;
   private final Set<Integer> channelsToUse;
   private final int numActiveChannels;
   private final boolean useNoiseThreshold;
   private final float globalNoiseThresholdSv;
   private final int minInversionDepthSample;
   private final int maxInversionDepthSample;

   PingSubSampler(List<Ping> pings, int mainChannelNumber, RawFileConfiguration rawFileConfiguration,
                  Set<Byte> excludedCategories, int depthSamplesPerBin, Set<Integer> channelsToUse,
                  boolean useNoiseThreshold, float noiseThresholdDb,
                  boolean useMinInversionDepth, float minInversionDepth,
                  boolean useMaxInversionDepth, float maxInversionDepth) {
      this.mainChannelNumber = mainChannelNumber;
      this.channelsToUse = channelsToUse;
      this.pings = pings;
      centerTime = TimeUtils.interpolateInstant(pings.getFirst().getInstant(), pings.getLast().getInstant(), 0.5);
      //resample PowerData to the 38 kHZ sample interval if necessary
      resampledSvArrays = new ArrayList<>();
      this.rawFileConfiguration = rawFileConfiguration.makeCopy();
      numActiveChannels = channelsToUse.size();
      this.useNoiseThreshold = useNoiseThreshold;
      globalNoiseThresholdSv = PowerData.logSvToSv(noiseThresholdDb);

      PowerData closestToCenter = null;
      int maxInversionDepthSample = 99000;
      int minInversionDepthSample = 0;
      for (Ping ping : pings) {
         PowerData mainPowerData = getReferenceDatagram(ping, mainChannelNumber);
         resampledSvArrays.add(getResampledArrays(ping, mainPowerData, channelsToUse));
         if (mainPowerData != null) {
            if (closestToCenter == null
                  || Math.abs(centerTime.until(mainPowerData.getInstant(), ChronoUnit.NANOS))
                  < Math.abs(centerTime.until(closestToCenter.getInstant(), ChronoUnit.NANOS))
            ) {
               closestToCenter = mainPowerData;
            }
            if (useMinInversionDepth) {
               minInversionDepthSample = Math.max(minInversionDepthSample, mainPowerData.depthToSampleIndex(minInversionDepth));
            }
            if (useMaxInversionDepth) {
               maxInversionDepthSample = Math.min(maxInversionDepthSample, mainPowerData.depthToSampleIndex(maxInversionDepth));
            }
         }
      }
      centerRaw = closestToCenter;

      this.excludedCategories = excludedCategories;
      this.depthSamplesPerBin = depthSamplesPerBin;
      int maxDepthSample = -1;
      for (Map<Integer, ResampledFloatArray> map : resampledSvArrays) {
         for (ResampledFloatArray resampledFloatArray : map.values()) {
            maxDepthSample = Math.max(maxDepthSample, resampledFloatArray.getEndReferenceIndex());
         }
      }
      this.maxDepthSample = maxDepthSample;
      this.minInversionDepthSample = minInversionDepthSample;
      this.maxInversionDepthSample = maxInversionDepthSample;
   }

   private static @Nullable PowerData getReferenceDatagram(Ping ping, int mainChannel) {
      PowerData powerData = ping.getPowerData(mainChannel);
      return powerData != null ? powerData : ping.getFirstAvailablePowerData();
   }

   private static Map<Integer, ResampledFloatArray> getResampledArrays(Ping ping, @Nullable PowerData mainPowerData, Set<Integer> channelToUse) {
      if (mainPowerData == null) {
         return Map.of();
      }
      return ping.getNonNullPowerDatas()
            .filter(powerData -> channelToUse.contains(powerData.getChannel()))
            .collect(Collectors.toMap(
                  ChannelData::getChannel,
                  powerData -> ResampledFloatArray.create(powerData.getSv(), powerData, mainPowerData)));
   }

   @Override
   public Iterator<SubSampledBin> iterator() {
      return new PingEnsembleIterator();
   }

   Instant getCenterTime() {
      return centerTime;
   }

   private boolean excludedDepthSample(int depthSample) {
      return depthSample < minInversionDepthSample || depthSample > maxInversionDepthSample;
   }

   //todo more samples schools etc. from the subsampling?
   private @Nullable SubSampledBin subSampledValues(int depthSample) {
      if (centerRaw == null || depthSample >= maxDepthSample) {
         return null;
      }
      float[] subSampledArray = new float[numActiveChannels];
      boolean[][] validPoints = new boolean[pings.size()][depthSamplesPerBin];
      Utils.fill(validPoints, true);
      int[] samplesPerChannel = new int[numActiveChannels];
      int pingNo = 0;
      for (Ping ping : pings) {
         PowerData mainPowerData = getReferenceDatagram(ping, mainChannelNumber);
         Cad0Datagram cad0 = ping.getPingItem(Cad0Datagram.class);
         int endSample = depthSample + depthSamplesPerBin;
         for (int i = depthSample; i < endSample; i++) {
            float depth = mainPowerData != null ? mainPowerData.getSampleDepth(i) : 0;
            int cad0Index = cad0 != null ? cad0.depthToIndex(depth) : 0;
            if (cad0 != null && cad0Index < cad0.getPixelCount() && excludedCategories.contains(cad0.getBestCategory(cad0Index))
                  || excludedDepthSample(depthSample)) {
               validPoints[pingNo][i - depthSample] = false;
            }
         }
         for (int channel = 1; channel <= rawFileConfiguration.getTransducerCount(); channel++) {
            if (!channelsToUse.contains(channel)) {
               continue;
            }
            PowerData powerData = ping.getPowerData(channel);
            if (powerData == null) {
               continue;
            }
            ResampledFloatArray resampledFloatArray = resampledSvArrays.get(pingNo).get(channel);
            for (int i = depthSample; i < endSample; i++) {
               if (!validPoints[pingNo][i - depthSample]) {
                  continue;
               }
               float svValue = (i >= resampledFloatArray.getBeginReferenceIndex() && i < resampledFloatArray.getEndReferenceIndex())
                     ? resampledFloatArray.getValueForReferenceIndex(i)
                     : 0;
               if (useNoiseThreshold && svValue > 0 && svValue < globalNoiseThresholdSv) {
                  validPoints[pingNo][i - depthSample] = false;
               }
            }
         }
         pingNo++;
      }
      pingNo = 0;
      for (Ping ping : pings) {
         int channelIndex = 1;
         for (int channel = 1; channel <= rawFileConfiguration.getTransducerCount(); channel++) {
            if (!channelsToUse.contains(channel)) {
               continue;
            }
            float sumSv = 0;
            PowerData powerData = ping.getPowerData(channel);
            if (powerData == null) {
               continue;
            }
            ResampledFloatArray resampledFloatArray = resampledSvArrays.get(pingNo).get(channel);
            int endSample = depthSample + depthSamplesPerBin;
            for (int i = depthSample; i < endSample; i++) {
               if (!validPoints[pingNo][i - depthSample]) {
                  continue;
               }
               if (i >= resampledFloatArray.getBeginReferenceIndex() && i < resampledFloatArray.getEndReferenceIndex()) {
                  sumSv += resampledFloatArray.getValueForReferenceIndex(i);
                  samplesPerChannel[channelIndex - 1]++;
               }
            }
            subSampledArray[channelIndex - 1] += sumSv;
            channelIndex++;
         }
         pingNo++;
      }
      boolean[] validSubSample = new boolean[numActiveChannels];
      for (int i = 0; i < subSampledArray.length; i++) {
         if (samplesPerChannel[i] > 0) {
            subSampledArray[i] /= samplesPerChannel[i] * PowerData.IMR_CONSTANT;
            validSubSample[i] = true;
         } else {
            subSampledArray[i] = 0;
            validSubSample[i] = false;
         }
      }
      float depth = centerRaw.getSampleDepth(depthSample + depthSamplesPerBin / 2);
      return new SubSampledBin(subSampledArray, validSubSample, depth, depthSample, depthSample + depthSamplesPerBin, validPoints);
   }
}
