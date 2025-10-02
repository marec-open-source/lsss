package no.imr.korona.data.util.geometry.depth;

import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.ping.PingIndex;

/**
 * The depth transform that transform the coordinated bottom to a horizontal line at z = 0.
 * The relation between depth and z is:
 * <blockquote>{@code
 * z = depth - bottomDepth(PingIndex)
 * }</blockquote>
 */
public final class CoordinatedBottomDepthTransform extends BottomDepthTransform {
   private final DataManager dataManager;

   /**
    * Creates a new CoordinatedBottomDepthTransform.
    *
    * @param dataManager the data manager
    */
   public CoordinatedBottomDepthTransform(DataManager dataManager) {
      super(dataManager.getDataConfiguration());

      this.dataManager = dataManager;
   }

   @Override
   protected float getReferenceDepth(PingIndex pingIndex) {
      return dataManager.getDataFileSet().getCoordinatedDepth(pingIndex);
   }
}
