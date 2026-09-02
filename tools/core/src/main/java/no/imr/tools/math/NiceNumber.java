package no.imr.tools.math;

/**
 * Generate "nice" number.
 */
public final class NiceNumber {
   private NiceNumber() {
   }

   /**
    * Find a "nice" number approximately equal to x.
    * Round the number if round=true, take ceiling if round=false
    * Interval is 1, 2, 5, 10
    *
    * @param x     value
    * @param round do round
    * @return nice number
    */
   public static double niceNumber(double x, boolean round) {
      if (x <= 0) {
         if (x == 0) {
            return 1;
         }
         throw new IllegalArgumentException("Negative value: " + x);
      }

      double exp = Math.floor(Math.log10(x)); /* exponent of x */
      if (exp >= 0) { // Two branches to avoid computing pow(10, negative exponent) which cannot be represented exactly
         double scale = Math.pow(10, exp); // NB: non-negative exponent
         double f = x / scale;            /* fractional part of x, between 1 and 10 */
         double nf = nice1to10(f, round); /* nice, rounded fraction */
         return nf * scale;
      } else {
         double scale = Math.pow(10, -exp); // NB: non-negative exponent
         double f = x * scale;            /* fractional part of x, between 1 and 10 */
         double nf = nice1to10(f, round); /* nice, rounded fraction */
         return nf / scale;
      }
   }

   public static double niceDegree(double x) {
      if (x < 10) return niceNumber(x, true);
      if (x < 17.5) return 15;
      if (x < 25) return 20;
      if (x < 45) return 30;
      if (x < 90) return 60;
      return 120;
   }

   public static double niceSecond(double x, boolean round) {
      if (x <= 1) {
         // less than 1 second
         return niceNumber(x, round);
      } else if (x <= 60) {
         // less than 1 minute
         return nice1to60(x, round);
      } else if (x <= 3600) {
         // less than 1 hour
         return 60 * nice1to60(x / 60, round);
      } else if (x <= 24 * 3600) {
         // less than one day
         return 3600 * nice1to24(x / 3600, round);
      } else {
         // more than one day
         return 86400 * niceNumber(x / 86400, round);
      }
   }

   private static double nice1to10(double f, boolean round) {
      if (round) {
         if (f < 1.5) return 1;
         if (f < 3.5) return 2;
         if (f < 7.5) return 5;
         return 10;
      } else {
         if (f <= 1) return 1;
         if (f <= 2) return 2;
         if (f <= 5) return 5;
         return 10;
      }
   }

   private static double nice1to60(double f, boolean round) {
      if (round) {
         if (f < 1.5) return 1;
         if (f < 3.5) return 2;
         if (f < 7.5) return 5;
         if (f < 12.5) return 10;
         if (f < 17.5) return 15;
         if (f < 25) return 20;
         if (f < 45) return 30;
         return 60;
      } else {
         if (f <= 1) return 1;
         if (f <= 2) return 2;
         if (f <= 5) return 5;
         if (f <= 10) return 10;
         if (f <= 15) return 15;
         if (f <= 20) return 20;
         if (f <= 30) return 30;
         return 60;
      }
   }

   private static double nice1to24(double f, boolean round) {
      if (round) {
         if (f < 1.5) return 1;
         if (f < 3) return 2;
         if (f < 5) return 4;
         if (f < 7) return 6;
         if (f < 10) return 8;
         if (f < 18) return 12;
         return 24;
      } else {
         if (f <= 1) return 1;
         if (f <= 2) return 2;
         if (f <= 4) return 4;
         if (f <= 6) return 6;
         if (f <= 8) return 8;
         if (f <= 12) return 12;
         return 24;
      }
   }
}
