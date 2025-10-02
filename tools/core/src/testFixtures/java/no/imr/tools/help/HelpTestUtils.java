package no.imr.tools.help;

import static org.junit.jupiter.api.Assertions.*;

public final class HelpTestUtils {
   private HelpTestUtils() {
   }

   public static void checkHelpSet(HelpSystemHelpSet helpSet) {
      assertTrue(helpSet.getTopHelpID().isValid());
   }
}
