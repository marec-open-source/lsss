package no.imr.lsss.framework;

import no.imr.lsss.LSSS;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.plugins.FeatureService;
import no.imr.tools.parameter.Name;

/**
 * The LSSS base system feature service.
 */
public class BaseSystemFeatureService extends FeatureService {
   public static final Name NAME = new Name("BaseSystem");

   public BaseSystemFeatureService() {
      super(NAME);
   }

   @Override
   public FeaturePlugin createPlugin(LSSS lsss) {
      return new BaseSystemFeaturePlugin(this, lsss);
   }
}
