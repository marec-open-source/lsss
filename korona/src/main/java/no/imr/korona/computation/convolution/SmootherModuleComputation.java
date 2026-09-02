package no.imr.korona.computation.convolution;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;

import java.io.IOException;

final class SmootherModuleComputation extends BaseFilterModuleComputation {
   private final SmootherModule module;
   private final ConvolutionKernel horizontalKernel;
   private final VerticalConvolution verticalConvolution;

   SmootherModuleComputation(SmootherModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
      super(module, computationContext, pingSource);

      this.module = module;

      horizontalKernel = SmootherModule.createConvolutionKernel(module.horizontalKernelType.getValue(), module.horizontalWidth.getFloatValue());
      setDistances(horizontalKernel.getRange());

      verticalConvolution = new VerticalConvolution(SmootherModule.createConvolutionKernel(module.verticalKernelType.getValue(), module.verticalWidth.getFloatValue()));
   }

   @Override
   PowerData filterVertically(PowerData rawInput,
                              boolean[] mask,
                              int newChannel) {
      PowerData result = rawInput.makeCopyWithAnglesOnly();
      result.setChannel(newChannel);

      float[] input = getArray(rawInput);
      float[] output = getArray(result);

      verticalConvolution.doConvolution(rawInput.getSampleDistance(), input, output, mask);

      return result;
   }

   @Override
   void doFilterHorizontally(FilterInput[] filterInputs,
                             int startI, int endI,
                             float[] output, RawAndMask currentDatagram) {
      float[] positions = getPositions(filterInputs);
      float[] w = horizontalKernel.getWeights(positions);
      float[] values = getArray(currentDatagram.powerData);

      for (int i = startI; i < endI; i++) {
         if (currentDatagram.mask[i]) {
            double sum = 0;
            double sumWeights = 0;
            for (int j = 0; j < w.length; j++) {
               FilterInput filterInput = filterInputs[j];
               int k = i + filterInputs[j].offset;
               if (filterInput.mask[k]) {
                  float weight = w[j];
                  sumWeights += weight;
                  sum += weight * filterInput.values[k];
               }
            }
            output[i] = sumWeights > 0 ? (float) (sum / sumWeights) : values[i];
         } else {
            output[i] = values[i];
         }
      }
   }

   @Override
   float[] getArray(PowerData powerData) {
      return module.logarithmicValues.getBooleanValue() ? powerData.getLogSv() : powerData.getSv();
   }
}
