package no.imr.korona.plugins;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.tools.parameter.Name;
import no.imr.tools.plugins.BaseService;

/**
 * A service providing a {@link DataFormatPlugin}.
 */
public abstract class DataFormatService extends BaseService {
   protected DataFormatService(Name name) {
      super(name);
   }

   public abstract DataFormatPlugin createPlugin(DatagramTypeManager datagramTypeManager);
}
