package no.imr.korona.incubator;

import no.imr.korona.computation.ModuleCategory;
import no.imr.korona.computation.ModuleInfoCollector;
import no.imr.korona.computation.categorization.TrackCategorizationModule;
import no.imr.korona.plugins.ModulePlugin;
import no.imr.tools.help.HelpSystemHelpSet;
import no.imr.tools.parameter.Name;

import java.util.Set;

public final class KoronaIncubatorModulePlugin extends ModulePlugin {
   KoronaIncubatorModulePlugin() {
      super(new Name("KoronaIncubator", "KORONA incubator"));
   }

   @Override
   public HelpSystemHelpSet getHelpSet() {
      return KoronaIncubatorHelp.HELP_SET;
   }

   @Override
   public void addModuleInfos(ModuleInfoCollector moduleInfoCollector) {
      if (KoronaIncubatorFeatureToggles.USE_TRACK_CATEGORIZATION) {
         moduleInfoCollector.group("Categorization")
               .add(TrackCategorizationModule.class, new Name("TrackCategorizationModule", "Track categorization"),
                     Set.of(ModuleCategory.ADDS_DATAGRAM),
                     "Categorizes tracks");
      }
   }
}
