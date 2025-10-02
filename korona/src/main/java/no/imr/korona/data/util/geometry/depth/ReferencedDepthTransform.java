package no.imr.korona.data.util.geometry.depth;

import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.ping.PingIndex;

/**
 * A depth transform using a reference depth.
 */
public abstract class ReferencedDepthTransform implements DepthTransform {
   private final DataConfiguration dataConfiguration;

   protected ReferencedDepthTransform(DataConfiguration dataConfiguration) {
      this.dataConfiguration = dataConfiguration;
   }

   protected abstract float getReferenceDepth(PingIndex pingIndex);

   protected float getSeabedMountedReferenceDepth(PingIndex pingIndex) {
      return getReferenceDepth(pingIndex);
   }

   protected DataConfiguration getDataConfiguration() {
      return dataConfiguration;
   }

   @Override
   public PerPingDepthTransform forPing(PingIndex pingIndex) {
      if (dataConfiguration.isSeabedMounted()) {
         return new PerPingSeabedMountedReferencedDepthTransform(getSeabedMountedReferenceDepth(pingIndex));
      } else {
         return new PerPingNormalReferencedDepthTransform(getReferenceDepth(pingIndex));
      }
   }
}
