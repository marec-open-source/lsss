package no.imr.korona.computation.categorization;

/**
 * Computes the discriminant as the product of the class conditional probability
 * and the a priori probability.
 */
final class AposterioriDiscriminant extends SimpleSubModule {
   AposterioriDiscriminant(CategorizationSubModule previousSubModule) {
      super(previousSubModule);
   }

   @Override
   void process(ExtendedPing extendedPing, CategorizationPing categorizationPing) {
      for (Pixel pixel : categorizationPing.getPixels()) {
         if (pixel.isDone()) {
            continue;
         }
         for (CategoryData cd : pixel.getCategoryDatas()) {
            cd.setDiscriminant(cd.getNormalizedProbability() * cd.getTotalApriori());
         }
      }
   }
}
