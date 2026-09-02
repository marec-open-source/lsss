package no.imr.tools.plot;

import no.imr.tools.math.MathUtils;

import java.util.function.DoubleUnaryOperator;

@FunctionalInterface
public interface ExportTransform extends DoubleUnaryOperator {

   static ExportTransform identity() {
      return value -> value;
   }

   static ExportTransform round(double roundingFactor) {
      return value -> MathUtils.round(value, roundingFactor);
   }
}
