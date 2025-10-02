package no.imr.tools.plot;

import no.imr.tools.Utils;

import java.util.function.DoubleUnaryOperator;

@FunctionalInterface
public interface ExportTransform extends DoubleUnaryOperator {

   static ExportTransform identity() {
      return value -> value;
   }

   static ExportTransform round(double roundingFactor) {
      return value -> Utils.round(value, roundingFactor);
   }
}
