package no.imr.tools.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ArrayKernelTest {
   @Test
   void createGaussian() {
      assertThrows(IllegalArgumentException.class, () -> ArrayKernel.createGaussian(-1));

      assertArrayEquals(
            new float[]{0, 0, 0, 1, 0, 0, 0},
            ArrayKernel.createGaussian(0).smooth(new float[]{0, 0, 0, 1, 0, 0, 0}));

      assertArrayEquals(
            new float[]{0.14618623f, 0.16145033f, 0.17164819f, 0.17524014f, 0.17164819f, 0.16145033f, 0.14618623f},
            ArrayKernel.createGaussian(3).smooth(new float[]{0, 0, 0, 1, 0, 0, 0}));
   }
}
