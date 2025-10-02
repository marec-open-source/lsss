package no.imr.korona.data.datagrams.subdatagrams;

/**
 * The global list of ids for {@link DatagramSubType}.
 */
public final class DatagramSubTypeId {
   public static final int GRAPHICAL_INFO = 1;
   public static final int GRAPHICAL_INFO_TOC = 2;
   public static final int DATA_INCOHERENCE = 3;
   public static final int DABGRAF_TRACK_INFO = 4;
   public static final int EXTRA_RAW_FILE_CONFIGURATION = 5;
   public static final int PLOT_PARAMETER_CONFIG = 6;
   public static final int PLOT_PARAMETER_VALUE = 7;
   public static final int BOTTOM_RANGES = 8;
   public static final int PROMUS_EXCLUDED_PINGS_TOC = 9;
   public static final int TS = 10;
   public static final int ECHO_LINE = 11;

   private DatagramSubTypeId() {
   }
}
