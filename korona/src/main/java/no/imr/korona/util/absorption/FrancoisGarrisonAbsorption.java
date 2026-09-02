package no.imr.korona.util.absorption;

public final class FrancoisGarrisonAbsorption implements Absorption {
   private final double a1_f1;
   private final double f1_f1;
   private final double a2_p2_f2;
   private final double f2_f2;
   private final double a3_p3;

   /**
    * Absorption.
    *
    * @param pH          acidity
    * @param salinity    [PPT]
    * @param temperature [C]
    * @param depth       [m]
    */
   public FrancoisGarrisonAbsorption(double pH, double salinity, double temperature, double depth) {
      double c = 1412.0 + 3.21 * temperature + 1.19 * salinity + 0.0167 * depth;    // Sound speed
      double a1 = 8.86 * Math.pow(10.0, 0.78 * pH - 5) / c;
      double a2 = 21.44 * salinity * (1 + 0.025 * temperature) / c;
      double a3;
      if (temperature > 20) { // Match MATLAB.
         a3 = 3.964e-4 - 1.146e-5 * temperature + 1.45e-7 * temperature * temperature - 6.5e-10 * temperature * temperature * temperature;
      } else {
         a3 = 4.937e-4 - 2.59e-5 * temperature + 9.11e-7 * temperature * temperature - 1.5e-8 * temperature * temperature * temperature;
      }
      double f1 = 2.8 * Math.sqrt(salinity / 35) * Math.pow(10, 4 - 1245. / (temperature + 273));
      double f2 = 8.17 * Math.pow(10, 8 - 1990.0 / (temperature + 273)) / (1 + 0.0018 * (salinity - 35));
      double p2 = 1.0 - 1.37e-4 * depth + 6.2e-9 * depth * depth;
      double p3 = 1.0 - 3.83e-5 * depth + 4.9e-10 * depth * depth;

      // The original formula uses frequency in kHz and calculates absorption in dB / km.
      // The constant factors change to frequency in Hz and absorption in dB / m.
      a1_f1 = 1e-3 * a1 * f1;
      f1_f1 = 1e6 * f1 * f1;
      a2_p2_f2 = 1e-3 * a2 * p2 * f2;
      f2_f2 = 1e6 * f2 * f2;
      a3_p3 = 1e-9 * a3 * p3;
   }

   @Override
   public double getAbsorption(double frequency) {
      double fSq = frequency * frequency;
      return fSq * (a1_f1 / (f1_f1 + fSq) + a2_p2_f2 / (f2_f2 + fSq) + a3_p3);
   }
}
