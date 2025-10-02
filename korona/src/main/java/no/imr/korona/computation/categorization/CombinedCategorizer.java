package no.imr.korona.computation.categorization;

import no.imr.korona.computation.categorization.apriori.PerPingAPriori;

import java.util.List;

/**
 * A combination of several categorizers.
 * Each pixel is tried categorized by each categorizer.
 */
final class CombinedCategorizer implements Categorizer {
   private final List<Categorizer> categorizers;

   CombinedCategorizer(List<Categorizer> categorizers) {
      this.categorizers = categorizers;
   }

   @Override
   public void categorize(Pixel pixel, PerPingAPriori perPingAPriori) {
      for (Categorizer categorizer : categorizers) {
         categorizer.categorize(pixel, perPingAPriori);
         if (!pixel.getCategoryDatas().isEmpty()) {
            break;
         }
      }
   }
}
