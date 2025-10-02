package no.imr.korona.computation.plankton.models;

/**
 * The model is taken from
 * "Stanton, T. K., P. H. Wieber, D. Chu, M. C. Benfield, L. Scanlon, L.
 * Martin, and R. L. Eastwood, 1994: On acoustic estimates of zooplankton
 * biomass. ICES J. Mar. Sci., 51, 505-512."
 */
public final class FluidBentCylinderModel extends BackscatterModel {
   private double lengthToWidth = 11;
   private double stdDevLength = 0.1;
   private double rFact = 0.058;
   private double soundSpeed = 1500;
   private double const1;
   private double const2;

   public FluidBentCylinderModel() {
      initializeDerivedParameters();
   }

   private void initializeDerivedParameters() {
      const1 = computeC1();
      const2 = computeC2();
   }

   private double computeC1() {
      return 0.08 * rFact * rFact * lengthToWidth;
   }

   private double computeC2() {
      return Math.PI / soundSpeed;
   }

   public double getLengthToWidth() {
      return lengthToWidth;
   }

   public void setLengthToWidth(float lengthToWidth) {
      this.lengthToWidth = lengthToWidth;
      initializeDerivedParameters();
   }

   public double getStdDevLength() {
      return stdDevLength;
   }

   public void setStdDevLength(float stdDevLength) {
      this.stdDevLength = stdDevLength;
   }

   public double getRFact() {
      return rFact;
   }

   public void setRFact(float rFact) {
      this.rFact = rFact;
      initializeDerivedParameters();
   }

   /**
    * Returns the computed backscatter for the Fluid Bent Cylinder model.
    *
    * @param size      the length of the cylinder [m]
    * @param frequency the frequency [Hz]
    * @return the computed backscatter
    */
   @Override
   public double getBackscatter(double size, double frequency) {
      double diam = size / lengthToWidth;
      double c3 = const2 * frequency * diam;
      return const1 * diam * diam * (1 - Math.exp(-8 * c3 * c3 * stdDevLength * stdDevLength) *
            Math.cos(c3 * (4 - 0.5 * Math.PI / (c3 + 0.4))));
   }

   @Override
   public double getReducedTS(double ka) {
      double diam = ka / const2;  //can assume without loss of generality that f==1
      double area = diam * diam * lengthToWidth * lengthToWidth;
      double length = diam * lengthToWidth;
      return 10 * Math.log10(getBackscatter(length, 1) / area);
   }

   @Override
   public double getBioVolume(double size) {
      double diam = size / lengthToWidth;
      return size * Math.PI * diam * diam / 4;
   }

   @Override
   public String toString() {
      return "beta = " + lengthToWidth + ", stdDevLength = " + stdDevLength + ", R = " + rFact;
   }
}
