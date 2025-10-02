package no.imr.korona.resources;

import no.imr.tools.help.HelpTestUtils;
import org.junit.jupiter.api.Test;

final class KoronaHelpTest {
   @Test
   void helpSet() {
      HelpTestUtils.checkHelpSet(KoronaHelp.HELP_SET);
   }
}
