package no.imr.korona.incubator;

import no.imr.korona.plugins.ModulePlugin;
import no.imr.korona.plugins.ModuleService;
import no.imr.tools.parameter.Name;

/**
 * Functionality that might eventually be included into KORONA.
 */
public final class KoronaIncubatorModuleService extends ModuleService {
   public KoronaIncubatorModuleService() {
      super(new Name("KoronaIncubator", "KORONA incubator"));
   }

   @Override
   public boolean canBeUsed() {
      return KoronaIncubatorFeatureToggles.INCUBATOR_ENABLED;
   }

   @Override
   public ModulePlugin createPlugin() {
      return new KoronaIncubatorModulePlugin();
   }
}
