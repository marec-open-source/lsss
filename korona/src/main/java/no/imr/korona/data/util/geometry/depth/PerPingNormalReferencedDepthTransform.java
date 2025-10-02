package no.imr.korona.data.util.geometry.depth;

public final class PerPingNormalReferencedDepthTransform implements PerPingDepthTransform {
   private final float referenceDepth;

   PerPingNormalReferencedDepthTransform(float referenceDepth) {
      this.referenceDepth = referenceDepth;
   }

   @Override
   public float depthToZ(float depth) {
      return depth - referenceDepth;
   }

   @Override
   public float zToDepth(float z) {
      return z + referenceDepth;
   }
}
