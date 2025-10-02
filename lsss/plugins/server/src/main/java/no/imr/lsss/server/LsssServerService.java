package no.imr.lsss.server;

import no.imr.lsss.LSSS;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.plugins.FeatureService;
import no.imr.lsss.resources.LsssIcons;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.svg.SvgIcon;

public final class LsssServerService extends FeatureService {
   public LsssServerService() {
      super(new Name("LsssServer", "LSSS server"));
   }

   @Override
   public SvgIcon getIcon() {
      return LsssIcons.LSSS_SERVER;
   }

   @Override
   public FeaturePlugin createPlugin(LSSS lsss) {
      return new LsssServerPlugin(this, lsss);
   }
}
