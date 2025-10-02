package no.imr.korona.computation.categorization;

import no.imr.korona.data.ping.items.channel.PowerData;

final class ThresholdCategorization extends SimpleSubModule {
   private final Category unknownCategory;
   private final float minLogSv;

   ThresholdCategorization(CategorizationSubModule previousSubModule, Category unknownCategory, float minLogSv) {
      super(previousSubModule);

      this.unknownCategory = unknownCategory;
      this.minLogSv = minLogSv;
   }

   @Override
   void process(ExtendedPing extendedPing, CategorizationPing categorizationPing) {
      Pixel[] pixels = categorizationPing.getPixels();
      PowerData referenceDatagram = categorizationPing.getReferenceDatagram();
      float[] logSv = referenceDatagram.getLogSv();

      for (int i = 0; i < logSv.length; i++) {
         if (logSv[i] < minLogSv) {
            pixels[i].setFinalCategory(unknownCategory);
         }
      }
   }
}
