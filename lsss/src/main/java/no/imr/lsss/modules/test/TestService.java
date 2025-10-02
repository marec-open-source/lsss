package no.imr.lsss.modules.test;

import no.imr.lsss.LSSS;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.plugins.FeatureService;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;

public final class TestService extends FeatureService {
   public TestService() {
      super(new Name("LsssTest", "LSSS test"));
   }

   @Override
   public boolean canBeUsed() {
      return Utils.useTestFeatures();
   }

   @Override
   public FeaturePlugin createPlugin(LSSS lsss) {
      return new TestPlugin(this, lsss);
   }
}
