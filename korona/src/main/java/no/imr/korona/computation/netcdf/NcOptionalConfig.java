package no.imr.korona.computation.netcdf;

import no.imr.korona.computation.offset.TransducerParameterManager;
import org.jspecify.annotations.Nullable;

final class NcOptionalConfig {
   @Nullable TransducerParameterManager horizontalTransducerParameterManager;
   @Nullable TransducerParameterManager verticalTransducerParameterManager;

   NcOptionalConfig() {
   }
}
