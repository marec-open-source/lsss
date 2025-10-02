package no.imr.deepvision.lsss.resources;

import no.imr.tools.help.HelpTestUtils;
import org.junit.jupiter.api.Test;

final class DeepVisionHelpTest {
   @Test
   void helpSet() {
      HelpTestUtils.checkHelpSet(DeepVisionHelp.HELP_SET);
   }
}
