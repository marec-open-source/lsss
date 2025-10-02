package no.imr.korona.util.masking.grid;

import no.imr.tools.test.BenchmarkTimer;

final class GridMaskTimer {
   private GridMaskTimer() {
   }

   public static void main(String[] args) {
      new BenchmarkTimer(() -> {
         GridMaskTest gridMaskTest = new GridMaskTest();
         gridMaskTest.pyramid();
      });
   }
}
