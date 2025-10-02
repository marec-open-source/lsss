package no.imr.korona.computation.plankton.models;

/**
 * The model is taken from
 * "Stanton, T. K., 1989: Simple approximate formulas for backscattering of sound
 * by spherical and elongated objects. J. Acoust. Soc. Am., 86, 1499-1510."
 */
public final class FluidProlateSpheroidModel extends BackscatterModel {
   private double lengthToWidth = 5;
   private double relativeDensity = 1.043; //g
   private double relativeSoundSpeed = 1.052; //h
   private double soundSpeed = 1500;
   private double rFactor;
   private double piFactor;

   public FluidProlateSpheroidModel() {
      initializeDerivedParameters();
   }

   private void initializeDerivedParameters() {
      piFactor = computePIFactor();
      rFactor = computeR();
   }

   private double computeAlpha() {
      double gTimesH2 = relativeDensity * relativeSoundSpeed * relativeSoundSpeed;
      return (1 - gTimesH2) / (2 * gTimesH2) + (1 - relativeDensity) / (1 + relativeDensity);
   }

   private double computePIFactor() {
      double alpha = computeAlpha();
      return Math.pow(Math.PI, 4) * alpha * alpha / (9 * Math.pow(soundSpeed, 4));
   }

   private double computeR() {
      double gh = relativeDensity * relativeSoundSpeed;
      return (gh - 1) / (gh + 1);
   }

   public double getLengthToWidth() {
      return lengthToWidth;
   }

   public void setLengthToWidth(float lengthToWidth) {
      this.lengthToWidth = lengthToWidth;
   }

   public double getRelativeDensity() {
      return relativeDensity;
   }

   public void setRelativeDensity(float relativeDensity) {
      this.relativeDensity = relativeDensity;
      initializeDerivedParameters();
   }

   public double getRelativeSoundSpeed() {
      return relativeSoundSpeed;
   }

   public void setRelativeSoundSpeed(float relativeSoundSpeed) {
      this.relativeSoundSpeed = relativeSoundSpeed;
      initializeDerivedParameters();
   }

   /**
    * Returns the computed backscatter for the Fluid Prolate Spheroid model.
    *
    * @param size      the length of the spheroid [m]
    * @param frequency the frequency [Hz]
    * @return the computed backscatter
    */
   @Override
   public double getBackscatter(double size, double frequency) {
      double diam = size / lengthToWidth;
      double ka = Math.PI * frequency * diam / soundSpeed;
      //F and G from Table II in Stanton '89
      double F = 2.5 * Math.pow(ka, 1.65);
      double G = 1 - 0.8 * Math.exp(-2.5 * (ka - 2.3) * (ka - 2.3));
      double fToFour = Math.pow(frequency, 4);
      return piFactor * fToFour * Math.pow(diam, 6) * lengthToWidth * lengthToWidth * G /
            (1 + 16 * piFactor * fToFour * Math.pow(diam, 4) / (rFactor * rFactor * F));
   }

   @Override
   public double getReducedTS(double ka) {
      double diam = ka / (Math.PI / soundSpeed); //can assume without loss of generality that f==1
      double area = diam * diam * lengthToWidth * lengthToWidth;
      double length = diam * lengthToWidth;
      return 10 * Math.log10(getBackscatter(length, 1) / area);
   }

   @Override
   public double getBioVolume(double size) {
      double diam = size / lengthToWidth;
      return size * diam * diam * Math.PI / 6;
   }

   @Override
   public String toString() {
      return "beta = " + lengthToWidth + ", g = " + relativeDensity + ", h = " + relativeSoundSpeed;
   }
}
