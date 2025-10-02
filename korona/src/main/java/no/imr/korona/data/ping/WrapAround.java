package no.imr.korona.data.ping;

/**
 * Represents vessel distance wrap around.
 * For example 9999.99 to 0.01
 */
public record WrapAround(
      PingIndex pingIndex,
      double vesselDistance
) {
   private static final double MIN_WRAP_AROUND_DISTANCE = 100;
   private static final double WRAP_AROUND_THRESHOLD = 0.1;

   public static boolean isWrapAround(double firstVesselDistance, double secondVesselDistance) {
      if (firstVesselDistance < secondVesselDistance) {
         return false;
      }
      double roundedDistance = roundToPowerOfTen(firstVesselDistance);
      return roundedDistance >= MIN_WRAP_AROUND_DISTANCE
            && Math.abs(firstVesselDistance - roundedDistance) < WRAP_AROUND_THRESHOLD
            && Math.abs(secondVesselDistance) < WRAP_AROUND_THRESHOLD;
   }

   public static double roundToPowerOfTen(double x) {
      return Math.pow(10, Math.round(Math.log10(x)));
   }
}
