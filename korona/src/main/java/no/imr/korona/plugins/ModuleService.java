package no.imr.korona.plugins;

import no.imr.tools.parameter.Name;
import no.imr.tools.plugins.BaseService;

public abstract class ModuleService extends BaseService {
   protected ModuleService(Name name) {
      super(name);
   }

   public abstract ModulePlugin createPlugin();
}
