package no.imr.korona.plugins;

import no.imr.tools.parameter.Name;
import no.imr.tools.plugins.BaseService;

/**
 * A service providing a {@link DatagramPlugin}.
 */
public abstract class DatagramService extends BaseService {
   protected DatagramService(Name name) {
      super(name);
   }

   public abstract DatagramPlugin createPlugin();
}
