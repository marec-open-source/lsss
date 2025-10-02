package no.imr.korona.computation.categorization;

/**
 * Base class for categorizers that categorize one pixel at the time
 * or one ping at the time.
 */
final class CategorizerSubModule extends SimpleSubModule {
   private final Categorizer categorizer;

   CategorizerSubModule(CategorizationSubModule previousSubModule, Categorizer categorizer) {
      super(previousSubModule);

      this.categorizer = categorizer;
   }

   @Override
   void process(ExtendedPing extendedPing, CategorizationPing categorizationPing) {
      for (Pixel pixel : categorizationPing.getPixels()) {
         if (!pixel.isDone()) {
            categorizer.categorize(pixel, categorizationPing.getPerPingAPriori());
         }
      }
   }
}
