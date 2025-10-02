package no.imr.lsss.util.phantom;

import no.imr.korona.data.datamanager.DataConfiguration;

public final class PhantomDataConfiguration extends DataConfiguration {
   public PhantomDataConfiguration() {
   }

   @Override
   public float getMinFrequencyForBottom() {
      return 0;
   }

   @Override
   public float getMaxFrequencyForBottom() {
      return Float.POSITIVE_INFINITY;
   }

   @Override
   public float getMinimumDepthThresholdFactor() {
      return 0.99f;
   }
}
