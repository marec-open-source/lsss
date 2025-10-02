package no.imr.korona.data.ping.items.configuration;

import java.util.Locale;

public final class PulseForm {
   public static final int NARROWBAND = 0;
   public static final int BROADBAND_LINEAR_UP = 1;
   public static final int BROADBAND_LINEAR_DOWN = 5;

   public static final String NARROWBAND_NAME = "cw";
   public static final String BROADBAND_LINEAR_UP_NAME = "fm_linear_up_sweep";
   public static final String BROADBAND_LINEAR_DOWN_NAME = "fm_linear_down_sweep";

   private PulseForm() {
   }

   public static int stringPulseFormToInt(String stingPulseForm) {
      try {
         return Integer.parseInt(stingPulseForm);
      } catch (NumberFormatException e) {
         return switch (stingPulseForm.toLowerCase(Locale.ENGLISH)) {
            case NARROWBAND_NAME -> NARROWBAND;
            case BROADBAND_LINEAR_UP_NAME -> BROADBAND_LINEAR_UP;
            case BROADBAND_LINEAR_DOWN_NAME -> BROADBAND_LINEAR_DOWN;
            default -> throw new IllegalArgumentException("Unknown pulse form: " + stingPulseForm);
         };
      }
   }

   public static String intPulseFormToString(int intPulseForm) {
      return switch (intPulseForm) {
         case NARROWBAND -> NARROWBAND_NAME;
         case BROADBAND_LINEAR_UP -> BROADBAND_LINEAR_UP_NAME;
         case BROADBAND_LINEAR_DOWN -> BROADBAND_LINEAR_DOWN_NAME;
         default -> throw new IllegalArgumentException("Unknown pulse form: " + intPulseForm);
      };
   }
}
