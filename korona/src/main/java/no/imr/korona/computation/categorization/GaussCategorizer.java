package no.imr.korona.computation.categorization;

import no.imr.korona.computation.categorization.apriori.PerPingAPriori;
import no.imr.korona.computation.feature.Feature;
import no.imr.korona.computation.feature.FeatureExtractor;
import no.imr.tools.Max;
import no.imr.tools.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Parametric classification using gaussian distributions.
 */
final class GaussCategorizer implements Categorizer {
   private final float deltaMinMaxSv38;
   private final float[] probabilityThresholds; // thresholds for different number of features
   private final List<Category> categoriesWithDistributions = new ArrayList<>();
   private final Category.DistributionLevel distributionLevel;
   private final boolean useSchoolApriori;
   private final Map<Category, Map<String, Float>> meanR = new HashMap<>();
   private final SimilarityMap similarityMap;

   private static final class SimilarityMap {
      private final Map<Integer, NavigableMap<Float, Float>> featureCountToProbabilityToSimilarity;

      private SimilarityMap(int maxFeatureCount, float resolution, float outlierFraction) {
         featureCountToProbabilityToSimilarity = makeSimilarityMap(maxFeatureCount, resolution, outlierFraction);
      }

      private static Map<Integer, NavigableMap<Float, Float>> makeSimilarityMap(int maxFeatureCount, float resolution, float outlierFraction) {
         Map<Integer, NavigableMap<Float, Float>> result = new HashMap<>();
         for (int i = 1; i < maxFeatureCount; i++) {
            NavigableMap<Float, Float> probabilityToSimilarity = new TreeMap<>();
            float similarity = 1;
            while (similarity > outlierFraction) {
               float probability = GaussUtils.outlierFractionProbability(similarity, i);
               probabilityToSimilarity.put(probability, similarity);
               similarity -= resolution;
            }
            result.put(i, probabilityToSimilarity);
         }
         return result;
      }

      private float getSimilarity(int featureCount, float probability) {
         NavigableMap<Float, Float> probabilityToSimilarity = featureCountToProbabilityToSimilarity.get(featureCount);
         if (probabilityToSimilarity == null) {
            return 0;
         }
         Map.Entry<Float, Float> floorEntry = probabilityToSimilarity.floorEntry(probability);
         return floorEntry != null ? floorEntry.getValue() : 0;
      }
   }

   GaussCategorizer(Category.DistributionLevel distributionLevel, boolean useSchoolApriori, Configurator configurator) {
      this.distributionLevel = distributionLevel;
      this.useSchoolApriori = useSchoolApriori;

      deltaMinMaxSv38 = configurator.deltaMinMaxSv38.getFloatValue();

      float outlierFraction = configurator.outlierFraction.getFloatValue();

      probabilityThresholds = new float[configurator.getFeatureExtractors().size()];
      for (int i = 0; i < probabilityThresholds.length; i++) {
         probabilityThresholds[i] = GaussUtils.outlierFractionProbability(outlierFraction, i);
      }

      for (Category category : configurator.getCategories()) {
         if (category.isActive() && !category.getCategoryDistribution(distributionLevel).getGaussDistribution().isEmpty()) {
            categoriesWithDistributions.add(category);
            meanR.put(category, getMeanR(configurator, category, distributionLevel));
         }
      }
      similarityMap = new SimilarityMap(configurator.getFeatureExtractors().size(), 0.005f, outlierFraction);
   }

   private static Map<String, Float> getMeanR(Configurator configurator, Category category, Category.DistributionLevel distributionLevel) {
      Map<String, Float> categoryMean = new HashMap<>();
      GaussDistribution distribution = category.getCategoryDistribution(distributionLevel).getGaussDistribution();
      Utils.getAllOfType(configurator.getOperationalFeatureExtractors(), FeatureExtractor.FrequencyFeatureExtractor.class).forEach(ffe -> {
         String name = ffe.getFeatureName();
         float mean = distribution.getMean(name);
         if (!Float.isNaN(mean)) {
            categoryMean.put(name, mean);
         }
      });
      return categoryMean;
   }

   @Override
   public void categorize(Pixel pixel, PerPingAPriori perPingAPriori) {
      for (Category category : categoriesWithDistributions) {
         List<Feature> features = pixel.getFeatures();
         if (!category.getFeatureRequirement().isValid(features)) {
            continue;
         }

         GaussDistribution distribution = category.getCategoryDistribution(distributionLevel).getGaussDistribution();
         GaussDistribution.Probability p = distribution.probability(features);
         float probabilityThreshold = probabilityThresholds[features.size()];
         if (p != null /* && p.unnormalized() > probabilityThreshold */) {
            float similarity = similarityMap.getSimilarity(features.size(), p.unnormalized());
            float aPriori = category.getApriori();
            if (useSchoolApriori && category.getSchoolApriori().isPresent()) {
               aPriori *= category.getSchoolApriori().get();
            }
            if (category.getMinLogSv38().isPresent() || category.getMaxLogSv38().isPresent()) {
               float svAPrioriFactor = computeAprioriFactor(pixel, category);
               if (svAPrioriFactor == 0) {
                  continue;
               }
               aPriori *= svAPrioriFactor;
            }
            Float perPingAPrioriFactor = perPingAPriori.getAPriori(category);
            if (perPingAPrioriFactor != null) {
               if (perPingAPrioriFactor == 0) {
                  continue;
               }
               aPriori *= perPingAPrioriFactor;
            }
            CategoryData categoryData = new CategoryData(category, 0, similarity, p.normalized(), probabilityThreshold * p.normalizer());
            categoryData.setApriori(aPriori);
            pixel.getCategoryDatas().add(categoryData);
         }
      }
   }

   private float computeAprioriFactor(Pixel pixel, Category category) {
      float logSv38 = pixel.getLogSv38();
      float minSv38 = category.getMinLogSv38().orElse(Float.NEGATIVE_INFINITY);
      float maxSv38 = category.getMaxLogSv38().orElse(Float.POSITIVE_INFINITY);

      float delta = Max.of(0, logSv38 - maxSv38, minSv38 - logSv38);

      // For other frequencies require that
      //        MinSv(f)            <= Sv(f)                   <= MaxSv(f)
      //   =>   MinSv38 + Mean R(f) <= Sv38 + R(f)             <= MaxSv38 + Mean R(f)
      //   =>   MinSv38             <= Sv38 + R(f) - Mean R(f) <= MaxSv38
      Map<String, Float> categoryMeanSv = meanR.get(category);
      for (Feature feature : pixel.getFeatures()) {
         Float featureMeanR = categoryMeanSv.get(feature.name());
         if (featureMeanR == null) {
            continue;
         }
         float value = logSv38 + feature.value() - featureMeanR;
         delta = Max.of(delta, value - maxSv38, minSv38 - value);
      }

      return 1 - Math.clamp(delta / deltaMinMaxSv38, 0, 1);
   }
}
