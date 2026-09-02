package no.imr.korona.data.datamanager;

import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.range.FloatRange;

import java.util.List;
import java.util.stream.IntStream;

/**
 * The dependency of {@link DataManager} to configuration set elsewhere.
 */
public abstract class DataConfiguration {
   protected DataConfiguration() {
   }

   public boolean isDataLoadingCancelled() {
      return false;
   }

   public boolean isSeabedMounted() {
      return false;
   }

   public float getSeabedMountedDistanceToSeabed() {
      return 0;
   }

   public float getSeabedMountedDistanceToSurface() {
      return 0;
   }

   public float depthToPhysicalDepth(float depth) {
      if (isSeabedMounted()) {
         return getSeabedMountedDistanceToSurface() - depth;
      } else {
         return depth;
      }
   }

   public FloatRange depthRangeToPhysicalDepthRange(FloatRange depthRange) {
      if (isSeabedMounted()) {
         float distanceToSurface = getSeabedMountedDistanceToSurface();
         return FloatRange.of(distanceToSurface - depthRange.max(), distanceToSurface - depthRange.min());
      } else {
         return depthRange;
      }
   }

   public float physicalDepthToDepth(float physicalDepth) {
      if (isSeabedMounted()) {
         return getSeabedMountedDistanceToSurface() - physicalDepth;
      } else {
         return physicalDepth;
      }
   }

   public float getSeabedMountedSeabedPhysicalDepth() {
      return getSeabedMountedDistanceToSeabed() + getSeabedMountedDistanceToSurface();
   }

   public float getPreferredFrequencyForBottom() {
      return 0;
   }

   public abstract float getMinFrequencyForBottom();

   public abstract float getMaxFrequencyForBottom();

   public abstract float getMinimumDepthThresholdFactor();

   public float getMinimumDepthThresholdDistance() {
      return 10;
   }

   List<Integer> getChannelsForBottom(RawFileConfiguration rawFileConfiguration) {
      float minFrequency = getMinFrequencyForBottom();
      float maxFrequency = getMaxFrequencyForBottom();
      return IntStream.rangeClosed(1, rawFileConfiguration.getTransducerCount())
            .filter(channel -> {
               float frequency = rawFileConfiguration.getTransducers().get(channel - 1).getFrequency();
               return frequency >= minFrequency && frequency <= maxFrequency;
            })
            .boxed()
            .toList();
   }

   int getPreferredChannelForBottom(RawFileConfiguration rawFileConfiguration) {
      float frequency = getPreferredFrequencyForBottom();
      if (frequency < getMinFrequencyForBottom() || frequency > getMaxFrequencyForBottom()) {
         return -1;
      }
      return rawFileConfiguration.firstChannelClosestTo(frequency);
   }
}
