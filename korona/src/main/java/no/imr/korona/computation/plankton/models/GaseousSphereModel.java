package no.imr.korona.computation.plankton.models;

/**
 * The model is taken from
 * "Stanton, T. K., P. H. Wieber, D. Chu, M. C. Benfield, L. Scanlon, L.
 * Martin, and R. L. Eastwood, 1994: On acoustic estimates of zooplankton
 * biomass. ICES J. Mar. Sci., 51, 505-512."
 */
public final class GaseousSphereModel extends BackscatterModel {
   private double relativeDensity = 0.0012; //g
   private double relativeSoundSpeed = 0.22; //h
   private double soundSpeed = 1500;
   private double piFactor;

   public GaseousSphereModel() {
      initializeDerivedParameters();
   }

   private void initializeDerivedParameters() {
      piFactor = computePIFactor();
   }

   private double computePIFactor() {
      double alpha = computeAlpha();
      return Math.pow(Math.PI, 4) * alpha * alpha / Math.pow(soundSpeed, 4);
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

   @Override
   public double getBackscatter(double size, double frequency) {
      double F = 1 + 0.5 / Math.pow(Math.PI * frequency * size / soundSpeed, 0.75);
      double G = 1 + 85 * Math.exp(-5 * 100000 * Math.pow(Math.PI * frequency * size / soundSpeed - 0.0135, 2));

      double fToFour = Math.pow(frequency, 4);
      double diameterToFour = Math.pow(size, 4);
      return (0.25 * piFactor * diameterToFour * size * size * fToFour * G) /
            (1 + 4 * piFactor * fToFour * diameterToFour / F);
   }

   @Override
   public double getReducedTS(double ka) {
      double diam = ka / (Math.PI / soundSpeed); //can assume without loss of generality that f==1
      double area = diam * diam * Math.PI / 4;
      return 10 * Math.log10(getBackscatter(diam, 1) / area);
   }

   @Override
   public double getBioVolume(double size) {
      return size * size * size * Math.PI / 6;
   }

   private double computeAlpha() {
      return (1 - relativeDensity * relativeSoundSpeed * relativeSoundSpeed) /
            (3 * relativeDensity * relativeSoundSpeed * relativeSoundSpeed) +
            (1 - relativeDensity) / (1 + 2 * relativeDensity);
   }

   @Override
   public String toString() {
      return "g = " + relativeDensity + ", h = " + relativeSoundSpeed;
   }
}
