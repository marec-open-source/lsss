package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.plugins.DataFormatPlugin;
import no.imr.korona.plugins.DataFormatService;
import no.imr.tools.parameter.Name;

public final class EK60DataFormatService extends DataFormatService {
   public EK60DataFormatService() {
      super(new Name("EK60"));
   }

   @Override
   public DataFormatPlugin createPlugin(DatagramTypeManager datagramTypeManager) {
      return new EK60DataFormatPlugin(getName(), datagramTypeManager);
   }
}
