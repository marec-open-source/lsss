package no.imr.lsss.resources;

import no.imr.tools.help.HelpTestUtils;
import org.junit.jupiter.api.Test;

final class LsssHelpTest {
   @Test
   void helpSet() {
      HelpTestUtils.checkHelpSet(LsssHelp.HELP_SET);
   }
}
