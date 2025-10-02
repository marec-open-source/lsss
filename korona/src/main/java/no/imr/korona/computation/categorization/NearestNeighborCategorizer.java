package no.imr.korona.computation.categorization;

import no.imr.korona.computation.categorization.apriori.PerPingAPriori;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Non-parametric classification searching for nearest neighbors.
 */
final class NearestNeighborCategorizer implements Categorizer {
   private final Metric metric;
   private final float distanceThreshold;
   private final Collection<Category> categoriesWithScatter = new ArrayList<>();
   private final Category.DistributionLevel distributionLevel;

   NearestNeighborCategorizer(Category.DistributionLevel distributionLevel, Configurator configurator) {
      this.distributionLevel = distributionLevel;
      metric = configurator.nearestNeighborMetric.getValue();
      distanceThreshold = configurator.nearestNeighborDistanceThreshold.getFloatValue();
      for (Category category : configurator.getCategories()) {
         if (category.isActive() && !category.getCategoryDistribution(distributionLevel).getNeighborhood().getNeighbors().isEmpty()) {
            category.getPixelCategoryDistribution().getNeighborhood().makeKDTree();
            categoriesWithScatter.add(category);
         }
      }
   }

   @Override
   public void categorize(Pixel pixel, PerPingAPriori perPingAPriori) {
      for (Category category : categoriesWithScatter) {
         Neighborhood neighborhood = category.getCategoryDistribution(distributionLevel).getNeighborhood();
         Neighborhood.NearestNeighbor nn = neighborhood.findNearestNeighbor(pixel, metric);
         if (nn.neighbor != null && nn.distance < distanceThreshold) {
            float probability = 1 / (1 + nn.distance);
            pixel.getCategoryDatas().add(new CategoryData(category, 0,
                  probability, probability, 0)); // todo: add probability threshold
         } else {
            //Log.global.fine("no nearest neighbor...");
         }
      }
   }
}
