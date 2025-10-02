package no.imr.korona.data.util.geometry.depth;

import no.imr.tools.range.FloatRange;

public final class PerPingIdentityDepthTransform implements PerPingDepthTransform {
   public static final PerPingIdentityDepthTransform INSTANCE = new PerPingIdentityDepthTransform();

   private PerPingIdentityDepthTransform() {
   }

   @Override
   public float depthToZ(float depth) {
      return depth;
   }

   @Override
   public FloatRange depthToZ(FloatRange depthRange) {
      return depthRange;
   }

   @Override
   public float zToDepth(float z) {
      return z;
   }

   @Override
   public FloatRange zToDepth(FloatRange zRange) {
      return zRange;
   }
}
