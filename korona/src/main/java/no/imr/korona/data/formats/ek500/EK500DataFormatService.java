package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.plugins.DataFormatPlugin;
import no.imr.korona.plugins.DataFormatService;
import no.imr.tools.parameter.Name;

public final class EK500DataFormatService extends DataFormatService {
   public EK500DataFormatService() {
      super(new Name("EK500"));
   }

   @Override
   public DataFormatPlugin createPlugin(DatagramTypeManager datagramTypeManager) {
      return new EK500DataFormatPlugin(getName());
   }
}
