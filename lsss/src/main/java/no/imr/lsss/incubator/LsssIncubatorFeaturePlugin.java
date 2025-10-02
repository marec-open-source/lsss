package no.imr.lsss.incubator;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.BaseSystemFeatureService;
import no.imr.lsss.incubator.modules.broadband.BroadbandByFrequencyModule;
import no.imr.lsss.incubator.modules.broadband.BroadbandPeakDetectionModule;
import no.imr.lsss.incubator.modules.categorization_analysis.CategorizationAnalysisModule;
import no.imr.lsss.modules.ModuleCollection;
import no.imr.lsss.modules.OnStartup;
import no.imr.lsss.modules.Where;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.help.HelpSystemHelpSet;
import no.imr.tools.parameter.Name;

public final class LsssIncubatorFeaturePlugin extends FeaturePlugin {
   LsssIncubatorFeaturePlugin(LsssIncubatorFeatureService service, LSSS lsss) {
      super(service, lsss);
   }

   @Override
   public String getMainPluginId() {
      return BaseSystemFeatureService.NAME.persistentName();
   }

   @Override
   public HelpSystemHelpSet getHelpSet() {
      return LsssIncubatorHelp.HELP_SET;
   }

   @Override
   public ModuleCollection<?> getModules() {
      ModuleCollection<LsssIncubatorFeaturePlugin> moduleCollection = new ModuleCollection<>(this);

      if (LsssIncubatorFeatureToggles.BROADBAND_PEAK_DETECTION) {
         moduleCollection.viewModules(Where.BOTTOM)
               .add(new Name("BroadbandPeakDetectionModule", "BB peak detection"),
                     "Detects noise frequencies in broadband data",
                     OnStartup.DISABLED, BroadbandPeakDetectionModule::new)
               .add(new Name("BroadbandByFrequencyModule", "BB Sv(ping, f)"),
                     "Displays a heatmap of Sv as a function of ping and frequency",
                     OnStartup.DISABLED, BroadbandByFrequencyModule::new);
      }
      if (LsssIncubatorFeatureToggles.CATEGORIZATION_ANALYSIS) {
         moduleCollection.viewModules(Where.BOTTOM)
               .add(new Name("CategorizationAnalysisModule", "Categorization analysis"),
                     "Compares KORONA categorization with manual interpretation",
                     OnStartup.DISABLED, CategorizationAnalysisModule::new);
      }

      return moduleCollection;
   }
}
