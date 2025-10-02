package no.imr.lsss.modules.test;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.BaseSystemFeatureService;
import no.imr.lsss.modules.ModuleCollection;
import no.imr.lsss.modules.OnStartup;
import no.imr.lsss.modules.Position;
import no.imr.lsss.modules.Where;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.parameter.Name;

public final class TestPlugin extends FeaturePlugin {
   TestPlugin(TestService service, LSSS lsss) {
      super(service, lsss);
   }

   @Override
   public String getMainPluginId() {
      return BaseSystemFeatureService.NAME.persistentName();
   }

   @Override
   public ModuleCollection<?> getModules() {
      ModuleCollection<TestPlugin> moduleCollection = new ModuleCollection<>(this);

      moduleCollection.viewModules(Where.BOTTOM)
            .add(new Name("StressModule", "Stress"),
                  "Performs random actions",
                  OnStartup.DISABLED, StressModule::new)
            .add(new Name("DebugModule", "Debug"),
                  "For various debugging",
                  OnStartup.DISABLED, DebugModule::new)
            .add(new Name("BottomResponseModule", "Bottom response"),
                  "Displays Sv around bottom",
                  OnStartup.DISABLED, BottomResponseModule::new)
            .add(new Name("TrackingExplorerModule", "Tracking explorer"),
                  "For exploring tracking",
                  OnStartup.DISABLED, TrackingExplorerModule::new);

      moduleCollection.overlays(EchogramModule.class)
            .add(new Name("OpeningAngleOverlay", "Opening angle"),
                  "Displays beam opening angle",
                  OnStartup.DISABLED, OpeningAngleOverlay::new,
                  Position.AFTER, "MaskingDisplayOverlay");

      return moduleCollection;
   }
}
