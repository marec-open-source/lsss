package no.imr.korona.data.util.geometry.depth;

import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.ping.PingIndex;

public abstract class BottomDepthTransform extends ReferencedDepthTransform {
   protected BottomDepthTransform(DataConfiguration dataConfiguration) {
      super(dataConfiguration);
   }

   @Override
   protected float getSeabedMountedReferenceDepth(PingIndex pingIndex) {
      return -getDataConfiguration().getSeabedMountedDistanceToSeabed();
   }
}
