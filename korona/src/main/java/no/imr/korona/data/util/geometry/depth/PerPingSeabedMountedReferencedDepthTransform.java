package no.imr.korona.data.util.geometry.depth;

record PerPingSeabedMountedReferencedDepthTransform(
      float seabedMountedReferenceDepth
) implements PerPingDepthTransform {

   @Override
   public float depthToZ(float depth) {
      return seabedMountedReferenceDepth - depth;
   }

   @Override
   public float zToDepth(float z) {
      return seabedMountedReferenceDepth - z;
   }
}
