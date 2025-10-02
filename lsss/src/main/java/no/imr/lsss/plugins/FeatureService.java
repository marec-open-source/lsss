package no.imr.lsss.plugins;

import no.imr.lsss.LSSS;
import no.imr.tools.parameter.Name;
import no.imr.tools.plugins.BaseService;
import no.imr.tools.swing.svg.SvgIcon;
import org.jspecify.annotations.Nullable;

/**
 * A service providing a {@link FeaturePlugin}.
 * <p>
 * Note that the service provider should be stateless.
 */
public abstract class FeatureService extends BaseService {
   protected FeatureService(Name name) {
      super(name);
   }

   public @Nullable SvgIcon getIcon() {
      return null;
   }

   public abstract FeaturePlugin createPlugin(LSSS lsss);
}
