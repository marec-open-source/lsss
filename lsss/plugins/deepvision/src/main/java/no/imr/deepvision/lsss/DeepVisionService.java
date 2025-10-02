package no.imr.deepvision.lsss;

import no.imr.deepvision.lsss.resources.DeepVisionResource;
import no.imr.lsss.LSSS;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.plugins.FeatureService;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.svg.SvgIcon;

public final class DeepVisionService extends FeatureService {
   public DeepVisionService() {
      super(new Name("DeepVision", "Deep Vision"));
   }

   @Override
   public SvgIcon getIcon() {
      return DeepVisionResource.DEEP_VISION;
   }

   @Override
   public FeaturePlugin createPlugin(LSSS lsss) {
      return new DeepVisionPlugin(this, lsss);
   }
}
