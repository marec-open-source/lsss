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

   /// Tests whether the plugin should be enabled when the application configuration
   /// has no setting for it, typically on a new installation.
   /// The user can enable or disable the plugin in the plugin configuration.
   ///
   /// @return `true` if the plugin should be enabled by default
   public boolean isEnabledByDefault() {
      return false;
   }

   public abstract FeaturePlugin createPlugin(LSSS lsss);
}
