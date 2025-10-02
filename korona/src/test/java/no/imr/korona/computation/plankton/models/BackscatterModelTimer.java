package no.imr.korona.computation.plankton.models;

import no.imr.tools.Utils;
import no.imr.tools.time.Stopwatch;

import java.util.Random;

@SuppressWarnings("PMD.SystemPrintln")
final class BackscatterModelTimer {
   private static final int NUMBER_OF_SAMPLINGS = 1000000;
   private static final long SEED = 241781587512379135L;

   private BackscatterModelTimer() {
   }

   private static void timeModel(BackscatterModel model) {
      Random random = new Random(SEED);
      Stopwatch stopwatch = Stopwatch.createStarted();
      for (int i = 0; i < NUMBER_OF_SAMPLINGS; i++) {
         float diameter = random.nextFloat(0.01f); // Up to 1 cm.
         float frequency = random.nextFloat(20_000, 420_000); // From 20 to 420 kHz.
         model.getBackscatter(diameter, frequency);
      }
      stopwatch.stop();

      System.out.println("model: " + model);
      System.out.println("stopwatch.seconds() = " + Utils.format("%.3f", stopwatch.seconds()) + "     ");
      System.out.println();
   }

   public static void main(String[] args) {
      timeModel(new GaseousSphereModel());
      timeModel(new HardShelledSphereModel());
      timeModel(new FluidProlateSpheroidModel());
      timeModel(new FluidBentCylinderModel());
   }
}
