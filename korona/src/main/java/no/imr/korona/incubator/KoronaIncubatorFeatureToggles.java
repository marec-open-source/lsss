package no.imr.korona.incubator;

public final class KoronaIncubatorFeatureToggles {
   public static final boolean INCUBATOR_ENABLED = Boolean.parseBoolean(System.getProperty("no.marec.incubator"));

   public static final boolean PLOT_PARAMETERS = INCUBATOR_ENABLED;
   public static final boolean ADCP_NETCDF = INCUBATOR_ENABLED;
   public static final boolean NOISE_NH_CORRECTION = INCUBATOR_ENABLED;
   public static final boolean CHANNEL_REMOVAL_PING_ID = INCUBATOR_ENABLED;

   /**
    * See issue #613.
    */
   public static final boolean USE_TRACK_CATEGORIZATION = Boolean.parseBoolean(System.getProperty("UseTrackCategorization"));

   private KoronaIncubatorFeatureToggles() {
   }
}
