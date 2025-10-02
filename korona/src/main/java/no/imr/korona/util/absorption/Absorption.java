package no.imr.korona.util.absorption;

public abstract class Absorption {
   protected Absorption() {
   }

   /**
    * Calculate absorption.
    *
    * @param frequency [Hz]
    * @return [dB/m]
    */
   public abstract double getAbsorption(double frequency);
}
