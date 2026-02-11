package no.imr.korona.data.util.geometry.depth;

record PerPingNormalReferencedDepthTransform(
      float referenceDepth
) implements PerPingDepthTransform {

   @Override
   public float depthToZ(float depth) {
      return depth - referenceDepth;
   }

   @Override
   public float zToDepth(float z) {
      return z + referenceDepth;
   }
}
