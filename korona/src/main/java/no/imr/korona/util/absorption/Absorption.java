package no.imr.korona.util.absorption;

public interface Absorption {

   /**
    * Calculate absorption.
    *
    * @param frequency [Hz]
    * @return [dB/m]
    */
   double getAbsorption(double frequency);
}
