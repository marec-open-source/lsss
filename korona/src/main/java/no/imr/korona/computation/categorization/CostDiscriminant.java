package no.imr.korona.computation.categorization;

import no.imr.tools.logging.Log;

import java.util.Arrays;
import java.util.List;

/**
 * Computes the discriminant based on a cost formulation.
 * The discriminant is set to minus the cost.
 */
final class CostDiscriminant extends SimpleSubModule {
   private final float[][] costMatrix;
   private final float[] cost;

   CostDiscriminant(CategorizationSubModule previousSubModule, Configurator configurator) {
      super(previousSubModule);

      int n = configurator.getMaxCategoryNumber() + 1;
      costMatrix = new float[n][n];
      for (Category estimated : configurator.getCategories()) {
         for (Category actual : configurator.getCategories()) {
            float cost = configurator.getCost(estimated, actual);
            costMatrix[estimated.getNumber()][actual.getNumber()] = cost;
         }
      }
      cost = new float[n];
      logCostMatrix();
   }

   private void logCostMatrix() {
      StringBuilder s = new StringBuilder("Cost matrix =\n");
      for (float[] row : costMatrix) {
         for (float v : row) {
            s.append(v).append(" \t");
         }
         s.append('\n');
      }
      Log.global.fine(s.toString());
   }

   @Override
   void process(ExtendedPing extendedPing, CategorizationPing categorizationPing) {
      for (Pixel pixel : categorizationPing.getPixels()) {
         if (pixel.isDone()) {
            continue;
         }
         List<CategoryData> categories = pixel.getCategoryDatas();
         Arrays.fill(cost, 0);
         for (CategoryData cdActual : categories) {
            int actualCategoryNumber = cdActual.getCategory().getNumber();
            float actualProbability = cdActual.getNormalizedProbability() * cdActual.getTotalApriori();
            for (CategoryData cdEstimated : categories) {
               int estimatedCategoryNumber = cdEstimated.getCategory().getNumber();
               cost[estimatedCategoryNumber] +=
                     costMatrix[estimatedCategoryNumber][actualCategoryNumber]
                           * actualProbability;
            }
         }
         for (CategoryData cd : categories) {
            int categoryNumber = cd.getCategory().getNumber();
            cd.setDiscriminant(-cost[categoryNumber]);
         }
      }
   }
}
