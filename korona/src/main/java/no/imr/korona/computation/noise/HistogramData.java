package no.imr.korona.computation.noise;

import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.TvgArray;
import no.imr.tools.math.MathUtils;

/**
 * Sample data from a PowerData to be added to a histogram.
 * Intended to be initialized from a PowerData then passed several noise masks
 * and finally be added to a histogram.
 */
public final class HistogramData {
   private final float[] data;
   private final float[] ranges;
   private final PowerData powerData;
   private final Quality quality;

   /**
    * Allocates arrays of a given length and copies sample datagram reference
    * and quality from a given HistogramData.
    *
    * @param n             number of samples
    * @param histogramData the source for datagram reference and quality
    */
   public HistogramData(int n, HistogramData histogramData) {
      data = new float[n];
      ranges = new float[n];
      powerData = histogramData.powerData;
      quality = histogramData.quality;
   }

   /**
    * Initialize from a sample datagram.
    *
    * @param powerData the PowerData to initialize from
    */
   public HistogramData(PowerData powerData) {
      this.powerData = powerData;
      quality = new Quality();

      float[] sv = powerData.getSv();
      TvgArray tvg = powerData.getTVGArray();
      int n = sv.length;

      data = new float[n];
      ranges = new float[n];

      for (int i = 0; i < n; i++) {
         data[i] = MathUtils.avoidInfinity(sv[i] / tvg.get(i));
         ranges[i] = powerData.getSampleRange(i);
      }
   }

   /**
    * Tests whether this histogram data contains any samples.
    *
    * @return true if empty, false otherwise
    */
   public boolean isEmpty() {
      return data.length == 0;
   }

   /**
    * Gets the noise samples.
    *
    * @return an array of noise samples
    */
   public float[] getData() {
      return data;
   }

   /**
    * Get the ranges of the noise samples.
    *
    * @return an array of the ranges of the noise samples
    */
   public float[] getRanges() {
      return ranges;
   }

   /**
    * Returns the sample datagram from where these noise data originate.
    *
    * @return the sample datagram from where these noise data originate
    */
   public PowerData getPowerData() {
      return powerData;
   }

   /**
    * Get the quality.
    *
    * @return the quality
    */
   public Quality getQuality() {
      return quality;
   }

   /**
    * The quality of noise data, i.e., from where in the water column it is picked.
    */
   public static final class Quality {
      /**
       * Indicating from where the noise samples are picked.
       */
      public int category;

      /**
       * The quality as a number between 0 and 100.
       */
      public float value;

      private Quality() {
      }
   }
}
