package no.imr.lsss.modules.integration;

/**
 * One of the integration areas for {@link RegionIntegrationModule}.
 */
public enum IntegrationArea {
   /**
    * No vertical limits.
    */
   TOTAL,

   /**
    * Vertical limits from the pelagic echogram.
    */
   PELAGIC,

   /**
    * Vertical limits from the bottom echogram.
    */
   BOTTOM
}
