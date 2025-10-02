package no.imr.korona.data.ping;

public record PingIndexCorrectionOptions(
      boolean updateSpeedBasedOnGeoPositions,
      boolean useNmeaVesselDistance
) {
   public PingIndexCorrectionOptions() {
      this(false, true);
   }
}
