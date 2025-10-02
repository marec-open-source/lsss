package no.imr.korona.computation.categorization;

import no.imr.korona.computation.feature.Feature;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains feature vector and categorization results.
 */
final class Pixel {
   private boolean done = false;
   private final float logSv38;
   private final List<CategoryData> categoryDatas = new ArrayList<>();
   private final List<Feature> features = new ArrayList<>();

   Pixel(float logSv38) {
      this.logSv38 = logSv38;
   }

   float getLogSv38() {
      return logSv38;
   }

   /**
    * Set a final category for the pixel (e.g. noise or bottom)
    *
    * @param category the category
    */
   void setFinalCategory(Category category) {
      if (done) {
         return;
      }
      categoryDatas.clear();
      categoryDatas.add(new CategoryData(category, 1, 1, 1, 0));
      done = true;
   }

   /**
    * {@return the category with the largest discriminant}
    */
   @Nullable CategoryData getBestCategory() {
      CategoryData bestCategory = null;
      for (CategoryData category : categoryDatas) {
         if (bestCategory == null || category.getDiscriminant() > bestCategory.getDiscriminant()) {
            bestCategory = category;
         }
      }
      return bestCategory;
   }

   /**
    * {@return the features of this pixel}
    */
   List<Feature> getFeatures() {
      return features;
   }

   /**
    * Adds a feature to this pixel.
    *
    * @param feature the new feature
    */
   void addFeature(Feature feature) {
      features.add(feature);
   }

   /**
    * {@return all categories for this pixel}
    */
   List<CategoryData> getCategoryDatas() {
      return categoryDatas;
   }

   List<CategoryData> getAcceptableCategoryDatas() {
      List<CategoryData> a = new ArrayList<>();
      for (CategoryData cd : categoryDatas) {
         if (cd.isAcceptable()) {
            a.add(cd);
         }
      }
      return a;
   }

   /**
    * Test if this pixel reached its final category.
    *
    * @return true if this pixel's category is final, false otherwise
    */
   boolean isDone() {
      return done;
   }

   /**
    * Normalizes all discriminant values to be in the range [0,1].
    */
   void normalizeDiscriminants() {
      for (CategoryData cd : categoryDatas) {
         cd.setDiscriminant((float) (Math.atan(cd.getDiscriminant()) / Math.PI + 0.5));
      }
   }
}
