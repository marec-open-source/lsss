package no.imr.korona.computation.convolution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SmootherKernelTest {
   @Test
   void tophat() {
      ConvolutionKernel kernel = SmootherKernel.tophat(2);
      float[] weights = kernel.getWeights(new float[]{-2, Math.nextDown(-1), -1, 0.5f, 0, 0.5f, 1, Math.nextUp(1), 2});
      assertArrayEquals(new float[]{0, 0, 0.2f, 0.2f, 0.2f, 0.2f, 0.2f, 0, 0}, weights);
   }
}
