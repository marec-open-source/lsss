package no.imr.korona.data.datagrams;

import no.imr.korona.plugins.DatagramPlugin;
import no.imr.korona.plugins.DatagramService;
import no.imr.tools.parameter.Name;

public final class KoronaDatagramService extends DatagramService {
   public KoronaDatagramService() {
      super(new Name("Korona", "KORONA"));
   }

   @Override
   public DatagramPlugin createPlugin() {
      return new KoronaDatagramPlugin(getName());
   }
}
