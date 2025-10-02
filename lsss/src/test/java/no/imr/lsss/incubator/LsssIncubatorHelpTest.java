package no.imr.lsss.incubator;

import no.imr.tools.help.HelpTestUtils;
import org.junit.jupiter.api.Test;

final class LsssIncubatorHelpTest {
   @Test
   void helpSet() {
      HelpTestUtils.checkHelpSet(LsssIncubatorHelp.HELP_SET);
   }
}
