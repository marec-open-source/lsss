package no.imr.lsss.framework.extensions;

import no.imr.lsss.LSSS;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.plugins.FeatureService;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.svg.SvgIcon;
import no.marec.lsss.api.LsssPluginLoader;
import org.jspecify.annotations.Nullable;

import java.net.URL;

final class ExtensionFeatureService extends FeatureService {
   private final LsssPluginLoader lsssPluginLoader;
   private final @Nullable SvgIcon icon;

   ExtensionFeatureService(LsssPluginLoader lsssPluginLoader) {
      super(new Name(lsssPluginLoader.getId(), lsssPluginLoader.getLabel()));

      this.lsssPluginLoader = lsssPluginLoader;
      String iconResource = lsssPluginLoader.getIconResource();
      URL url = iconResource != null ? lsssPluginLoader.getClass().getClassLoader().getResource(iconResource) : null;
      icon = url != null ? SvgIcon.of(url.toString()) : null;
   }

   @Override
   public @Nullable SvgIcon getIcon() {
      return icon;
   }

   @Override
   public FeaturePlugin createPlugin(LSSS lsss) {
      return new ExtensionFeaturePlugin(this, lsss, lsssPluginLoader);
   }
}
