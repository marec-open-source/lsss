package no.imr.korona.computation.categorization;

import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.TvgArray;

/**
 * Categorizes as noise all pixels which the reference datagram indicates as
 * not above noise.
 * Assumes that noise has been removed.
 */
final class NoiseCategorization extends SimpleSubModule {
   private final Category noiseCategory;

   NoiseCategorization(CategorizationSubModule previousSubModule, Category noiseCategory) {
      super(previousSubModule);

      this.noiseCategory = noiseCategory;
   }

   @Override
   void process(ExtendedPing extendedPing, CategorizationPing categorizationPing) {
      Pixel[] pixels = categorizationPing.getPixels();
      PowerData powerData = categorizationPing.getReferenceDatagram();
      float noiseThreshold = powerData.getNoisePowerIndexNoiseThreshold();
      float[] sv = powerData.getSv();
      TvgArray tvgArray = powerData.getTVGArray();
      for (int i = 0; i < pixels.length; i++) {
         float noise = sv[i] / tvgArray.get(i);
         if (noise < noiseThreshold) {
            pixels[i].setFinalCategory(noiseCategory);
         }
      }
   }
}
