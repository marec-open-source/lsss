package no.imr.korona.computation.noise;

import no.imr.korona.data.ping.Ping;
import org.jspecify.annotations.Nullable;

/**
 * Base class for the noise masker classes.
 */
abstract class BaseNoiseMask extends SubModuleWithLogging {
   BaseNoiseMask() {
   }

   /**
    * Fill noise power index data into a HistogramData according to specified filter.
    *
    * @param histogramData a histogram data object with unmasked sample values
    * @param ping          the ping to base the masking on
    * @param rangeValues   range values
    * @return a masked HistogramData
    */
   abstract @Nullable HistogramData mask(@Nullable HistogramData histogramData, Ping ping, RangeValues rangeValues);
}
