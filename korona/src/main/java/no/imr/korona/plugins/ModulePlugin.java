package no.imr.korona.plugins;

import no.imr.korona.computation.ModuleInfoCollector;
import no.imr.korona.computation.display.VisualizerModule;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.tools.help.HelpSystemHelpSet;
import no.imr.tools.parameter.Name;
import no.imr.tools.plugins.BasePlugin;
import org.jspecify.annotations.Nullable;

public abstract class ModulePlugin extends BasePlugin {
   protected ModulePlugin(Name name) {
      super(name);
   }

   public HelpSystemHelpSet getHelpSet() {
      return HelpSystemHelpSet.EMPTY;
   }

   public abstract void addModuleInfos(ModuleInfoCollector moduleInfoCollector);

   /**
    * Used if no visualizer modules are present in the module configuration.
    *
    * @param pingConfiguration        ping configuration
    * @param previousVisualizerModule the previous visualizer module returned by this plugin, or {@code null}
    * @return the default visualizer module, or {@code null}
    */
   public @Nullable VisualizerModule getDefaultVisualizerModule(PingConfiguration pingConfiguration, @Nullable VisualizerModule previousVisualizerModule) {
      return null;
   }

   public @Nullable String getModuleConfigurationSubDirName() {
      return null;
   }
}
