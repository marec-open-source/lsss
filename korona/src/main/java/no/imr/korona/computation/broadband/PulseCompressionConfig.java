package no.imr.korona.computation.broadband;

import no.imr.korona.computation.broadband.transferfunction.TransferFunction;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.tools.range.FloatRange;

public record PulseCompressionConfig(
      float slope,
      float pulseDuration,
      FloatRange frequencyRange,
      int pulseForm,
      float originalSamplingRate,
      PulseCompressionFilterChain filterChain,
      TransferFunction signalTransferFunction,
      TransferFunction responseTransferFunction
) {
   public PulseCompressionConfig(BroadbandData broadbandData,
                                 PulseCompressionFilterChain filterChain,
                                 TransferFunction signalTransferFunction,
                                 TransferFunction responseTransferFunction) {
      this(
            broadbandData.getSlope(),
            broadbandData.getPulseDuration(),
            broadbandData.getFrequencyRange(),
            broadbandData.getPulseForm(),
            broadbandData.getTransducer().getEK80fs(),
            filterChain,
            signalTransferFunction,
            responseTransferFunction
      );
   }
}
