package no.imr.korona.computation.plankton.models;

public final class DummyModel extends BackscatterModel {
   public DummyModel() {
   }

   @Override
   public double getBackscatter(double size, double frequency) {
      double f = frequency / 1000;
      return size * size * f * f;
   }

   @Override
   public double getReducedTS(double ka) {
      return 1;
   }

   @Override
   public double getBioVolume(double size) {
      return size * size * size;
   }
}
