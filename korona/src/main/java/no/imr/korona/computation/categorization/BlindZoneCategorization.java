package no.imr.korona.computation.categorization;

import no.imr.korona.data.ping.items.channel.PowerData;

/**
 * Categorizes as blind zone all pixels above transducer depth plus a specified distance.
 */
final class BlindZoneCategorization extends SimpleSubModule {
   private final Category blindZoneCategory;
   private final float blindZoneRange;

   BlindZoneCategorization(CategorizationSubModule previousSubModule, Category blindZoneCategory, float blindZoneRange) {
      super(previousSubModule);

      this.blindZoneCategory = blindZoneCategory;
      this.blindZoneRange = blindZoneRange;
   }

   @Override
   void process(ExtendedPing extendedPing, CategorizationPing categorizationPing) {
      PowerData referenceDatagram = categorizationPing.getReferenceDatagram();
      float depthLimit = blindZoneRange + referenceDatagram.getHeaveCorrectedTransducerDepth();
      Pixel[] pixels = categorizationPing.getPixels();
      int endIndex = Math.min(referenceDatagram.depthToSampleIndex(depthLimit), pixels.length);
      for (int i = 0; i < endIndex; i++) {
         pixels[i].setFinalCategory(blindZoneCategory);
      }
   }
}
