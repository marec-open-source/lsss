package no.imr.korona.data.ping.items.configuration;

public final class BeamType {
   public static final int SINGLE = 0;
   public static final int SPLIT = 0x1;
   public static final int REF = 0x2;
   public static final int REF_B = 0x4;
   public static final int SPLIT_3 = 0x11;      // 17
   public static final int SPLIT_2 = 0x21;      // 33
   public static final int SPLIT_3_C = 0x31;    // 49
   public static final int SPLIT_3_CN = 0x41;   // 65
   public static final int SPLIT_3_CW = 0x51;   // 81
   public static final int SPLIT_4_B = 0x61;    // 97
   public static final int ADCP_SINGLE = 0x100; // 256

   private BeamType() {
   }

   public static boolean isSplit(int beamType) {
      return switch (beamType) {
         case SPLIT,
              SPLIT_3,
              SPLIT_2,
              SPLIT_3_C,
              SPLIT_3_CN,
              SPLIT_3_CW,
              SPLIT_4_B -> true;

         default -> false;
      };
   }

   public static boolean has3Or3Plus1Sectors(int beamType) {
      return switch (beamType) {
         case SPLIT_3,
              SPLIT_3_C,
              SPLIT_3_CN,
              SPLIT_3_CW -> true;

         default -> false;
      };
   }
}
