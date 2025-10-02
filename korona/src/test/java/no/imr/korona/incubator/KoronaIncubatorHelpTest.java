package no.imr.korona.incubator;

import no.imr.tools.help.HelpTestUtils;
import org.junit.jupiter.api.Test;

final class KoronaIncubatorHelpTest {
   @Test
   void helpSet() {
      HelpTestUtils.checkHelpSet(KoronaIncubatorHelp.HELP_SET);
   }
}
