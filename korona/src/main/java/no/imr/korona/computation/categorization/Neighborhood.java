package no.imr.korona.computation.categorization;

import no.imr.korona.computation.feature.Feature;
import no.imr.korona.computation.feature.FeatureExtractor;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A collection of Neighbors defining a category.
 * Used when doing categorization by nearest neighbor searching.
 */
public final class Neighborhood {
   private final List<Neighbor> neighbors;
   //private KDTree kdTree;

   /**
    * Creates a new empty neighborhood.
    */
   public Neighborhood() {
      neighbors = List.of();
   }

   /**
    * Creates a new neighborhood.
    *
    * @param neighbors a list of neighbors
    */
   public Neighborhood(List<Neighbor> neighbors) {
      this.neighbors = neighbors;
   }

   /**
    * Creates a new neighborhood from XML.
    *
    * @param element an XML element
    */
   public Neighborhood(Element element) {
      neighbors = element.elements().stream()
            .map(Neighbor::new)
            .toList();
   }

   /**
    * Returns this neighborhood as an XML element.
    *
    * @return an XML element
    */
   public Element toXml() {
      Element element = DocumentHelper.createElement(Configurator.XML.NEIGHBORHOOD);
      for (Neighbor neighbor : neighbors) {
         element.add(neighbor.toXml());
      }
      return element;
   }

   /**
    * Saves this neighborhood to an XML file.
    *
    * @param file the XML file
    */
   public void save(Path file) throws IOException {
      XmlUtils.writeDocument(toXml(), file);
   }

   /**
    * Returns all neighbors in this neighborhood.
    *
    * @return a list of all neighbors
    */
   public List<Neighbor> getNeighbors() {
      return neighbors;
   }

   /**
    * Must be called before computing nearest neighbor.
    *
    * @see Neighborhood#findNearestNeighbor(Pixel, Metric)
    */
   public void makeKDTree() {
      //todo: fix KDTree
      //throw new UnsupportedOperationException();
      /*
      int N = ((Neighbor) neighbors.get(0)).getFeaturesByNames().size();
      kdTree = new KDTree(N);
      for (int i = 0; i < neighbors.size(); i++) {
         Neighbor neighbor = (Neighbor) neighbors.get(i);
         kdTree.add(neighbor.getFeatureValues(), neighbor);
      }
      rebuildKDTree();
      */
   }

   private void rebuildKDTree() {
      /*
      Log.global.fine("KDTree before rebuild: " + kdTree.getInfoString());
      kdTree.rebuild();
      Log.global.fine(" KDTree after rebuild: " + kdTree.getInfoString());
      */
   }

   /**
    * Performs a nearest neighbor search.
    *
    * @param pixel  the pixel whose feature vector is subject of the search
    * @param metric the metric to use
    * @return the nearest neighbor
    */
   NearestNeighbor findNearestNeighbor(Pixel pixel, Metric metric) {
      return findNearestNeighborBruteForce(pixel, metric);
      /*
      if (metric == Metric.EUCLIDEAN) {
         //compareBruteForceToKDTree(pixel, metric); // for testing
         return findNearestNeighborKDTree(pixel, metric);
      } else {
         return findNearestNeighborBruteForce(pixel, metric);
      }
      */
   }

   private NearestNeighbor findNearestNeighborKDTree(Pixel pixel, Metric metric) {
      //todo: fix KDTree
      throw new UnsupportedOperationException();
      /*
      nearestNeighbor = (Neighbor) kdTree.nearestNeighbor(pixel.getFeatureValues());
      if (nearestNeighbor != null) {
         nearestNeighbor.setDistance(metric.distance(pixel, nearestNeighbor));
      } else {
         String f = "";
         for (Iterator it = pixel.getFeaturesByNames().iterator(); it.hasNext();) {
            Feature feature = (Feature) it.next();
            f += feature.getValue() + " ";
         }
         Log.global.fine("no nearest neighbor for feature values " + f);
      }
      */
   }

   /**
    * Holds the result of a nearest neighbor search.
    */
   static final class NearestNeighbor {
      /**
       * The nearest neighbor.
       */
      @Nullable Neighbor neighbor;

      /**
       * The distance to the nearest neighbor.
       */
      float distance = Float.POSITIVE_INFINITY;

      private NearestNeighbor() {
      }

      private void update(Neighbor candidate, float dist) {
         if (dist < distance) {
            neighbor = candidate;
            distance = dist;
         }
      }
   }

   private NearestNeighbor findNearestNeighborBruteForce(Pixel pixel, Metric metric) {
      NearestNeighbor nearestNeighbor = new NearestNeighbor();
      for (Neighbor neighbor : neighbors) {
         nearestNeighbor.update(neighbor, metric.distance(pixel, neighbor));
      }
      return nearestNeighbor;
   }

   // For testing...
   private void compareBruteForceToKDTree(Pixel pixel, Metric metric) {
      NearestNeighbor kdNN = findNearestNeighborKDTree(pixel, metric);
      NearestNeighbor bfNN = findNearestNeighborBruteForce(pixel, metric);

      if (bfNN.neighbor != kdNN.neighbor) {
         Log.global.warning("KDTree disagreement " + (bfNN.distance - kdNN.distance));
      }
   }

   /**
    * Return the outliers according to a specified outlier fraction from this neighborhood.
    *
    * @param outlierFraction   the outlier fraction
    * @param featureExtractors the features to use
    * @param distribution      the distribution to test against
    * @return the outliers
    */
   public Collection<Neighbor> getOutliers(float outlierFraction, Collection<FeatureExtractor> featureExtractors,
                                           GaussDistribution distribution) {
      float pThreshold = GaussUtils.outlierFractionProbability(outlierFraction,
            distribution.getValidFeatureCount(featureExtractors));

      Collection<Neighbor> outliers = new ArrayList<>();
      for (Neighbor neighbor : neighbors) {
         GaussDistribution.Probability p = distribution.probability(neighbor.getFeatures(featureExtractors));
         if (p != null && p.unnormalized() < pThreshold) {
            outliers.add(neighbor);
         }
      }
      return outliers;
   }

   /**
    * Classifies the neighbors in this neighborhood into
    * a map from category to collection of neighbors.
    * The unknown category is represented in the map as {@code null}.
    *
    * @param outlierFraction   the outlier fraction
    * @param featureExtractors the features to use
    * @param categories        the categories to use
    * @return a map from category to collection of neighbors
    */
   public Map<@Nullable Category, Collection<Neighbor>> getClassificationMap(float outlierFraction, Collection<FeatureExtractor> featureExtractors,
                                                                             Collection<Category> categories) {
      Map<@Nullable Category, Collection<Neighbor>> classification = HashMap.newHashMap(1 + categories.size());
      classification.put(null, new ArrayList<>());
      for (Category category : categories) {
         classification.put(category, new ArrayList<>());
      }

      for (Neighbor neighbor : neighbors) {
         Collection<Feature> features = neighbor.getFeatures(featureExtractors);

         if (features.isEmpty()) {
            continue;
         }

         Category bestCategory = null;
         float maxP = 0;
         float probabilityThreshold = GaussUtils.outlierFractionProbability(outlierFraction,
               features.size());

         for (Category category : categories) {
            GaussDistribution distribution = category.getPixelCategoryDistribution().getGaussDistribution();

            if (distribution.isEmpty()) {
               continue;
            }

            GaussDistribution.Probability p = distribution.probability(features);
            if (p != null && p.unnormalized() >= probabilityThreshold && p.normalized() >= maxP) {
               bestCategory = category;
               maxP = p.normalized();
            }
         }

         classification.get(bestCategory).add(neighbor);
      }
      return classification;
   }
}
