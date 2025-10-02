package no.imr.korona.computation.plankton.models;

import no.imr.korona.data.datagrams.Pid0Datagram;

public abstract class BackscatterModel {
   protected BackscatterModel() {
   }

   public abstract double getBackscatter(double size, double frequency);

   public abstract double getReducedTS(double ka);

   public abstract double getBioVolume(double size);

   public double getBioVolume(Pid0Datagram.LengthDistribution lengthDistribution) {
      double bioVolume = 0;

      float[] dividers = lengthDistribution.getDividers();
      float[] abundances = lengthDistribution.getAbundances();
      for (int i = 0; i < abundances.length; i++) {
         float abundance = abundances[i];
         float size = (dividers[i] + dividers[i + 1]) / 2;
         bioVolume += abundance * getBioVolume(size);
      }

      return bioVolume;
   }
}
