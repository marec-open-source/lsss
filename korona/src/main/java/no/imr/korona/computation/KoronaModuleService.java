package no.imr.korona.computation;

import no.imr.korona.plugins.ModulePlugin;
import no.imr.korona.plugins.ModuleService;
import no.imr.tools.parameter.Name;

public final class KoronaModuleService extends ModuleService {
   public KoronaModuleService() {
      super(new Name("KoronaModule"));
   }

   @Override
   public ModulePlugin createPlugin() {
      return new KoronaModulePlugin();
   }
}
