package no.imr.lsss.database.reports.hibernate;

/**
 * Count of distance-interval on surveys.
 */
public record DistCount(
      int frequency,
      short transceiver,
      short scatterType,
      float distanceInterval,
      long distCount) {
}
