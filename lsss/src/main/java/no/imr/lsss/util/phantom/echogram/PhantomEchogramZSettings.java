package no.imr.lsss.util.phantom.echogram;

import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.framework.InterpretationZSettings;

public final class PhantomEchogramZSettings extends EchogramZSettings {
   private final PhantomEchogramSettings phantomEchogramSettings;

   PhantomEchogramZSettings(PhantomEchogramSettings phantomEchogramSettings) {
      super(phantomEchogramSettings.getPhantomDataFileSet().getDataConfiguration());

      this.phantomEchogramSettings = phantomEchogramSettings;
   }

   void reset() {
      float maxZ = (float) InterpretationZSettings.DEFAULT_MAX_DEPTH_FACTOR * phantomEchogramSettings.getPhantomDataFileSet().getMaxDepth();
      maxZ = Math.max(MIN_DELTA_Z, (float) Math.ceil(maxZ));
      setMaxZ(0, maxZ);
      zoomOut();
   }

   @Override
   public DepthTransform getDepthTransform() {
      return IdentityDepthTransform.INSTANCE;
   }

   public float yToDepth(float y) {
      // Assuming z == depth
      return yToZ(y);
   }

   public float depthToY(float depth) {
      // Assuming z == depth
      return zToY(depth);
   }
}
