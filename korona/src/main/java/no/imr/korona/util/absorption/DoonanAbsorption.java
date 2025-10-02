package no.imr.korona.util.absorption;

/**
 * Implements the formula given in
 * Doonan, I.J.; Coombs, R.F.; McClatchie, S. (2003). The absorption of sound in seawater in relation to estimation of
 * deep-water fish biomass. ICES Journal of Marine Science 60: 1-9.
 * <p>
 * Note that the paper has two errors in the formulae given in the conclusions.
 * Returns the absorption coefficient [dB/km] for
 * % the given acoustic frequency (f [kHz]), salinity
 * % (S, [ppt]), temperature (T, [degC]), and depth
 * % (D, [m]).
 */
public final class DoonanAbsorption extends Absorption {
   private final double a2_p2_f2_by_c;
   private final double f2_f2;
   private final double a3_p3;

   /**
    * Absorption.
    *
    * @param salinity    [PPT]
    * @param temperature [C]
    * @param depth       [m]
    */
   public DoonanAbsorption(double salinity, double temperature, double depth) {
      double c = 1412.0 + 3.21 * temperature + 1.19 * salinity + 0.0167 * depth;    // Sound speed
      double A2 = 22.19 * salinity * (1.0 + 0.017 * temperature);
      double f2 = 1.8 * Math.pow(10, 7.0 - 1518 / (temperature + 273));
      double P2 = Math.exp(-1.76e-4 * depth);
      double A3 = 4.937e-4 - 2.59e-5 * temperature + 9.11e-7 * temperature * temperature - 1.5e-8 * temperature * temperature * temperature;
      double P3 = 1.0 - 3.83e-5 * depth + 4.9e-10 * depth * depth;

      // The original formula uses frequency in kHz and calculates absorption in dB / km.
      // The constant factors change to frequency in Hz and absorption in dB / m.
      a2_p2_f2_by_c = 1e-3 * A2 * P2 * f2 / c;
      f2_f2 = 1e6 * f2 * f2;
      a3_p3 = 1e-9 * A3 * P3;
   }

   @Override
   public double getAbsorption(double frequency) {
      double fSq = frequency * frequency;
      return fSq * (a2_p2_f2_by_c / (f2_f2 + fSq) + a3_p3);
   }
}
