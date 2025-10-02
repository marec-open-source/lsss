package no.imr.korona.data.util.geometry.depth;

import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.ping.PingIndex;

public final class PelagicDepthTransform implements DepthTransform {
   private final DataConfiguration dataConfiguration;

   public PelagicDepthTransform(DataConfiguration dataConfiguration) {
      this.dataConfiguration = dataConfiguration;
   }

   @Override
   public boolean dependsOnPingIndex() {
      return false;
   }

   @Override
   public PerPingDepthTransform forPing(PingIndex pingIndex) {
      if (dataConfiguration.isSeabedMounted()) {
         return new PerPingSeabedMountedReferencedDepthTransform(dataConfiguration.getSeabedMountedDistanceToSurface());
      } else {
         return PerPingIdentityDepthTransform.INSTANCE;
      }
   }
}
