package no.imr.korona.computation.categorization;

import no.imr.korona.data.datagrams.Dep0Datagram;
import no.imr.korona.data.ping.items.channel.PowerData;

/**
 * Categorizes as bottom all pixels below the minimum depth returned from the Dep0Datagrams.
 */
final class BottomCategorization extends SimpleSubModule {
   private final Category bottomCategory;

   BottomCategorization(CategorizationSubModule previousSubModule, Category bottomCategory) {
      super(previousSubModule);

      this.bottomCategory = bottomCategory;
   }

   @Override
   void process(ExtendedPing extendedPing, CategorizationPing categorizationPing) {
      Dep0Datagram depthDatagram = extendedPing.getPing().getDep0Datagram();
      if (depthDatagram == null) {
         return;
      }
      Pixel[] pixels = categorizationPing.getPixels();
      PowerData referenceDatagram = categorizationPing.getReferenceDatagram();
      int startBottom = referenceDatagram.depthToSampleIndex(depthDatagram.getMinimumDepth());
      startBottom = Math.max(0, startBottom);
      for (int i = startBottom; i < pixels.length; i++) {
         pixels[i].setFinalCategory(bottomCategory);
      }
   }
}
