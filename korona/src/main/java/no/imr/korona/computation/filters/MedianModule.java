package no.imr.korona.computation.filters;

import no.imr.korona.computation.BaseMatrixModule;
import no.imr.korona.computation.BaseMatrixModuleComputation;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.math.Median;

/**
 * Sets the sample value to the median of its 8 neighbors and itself.
 * Has the effect of removing spikes and of smoothing the data whilst preserving edges.
 */
public final class MedianModule extends BaseMatrixModule {
   public MedianModule() {
      super(1);
   }

   @Override
   public BaseMatrixModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new MedianModuleComputation(this, computationContext, pingSource);
   }

   private static final class MedianModuleComputation extends BaseMatrixModuleComputation {
      private MedianModuleComputation(MedianModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);
      }

      @Override
      protected void doPing(int startDepth, int endDepth, int channelIndex) {
         int start = startDepth + 1;
         int end = endDepth - 1;

         float p0 = getRawDataFromBuffer(channelIndex, start - 1, -1);
         float p1 = getRawDataFromBuffer(channelIndex, start - 1, 0);
         float p2 = getRawDataFromBuffer(channelIndex, start - 1, 1);

         float p3 = getRawDataFromBuffer(channelIndex, start, -1);
         float p4 = getRawDataFromBuffer(channelIndex, start, 0);
         float p5 = getRawDataFromBuffer(channelIndex, start, 1);

         for (int dep = start; dep < end; dep++) {
            float p6 = getRawDataFromBuffer(channelIndex, dep + 1, -1);
            float p7 = getRawDataFromBuffer(channelIndex, dep + 1, 0);
            float p8 = getRawDataFromBuffer(channelIndex, dep + 1, 1);

            setData(channelIndex, dep, Median.of(p0, p1, p2, p3, p4, p5, p6, p7, p8));

            p0 = p3;
            p1 = p4;
            p2 = p5;

            p3 = p6;
            p4 = p7;
            p5 = p8;
         }
      }
   }
}
