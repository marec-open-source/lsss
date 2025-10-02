package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.plugins.DataFormatPlugin;
import no.imr.korona.plugins.DataFormatService;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;

public final class SyntheticDataFormatService extends DataFormatService {
   public SyntheticDataFormatService() {
      super(new Name("Synthetic"));
   }

   @Override
   public boolean canBeUsed() {
      return Utils.useTestFeatures();
   }

   @Override
   public DataFormatPlugin createPlugin(DatagramTypeManager datagramTypeManager) {
      return new SyntheticDataFormatPlugin(getName());
   }
}
