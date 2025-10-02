package no.imr.korona.computation.categorization;

import no.imr.korona.data.datagrams.Cad0Datagram;

import java.util.List;

/**
 * Generates Cad0Datagrams containing the results of the categorization.
 * These category datagrams are placed in the ping of each CategorizationPing.
 */
final class MakeCad0SubModule extends SimpleSubModule {
   private final int categoryCount;
   private final Category unknownCategory;

   MakeCad0SubModule(CategorizationSubModule previousSubModule, int categoryCount, Category unknownCategory) {
      super(previousSubModule);

      this.categoryCount = categoryCount;
      this.unknownCategory = unknownCategory;
   }

   @Override
   void process(ExtendedPing extendedPing, CategorizationPing categorizationPing) {
      Pixel[] pixels = categorizationPing.getPixels();
      if (pixels.length == 0) {
         return;
      }
      Cad0Datagram cad0 = new Cad0Datagram(extendedPing.getPing().getNTDate(),
            categoryCount, categorizationPing.getPixels().length,
            categorizationPing.getSampleDistance(), categorizationPing.getFirstDepth());

      for (int iPixel = 0; iPixel < pixels.length; iPixel++) {
         pixels[iPixel].normalizeDiscriminants();
         //List categories = pixels[iPixel].getCategoryDatas();
         List<CategoryData> categories = pixels[iPixel].getAcceptableCategoryDatas();
         if (categories.isEmpty()) {
            pixels[iPixel].setFinalCategory(unknownCategory);
         }
         categories.sort(null);
         for (int iCat = 0; iCat < categoryCount; iCat++) {
            if (iCat < categories.size()) {
               CategoryData cd = categories.get(iCat);
               cad0.setPixel(iPixel, iCat,
                     cd.getCategory().getNumber(), cd.getDiscriminant(), cd.getBoundedProbability());
            } else {
               cad0.setPixel(iPixel, iCat, unknownCategory.getNumber(), 0, 0);
            }
         }
      }
      extendedPing.getPing().removeAll(Cad0Datagram.class);
      extendedPing.getPing().add(cad0);
   }
}
