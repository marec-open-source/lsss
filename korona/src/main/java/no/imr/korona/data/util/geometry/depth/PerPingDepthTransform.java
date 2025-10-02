package no.imr.korona.data.util.geometry.depth;

import no.imr.tools.range.FloatRange;

public interface PerPingDepthTransform {
   float depthToZ(float depth);

   default FloatRange depthToZ(FloatRange depthRange) {
      if (depthRange.isEmpty()) {
         return FloatRange.EMPTY_RANGE;
      }
      return FloatRange.ofUnsorted(depthToZ(depthRange.min()), depthToZ(depthRange.max()));
   }

   float zToDepth(float z);

   default FloatRange zToDepth(FloatRange zRange) {
      if (zRange.isEmpty()) {
         return FloatRange.EMPTY_RANGE;
      }
      return FloatRange.ofUnsorted(zToDepth(zRange.min()), zToDepth(zRange.max()));
   }
}
