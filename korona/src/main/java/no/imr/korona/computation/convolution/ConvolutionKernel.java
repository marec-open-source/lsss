package no.imr.korona.computation.convolution;

import no.imr.tools.range.FloatRange;

/**
 * Interface for all convolution kernels.
 */
public abstract class ConvolutionKernel {
   private final FloatRange range;

   ConvolutionKernel(FloatRange range) {
      this.range = range;
   }

   /**
    * Returns the lower and upper limits for this kernel.
    *
    * @return the range
    */
   public FloatRange getRange() {
      return range;
   }

   /**
    * Returns the weights.
    *
    * @param positions the positions to give weights
    * @return the weights
    */
   public abstract float[] getWeights(float[] positions);
}
