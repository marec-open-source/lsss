package no.imr.lsss.incubator;

import no.imr.korona.incubator.KoronaIncubatorFeatureToggles;
import no.imr.lsss.LSSS;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.plugins.FeatureService;
import no.imr.tools.parameter.Name;

public final class LsssIncubatorFeatureService extends FeatureService {
   public LsssIncubatorFeatureService() {
      super(new Name("LsssIncubator", "LSSS incubator"));
   }

   @Override
   public boolean canBeUsed() {
      return KoronaIncubatorFeatureToggles.INCUBATOR_ENABLED;
   }

   @Override
   public FeaturePlugin createPlugin(LSSS lsss) {
      return new LsssIncubatorFeaturePlugin(this, lsss);
   }
}
