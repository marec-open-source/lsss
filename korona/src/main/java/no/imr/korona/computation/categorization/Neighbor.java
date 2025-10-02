package no.imr.korona.computation.categorization;

import no.imr.korona.computation.feature.CategoryVisualizer;
import no.imr.korona.computation.feature.Feature;
import no.imr.korona.computation.feature.FeatureExtractor;
import no.imr.tools.Utils;
import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A member of a {@link Neighborhood}.
 */
public class Neighbor {
   private final Map<String, Feature> nameToFeature;

   /**
    * Creates a neighbor with a specified list of features.
    *
    * @param features the features of this neighbor
    */
   public Neighbor(Collection<Feature> features) {
      nameToFeature = HashMap.newHashMap(features.size());
      for (Feature feature : features) {
         nameToFeature.put(feature.name(), feature);
      }
   }

   /**
    * Creates a neighbor from XML.
    *
    * @param element an XML element
    */
   public Neighbor(Element element) {
      List<Attribute> attributes = element.attributes();
      nameToFeature = HashMap.newHashMap(attributes.size());
      for (Attribute attribute : attributes) {
         String name = attribute.getName();
         float value = Float.parseFloat(attribute.getValue());
         nameToFeature.put(name, new Feature(name, value));
      }
   }

   /**
    * Returns this neighbor as an XML element.
    *
    * @return an XML element
    */
   public Element toXml() {
      Element element = DocumentHelper.createElement(Configurator.XML.NEIGHBOR);
      for (Feature feature : getFeatures()) {
         element.addAttribute(feature.name(), Utils.toString(feature.value()));
      }
      element.attributes().sort(Comparator.comparing(Attribute::getName));
      return element;
   }

   /**
    * Returns the feature of this neighbor with a specified name.
    *
    * @param featureName the name of the feature
    * @return the feature, or {@code null} is if this neighbor has no such feature
    */
   public @Nullable Feature getFeature(String featureName) {
      return nameToFeature.get(featureName);
   }

   /**
    * Returns a collection of named features. todo: remove later? - only use extractors to get features
    *
    * @param featureNames the feature names
    * @return a collection of the named features which this neighbor has
    */
   public Collection<Feature> getFeaturesByNames(Collection<String> featureNames) {
      Collection<Feature> features = new ArrayList<>(featureNames.size());
      for (String featureName : featureNames) {
         Feature feature = getFeature(featureName);
         if (feature != null) {
            features.add(feature);
         }
      }
      return features;
   }

   /**
    * Returns a collection of named features.
    *
    * @param featureExtractors the feature extractors
    * @return a collection of the named features which this neighbor has
    */
   public Collection<Feature> getFeatures(Collection<FeatureExtractor> featureExtractors) {
      return getFeaturesByNames(CategoryVisualizer.toFeatureNames(featureExtractors));
   }

   /**
    * Returns all features of this neighbor.
    *
    * @return a collection of all features
    */
   public Collection<Feature> getFeatures() {
      return nameToFeature.values();
   }
}
