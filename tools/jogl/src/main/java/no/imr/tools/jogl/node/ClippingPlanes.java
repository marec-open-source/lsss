package no.imr.tools.jogl.node;

import no.imr.tools.range.FloatRange;

public record ClippingPlanes(FloatRange x, FloatRange y) {

   ClippingPlanes adjust(float width, float height) {
      float xSize = x.getSize();
      float ySize = y.getSize();
      float windowAspect = width / height;
      if (xSize / ySize > windowAspect) {
         float yCenter = y.fractionToValue(0.5f);
         return new ClippingPlanes(x, FloatRange.ofCenterAndSize(yCenter, xSize / windowAspect));
      } else {
         float xCenter = x.fractionToValue(0.5f);
         return new ClippingPlanes(FloatRange.ofCenterAndSize(xCenter, windowAspect * ySize), y);
      }
   }
}
