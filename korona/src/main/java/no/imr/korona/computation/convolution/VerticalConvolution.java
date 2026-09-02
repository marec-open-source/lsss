package no.imr.korona.computation.convolution;

public final class VerticalConvolution {
   private final ConvolutionKernel kernel;

   private float weightsSampleDistance;
   private float[][] weightsSurface = new float[0][];
   private float[] weightsMiddle = new float[0];
   private float[][] weightsBottom = new float[0][];

   public VerticalConvolution(ConvolutionKernel kernel) {
      this.kernel = kernel;
   }

   /**
    * Generate weights.
    * Situation near the ends are treated separately.
    *
    * @param sampleDistance distance between pixels
    */
   private void makeWeights(float sampleDistance) {
      weightsSampleDistance = sampleDistance;
      int m = (int) Math.floor(kernel.getRange().max() / sampleDistance);
      int n = (int) Math.floor(-kernel.getRange().min() / sampleDistance);

      weightsSurface = new float[m][];
      for (int i = 0; i < m; i++) {
         float[] positions = new float[i + n + 1];
         for (int j = 0; j < positions.length; j++) {
            positions[j] = sampleDistance * (j - i);
         }
         weightsSurface[i] = kernel.getWeights(positions);
      }

      // middle
      {
         float[] positions = new float[m + n + 1];
         for (int j = 0; j < positions.length; j++) {
            positions[j] = sampleDistance * (j - m);
         }
         weightsMiddle = kernel.getWeights(positions);
      }

      weightsBottom = new float[n][];
      for (int i = 0; i < n; i++) {
         float[] positions = new float[m + n - i];
         for (int j = 0; j < positions.length; j++) {
            positions[j] = sampleDistance * (-m + j);
         }
         weightsBottom[i] = kernel.getWeights(positions);
      }
   }

   public void doConvolution(float sampleDistance, float[] input, float[] output, boolean[] mask) {
      if (weightsSampleDistance != sampleDistance) {
         makeWeights(sampleDistance);
      }

      int m = weightsSurface.length;
      int n = weightsBottom.length;

      int length = input.length;

      if (length < m + n + 1) {
         System.arraycopy(input, 0, output, 0, length);
         return;
      }

      for (int i = 0; i < m; i++) {
         if (mask[i]) {
            float[] w = weightsSurface[i];
            double sum = 0;
            double sumWeights = 0;
            for (int j = 0, k = 0; j < w.length; j++, k++) {
               if (mask[k]) {
                  float weight = w[j];
                  sumWeights += weight;
                  sum += weight * input[k];
               }
            }
            // sumWeights includes w[i] and is therefore > 0.
            output[i] = (float) (sum / sumWeights);
         } else {
            output[i] = input[i];
         }
      }

      {
         float[] w = weightsMiddle;
         for (int i = m; i < length - n; i++) {
            if (mask[i]) {
               double sum = 0;
               double sumWeights = 0;
               for (int j = 0, k = i - m; j < w.length; j++, k++) {
                  if (mask[k]) {
                     float weight = w[j];
                     sumWeights += weight;
                     sum += weight * input[k];
                  }
               }
               // sumWeights includes w[i] and is therefore > 0.
               output[i] = (float) (sum / sumWeights);
            } else {
               output[i] = input[i];
            }
         }
      }

      for (int i = length - n; i < length; i++) {
         if (mask[i]) {
            float[] w = weightsBottom[i - (length - n)];
            double sum = 0;
            double sumWeights = 0;
            for (int j = 0, k = i - m; j < w.length; j++, k++) {
               if (mask[k]) {
                  float weight = w[j];
                  sumWeights += weight;
                  sum += weight * input[k];
               }
            }
            // sumWeights includes w[i] and is therefore > 0.
            output[i] = (float) (sum / sumWeights);
         } else {
            output[i] = input[i];
         }
      }
   }
}
