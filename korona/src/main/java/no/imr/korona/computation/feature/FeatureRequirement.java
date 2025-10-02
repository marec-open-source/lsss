package no.imr.korona.computation.feature;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for implementations compiled at runtime from generated sources.
 * <p>
 * Note: This class cannot be package private since its subclasses will have a different
 * class loader and thus not be in the same runtime package.
 */
public interface FeatureRequirement {
   FeatureRequirement ALWAYS_TRUE = new FeatureRequirement() {
      @Override
      public boolean isValid(Set<String> featureNames) {
         return true;
      }

      @Override
      public boolean isValid(Collection<Feature> features) {
         return true;
      }
   };

   boolean isValid(Set<String> featureNames);

   default boolean isValid(Collection<Feature> features) {
      Set<String> featureNames = HashSet.newHashSet(features.size());
      for (Feature feature : features) {
         featureNames.add(feature.name());
      }
      return isValid(featureNames);
   }

   default int count(boolean... featureNames) {
      int n = 0;
      for (boolean featureName : featureNames) {
         if (featureName) {
            n++;
         }
      }
      return n;
   }
}
