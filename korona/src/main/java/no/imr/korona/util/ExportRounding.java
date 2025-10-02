package no.imr.korona.util;

import no.imr.tools.plot.ExportTransform;

public final class ExportRounding {
   private ExportRounding() {
   }

   public static ExportTransform absorption() {
      return ExportTransform.round(100_000);
   }

   public static ExportTransform db() {
      return ExportTransform.round(100);
   }

   public static ExportTransform degrees() {
      return ExportTransform.round(100);
   }

   public static ExportTransform depth() {
      return ExportTransform.round(1000);
   }

   public static ExportTransform geoPos() {
      return ExportTransform.round(1e8);
   }

   public static ExportTransform kHz() {
      return ExportTransform.round(1000);
   }

   public static ExportTransform sa() {
      return ExportTransform.round(100);
   }

   public static ExportTransform vesselDistance() {
      return ExportTransform.round(1_000_000);
   }
}
