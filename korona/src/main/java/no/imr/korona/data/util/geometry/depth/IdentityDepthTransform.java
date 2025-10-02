package no.imr.korona.data.util.geometry.depth;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.tools.range.FloatRange;

/**
 * The identity depth transform: depth = z.
 */
public final class IdentityDepthTransform implements DepthTransform {
   public static final IdentityDepthTransform INSTANCE = new IdentityDepthTransform();

   private IdentityDepthTransform() {
   }

   @Override
   public boolean dependsOnPingIndex() {
      return false;
   }

   @Override
   public PerPingDepthTransform forPing(PingIndex pingIndex) {
      return PerPingIdentityDepthTransform.INSTANCE;
   }

   @Override
   public float depthToZ(float depth, PingIndex pingIndex) {
      return depth;
   }

   @Override
   public float depthToZ(EchogramPoint echogramPoint) {
      return echogramPoint.depth();
   }

   @Override
   public FloatRange depthToZ(FloatRange depthRange, PingIndex pingIndex) {
      return depthRange;
   }

   @Override
   public float zToDepth(float z, PingIndex pingIndex) {
      return z;
   }

   @Override
   public FloatRange zToDepth(FloatRange zRange, PingIndex pingIndex) {
      return zRange;
   }
}
