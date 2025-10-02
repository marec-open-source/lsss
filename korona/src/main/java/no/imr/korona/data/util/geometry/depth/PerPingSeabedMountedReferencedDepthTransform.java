package no.imr.korona.data.util.geometry.depth;

public final class PerPingSeabedMountedReferencedDepthTransform implements PerPingDepthTransform {
   private final float seabedMountedReferenceDepth;

   PerPingSeabedMountedReferencedDepthTransform(float seabedMountedReferenceDepth) {
      this.seabedMountedReferenceDepth = seabedMountedReferenceDepth;
   }

   @Override
   public float depthToZ(float depth) {
      return seabedMountedReferenceDepth - depth;
   }

   @Override
   public float zToDepth(float z) {
      return seabedMountedReferenceDepth - z;
   }
}
