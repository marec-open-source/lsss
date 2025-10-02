package no.imr.korona.computation.plankton.models;

/**
 * The model is taken from
 * "Stanton, T. K., P. H. Wieber, D. Chu, M. C. Benfield, L. Scanlon, L.
 * Martin, and R. L. Eastwood, 1994: On acoustic estimates of zooplankton
 * biomass. ICES J. Mar. Sci., 51, 505-512."
 */
public final class HardShelledSphereModel extends BackscatterModel {
   private static final double TWENTY_FIVE_OVER_ONE_FOUR_FOUR = 25.0 / 144.0;
   private static final double TWENTY_FIVE_OVER_NINE = 25.0 / 9.0;
   private static final double SOUND_SPEED = 1500.0;
   private static final double PI_OVER_SOUND_FACT = Math.pow(Math.PI / SOUND_SPEED, 4);

   private double rFact = 0.5;

   public HardShelledSphereModel() {
   }

   public double getRFact() {
      return rFact;
   }

   public void setRFact(float rFact) {
      this.rFact = rFact;
   }

   @Override
   public double getBackscatter(double size, double frequency) {
      double fToFour = Math.pow(frequency, 4);
      double diameterToFour = Math.pow(size, 4);
      return TWENTY_FIVE_OVER_ONE_FOUR_FOUR * PI_OVER_SOUND_FACT * diameterToFour * size * size * fToFour * rFact * rFact /
            (1 + TWENTY_FIVE_OVER_NINE * PI_OVER_SOUND_FACT * fToFour * diameterToFour);
   }

   @Override
   public double getReducedTS(double ka) {
      double diam = ka / (Math.PI / SOUND_SPEED); //can assume without loss of generality that f==1
      double area = diam * diam * Math.PI / 4;
      return 10 * Math.log10(getBackscatter(diam, 1) / area);
   }

   @Override
   public double getBioVolume(double size) {
      return size * size * size * Math.PI / 6;
   }

   @Override
   public String toString() {
      return "R = " + rFact;
   }
}
