package no.imr.korona.data.formats.netcdf;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.incubator.KoronaIncubatorFeatureToggles;
import no.imr.korona.plugins.DataFormatPlugin;
import no.imr.korona.plugins.DataFormatService;
import no.imr.tools.parameter.Name;

public final class NetcdfDataFormatService extends DataFormatService {
   public NetcdfDataFormatService() {
      super(new Name("NetCDF"));
   }

   @Override
   public boolean canBeUsed() {
      return KoronaIncubatorFeatureToggles.INCUBATOR_ENABLED;
   }

   @Override
   public DataFormatPlugin createPlugin(DatagramTypeManager datagramTypeManager) {
      return new NetcdfDataFormatPlugin(getName());
   }
}
