package no.imr.lsss.modules.echogram;

import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.depth.ReferencedDepthTransform;
import no.imr.korona.region.LayerManager;
import no.imr.tools.Utils;
import no.imr.tools.range.FloatRange;

/**
 * The depth transform that transform the bottom boundary to a horizontal line at z = 0.
 * The relation between depth and z is:
 * <blockquote><pre>
 * z = depth - bottomBoundaryDepth(PingIndex)
 * </pre></blockquote>
 */
public final class BottomBoundaryDepthTransform extends ReferencedDepthTransform {
   private final LayerManager layerManager;
   private final DataManager dataManager;
   private FrozenTransform frozenTransform;

   public BottomBoundaryDepthTransform(LayerManager layerManager, DataManager dataManager) {
      super(dataManager.getDataConfiguration());

      this.layerManager = layerManager;
      this.dataManager = dataManager;
      frozenTransform = new FrozenTransform(PingRange.EMPTY_RANGE);
   }

   public void freeze(PingRange pingRange) {
      frozenTransform = new FrozenTransform(pingRange);
   }

   @Override
   protected float getReferenceDepth(PingIndex pingIndex) {
      return frozenTransform.getReferenceDepth(pingIndex);
   }

   private final class FrozenTransform {
      private final PingRange pingRange;
      private final float[] depths;

      private FrozenTransform(PingRange pingRange) {
         this.pingRange = pingRange;

         if (pingRange.isEmpty()) {
            depths = Utils.EMPTY_FLOAT_ARRAY;
            return;
         }

         depths = new float[pingRange.getPingCount() + 1];
         int i = 0;
         for (PingIndex pingIndex : dataManager.getDataFileSet().getPingIndices(pingRange)) {
            depths[i++] = getBoundaryDepth(pingIndex);
         }
         depths[i] = getBoundaryDepth(pingRange.end());
      }

      private float getReferenceDepth(PingIndex pingIndex) {
         if (pingRange.containsIncludingEnd(pingIndex)) {
            return depths[(int) (pingIndex.getPingNumber() - pingRange.begin().getPingNumber())];
         } else {
            return getBoundaryDepth(pingIndex);
         }
      }

      private float getBoundaryDepth(PingIndex pingIndex) {
         boolean seabedMounted = dataManager.getDataConfiguration().isSeabedMounted();
         FloatRange boundaryDepthRange = layerManager.getBoundaryDepthRange(pingIndex);
         return seabedMounted ? boundaryDepthRange.min() : boundaryDepthRange.max();
      }
   }
}
