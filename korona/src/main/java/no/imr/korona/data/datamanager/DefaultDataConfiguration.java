package no.imr.korona.data.datamanager;

public final class DefaultDataConfiguration extends DataConfiguration {
   public DefaultDataConfiguration() {
   }

   @Override
   public float getMinFrequencyForBottom() {
      return 10000;
   }

   @Override
   public float getMaxFrequencyForBottom() {
      return 200000;
   }

   @Override
   public float getMinimumDepthThresholdFactor() {
      return 0.99f;
   }
}
