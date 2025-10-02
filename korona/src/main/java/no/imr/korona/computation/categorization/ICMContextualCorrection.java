package no.imr.korona.computation.categorization;

import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * Updates the apriori probabilities based on neighbor category counts.
 * The new apriori probability for category c is set to exp(beta * n),
 * where n is the number of neighbors with category c.
 */
final class ICMContextualCorrection extends AbstractSubModule {
   private final float[] contextualApriori;
   private final int[] neighborCategoryCount;
   private final LinkedList<ExtendedPing> pingQueue = new LinkedList<>();

   ICMContextualCorrection(CategorizationSubModule previousSubModule, Configurator configurator) {
      super(previousSubModule);

      float beta = configurator.icmBeta.getFloatValue();
      Log.global.finer("beta = " + beta);
      neighborCategoryCount = new int[configurator.getMaxCategoryNumber() + 1];
      if (beta != 0.1f) {
         Log.global.log(Level.WARNING, "Beta != 0.1 (which is a good value of beta). Currently using beta = " + beta);
      }

      contextualApriori = new float[15]; // because max 14 neighbors
      for (int iNeighbors = 0; iNeighbors < contextualApriori.length; iNeighbors++) {
         double pMax = 5.0; //Want to keep correction due to many or few neighbours between <1/pMax, pMax>
         contextualApriori[iNeighbors] = (float) Math.clamp(Math.exp(beta * (iNeighbors - 3)), 1 / pMax, pMax);
      }
   }

   @Override
   public @Nullable ExtendedPing nextExtendedPing() throws IOException {
      if (pingQueue.size() < 3) {
         while (pingQueue.size() < 3) {
            ExtendedPing extendedPing = inputExtendedPing();
            if (extendedPing == null) {
               break;
            }
            pingQueue.addLast(extendedPing);
         }
         if (pingQueue.size() == 3) {
            CategorizationPing left = pingQueue.get(0).getCategorizationPing();
            CategorizationPing center = pingQueue.get(1).getCategorizationPing();
            CategorizationPing right = pingQueue.get(2).getCategorizationPing();
            if (left != null && center != null && right != null) {
               contextualCorrection(left, center, right);
            }
         }
      }
      return pingQueue.pollFirst();
   }

   private void contextualCorrection(CategorizationPing left, CategorizationPing center, CategorizationPing right) {
      Pixel[] leftPixels = left.getPixels();
      Pixel[] rightPixels = right.getPixels();
      Pixel[] centerPixels = center.getPixels();

      int leftOffset = center.getOffset() - left.getOffset();
      int rightOffset = center.getOffset() - right.getOffset();

      for (int iCenter = 0; iCenter < centerPixels.length; iCenter++) {
         Pixel centerPixel = centerPixels[iCenter];
         if (centerPixel.isDone()) {
            continue;
         }

         Arrays.fill(neighborCategoryCount, 0);
         /*
         countNeighborCategories(centerPixels, iCenter - 1, 1);
         countNeighborCategories(centerPixels, iCenter + 1, 1);
         countNeighborCategories(leftPixels, iCenter + leftOffset - 1, 3);
         countNeighborCategories(rightPixels, iCenter + rightOffset - 1, 3);
         */
         countNeighborCategories(centerPixels, iCenter - 2, 2);
         countNeighborCategories(centerPixels, iCenter + 1, 2);
         countNeighborCategories(leftPixels, iCenter + leftOffset - 2, 5);
         countNeighborCategories(rightPixels, iCenter + rightOffset - 2, 5);

         //CategoryData bestBefore = centerPixel.getBestCategory();
         for (CategoryData cd : centerPixel.getCategoryDatas()) {
            //cd.setApriori(getContextualApriori(neighborCategoryCount[cd.getCategory().getNumber()]));
            cd.setContextualApriori(getContextualApriori(neighborCategoryCount[cd.getCategory().getNumber()]));
         }
         /*
         CategoryData bestAfter = centerPixel.getBestCategory();
         if (bestAfter != bestAfter) {
            Log.global.fine("ICM category change: " + bestBefore.getCategory().getNumber()
                  + " -> " + bestAfter.getCategory().getNumber());
         }
         */
      }
   }

   private void contextualCorrection5Pings(
         CategorizationPing leftLeft,
         CategorizationPing left,
         CategorizationPing center,
         CategorizationPing right,
         CategorizationPing rightRight) {
      Pixel[] leftLeftPixels = leftLeft.getPixels();
      Pixel[] leftPixels = left.getPixels();
      Pixel[] rightPixels = right.getPixels();
      Pixel[] rightRightPixels = rightRight.getPixels();
      Pixel[] centerPixels = center.getPixels();

      int left_leftOffset = center.getOffset() - leftLeft.getOffset();
      int leftOffset = center.getOffset() - left.getOffset();
      int rightOffset = center.getOffset() - right.getOffset();
      int right_rightOffset = center.getOffset() - rightRight.getOffset();

      for (int iCenter = 0; iCenter < centerPixels.length; iCenter++) {
         Pixel centerPixel = centerPixels[iCenter];
         if (centerPixel.isDone()) {
            continue;
         }

         Arrays.fill(neighborCategoryCount, 0);
         countNeighborCategories(centerPixels, iCenter - 2, 2);
         countNeighborCategories(centerPixels, iCenter + 1, 2);
         countNeighborCategories(leftLeftPixels, iCenter + left_leftOffset - 2, 5);
         countNeighborCategories(leftPixels, iCenter + leftOffset - 2, 5);
         countNeighborCategories(rightPixels, iCenter + rightOffset - 2, 5);
         countNeighborCategories(rightRightPixels, iCenter + right_rightOffset - 2, 5);

         for (CategoryData cd : centerPixel.getCategoryDatas()) {
            cd.setContextualApriori(getContextualApriori(neighborCategoryCount[cd.getCategory().getNumber()]));
         }
      }
   }

   private float getContextualApriori(int neighborCount) {
      return contextualApriori[neighborCount];
   }

   private void countNeighborCategories(Pixel[] pixels, int startIndex, int indexCount) {
      int start = Math.max(0, startIndex);
      int end = Math.min(pixels.length, startIndex + indexCount);
      for (int i = start; i < end; i++) {
         CategoryData cd = pixels[i].getBestCategory();
         if (cd != null) {
            neighborCategoryCount[cd.getCategory().getNumber()]++;
         }
      }
   }
}
