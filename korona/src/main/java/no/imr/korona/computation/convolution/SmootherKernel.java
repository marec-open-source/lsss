package no.imr.korona.computation.convolution;

import no.imr.tools.math.ArrayMath;
import no.imr.tools.misc.FloatUnaryOperator;
import no.imr.tools.range.FloatRange;

/**
 * Smoother kernels where the weights sum to 1.
 */
public final class SmootherKernel extends ConvolutionKernel {
   private final FloatUnaryOperator function;

   public SmootherKernel(FloatRange range, FloatUnaryOperator function) {
      super(range);

      this.function = function;
   }

   /**
    * Returns a set of weights that sum to 1.
    *
    * @param positions a set of positions
    * @return the normalized weights
    */
   @Override
   public float[] getWeights(float[] positions) {
      double sumWeights = 0;
      float[] weights = new float[positions.length];
      for (int i = 0; i < positions.length; i++) {
         float weight = function.applyAsFloat(positions[i]);
         weights[i] = weight;
         sumWeights += weight;
      }
      if (sumWeights > 0) {
         ArrayMath.divide(weights, (float) sumWeights);
      }
      return weights;
   }

   /**
    * Constructs a new tophat kernel, with
    * f(x) = 1 for x in [-width/2, width/2] and 0 elsewhere.
    *
    * @param width the width of the tophat
    */
   public static SmootherKernel tophat(float width) {
      float radius = width / 2;
      FloatRange range = FloatRange.of(-radius, radius);
      return new SmootherKernel(range, position -> {
         return range.containsIncludingEnd(position) ? 1 : 0;
      });
   }

   /**
    * Creates a gaussian distribution, given a width, where
    * the width is twice the standard deviation.
    *
    * @param width the width
    */
   public static SmootherKernel gaussian(float width) {
      float stdDev = width / 2;
      float cutoff = (float) Math.sqrt(-Math.log(0.01));
      float radius = stdDev * cutoff;
      FloatRange range = FloatRange.of(-radius, radius);
      return new SmootherKernel(range, position -> {
         float x = position / stdDev;
         return (float) Math.exp(-x * x);
      });
   }
}
