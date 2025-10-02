package no.imr.korona.computation.categorization;

import no.imr.korona.computation.feature.Feature;
import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

final class GaussDistributionTest {
   private record FeatureInfo(String name, float mean, float stdDev) {
   }

   @Test
   void nullForNoValidFeatures() {
      GaussDistribution gaussDistribution = new GaussDistribution(new Neighborhood(List.of(
            new Neighbor(List.of(new Feature("a", 1))),
            new Neighbor(List.of(new Feature("a", 2)))
      )));
      assertNotNull(gaussDistribution.probability(List.of(new Feature("a", 0.5f))));
      assertNull(gaussDistribution.probability(List.of(new Feature("b", 1))));
   }

   @Test
   void testCovarianceMatrix() {
      JUnitUtils.runWithRandom(GaussDistributionTest::testCovarianceMatrix);
   }

   private static void testCovarianceMatrix(Random random) {
      List<FeatureInfo> featureInfos = new ArrayList<>();
      featureInfos.add(new FeatureInfo("A", 10, 1));
      featureInfos.add(new FeatureInfo("B", 60, 50));
      featureInfos.add(new FeatureInfo("C", -5, 4));

      Neighborhood neighborhood1 = createRandomNeighborhood(featureInfos, random);

      GaussDistribution gaussDistribution = new GaussDistribution(neighborhood1);

      float det = gaussDistribution.getCovarianceMatrixDeterminant();
      assertTrue(det >= 0);

      //add a neighbourhood with 4 features
      featureInfos.clear();
      featureInfos.add(new FeatureInfo("A", 50, 5));
      featureInfos.add(new FeatureInfo("B", 70, 50000));
      featureInfos.add(new FeatureInfo("C", 0, 5));
      featureInfos.add(new FeatureInfo("D", 5000, 10000));

      Neighborhood neighborhood2 = createRandomNeighborhood(featureInfos, random);

      gaussDistribution = new GaussDistribution(List.of(neighborhood1, neighborhood2));
      float det2 = gaussDistribution.getCovarianceMatrixDeterminant();
      /*
      if (det2 < 0) {
         System.out.println("seed = " + seed);
      }
      */
      assertTrue(det2 >= 0);

      //change the order the neighborhoods are added
      GaussDistribution gaussDistributionReversed = new GaussDistribution(List.of(neighborhood2, neighborhood1));

      float det3 = gaussDistributionReversed.getCovarianceMatrixDeterminant();
      assertEquals(det2, det3, 1e-9 * det2);

      featureInfos.add(new FeatureInfo("E", -20, 5));
      Neighborhood neighborhood3 = createRandomNeighborhood(featureInfos, random);

      GaussDistribution gaussDistribution2 = new GaussDistribution(List.of(neighborhood3, neighborhood1));
      float det4 = gaussDistribution2.getCovarianceMatrixDeterminant();
      assertTrue(det4 >= 0);

      // A renaming of the features can give a different sorting internally in hashmaps,
      // but the result of the covariance computation should remain the same.
      List<Neighbor> renamedFeatureNeighbor = new ArrayList<>();
      for (Neighbor neighbor : neighborhood3.getNeighbors()) {
         //rename feature "E"
         List<Feature> renamedFeatures = new ArrayList<>();
         for (Feature feature : neighbor.getFeatures()) {
            if (feature.name().equals("E")) {
               renamedFeatures.add(new Feature("EEEE", feature.value()));
            } else {
               renamedFeatures.add(feature);
            }
         }
         renamedFeatureNeighbor.add(new Neighbor(renamedFeatures));
      }
      Neighborhood neighborhood4 = new Neighborhood(renamedFeatureNeighbor);

      GaussDistribution gaussDistribution3 = new GaussDistribution(List.of(neighborhood4, neighborhood1));
      float det5 = gaussDistribution3.getCovarianceMatrixDeterminant();
      assertEquals(det4, det5, 1e-9 * det4);
   }

   @Test
   void testFeaturesMissingInNeighborhood() {
      JUnitUtils.runWithRandom(GaussDistributionTest::testFeaturesMissingInNeighborhood);
   }

   private static void testFeaturesMissingInNeighborhood(Random random) {
      List<FeatureInfo> featureInfos = new ArrayList<>();
      featureInfos.add(new FeatureInfo("A", 10, 1));
      featureInfos.add(new FeatureInfo("B", 60, 50));
      featureInfos.add(new FeatureInfo("C", -5, 4));
      Neighborhood neighborhood1 = createRandomNeighborhood(featureInfos, random);
      featureInfos.remove(2);
      Neighborhood neighborhood2 = createRandomNeighborhood(featureInfos, random);

      GaussDistribution gaussDistributionSeparate = new GaussDistribution(List.of(neighborhood1, neighborhood2));
      float detSeparate = gaussDistributionSeparate.getCovarianceMatrixDeterminant();
      assertTrue(detSeparate >= 0);

      List<Neighbor> combinedNeighbors = new ArrayList<>();
      combinedNeighbors.addAll(neighborhood1.getNeighbors());
      combinedNeighbors.addAll(neighborhood2.getNeighbors());

      Neighborhood neighborhoodCombined = new Neighborhood(combinedNeighbors);
      GaussDistribution gaussDistributionCombined = new GaussDistribution(List.of(neighborhoodCombined));
      float detCombined = gaussDistributionCombined.getCovarianceMatrixDeterminant();
      assertTrue(detCombined >= 0);

      assertEquals(detSeparate, detCombined, 1e-9 * detSeparate);
   }

   private static Neighborhood createRandomNeighborhood(List<FeatureInfo> featureInfos, Random random) {
      List<Neighbor> neighbors = new ArrayList<>();
      // Minimum featureInfos.size to avoid possible rank deficient matrix.
      int neighborHoodSize = random.nextInt(featureInfos.size(), 10000);
      for (int i = 0; i < neighborHoodSize; i++) {
         List<Feature> features = new ArrayList<>();
         for (FeatureInfo featureInfo : featureInfos) {
            features.add(new Feature(featureInfo.name, random.nextGaussian(featureInfo.mean, featureInfo.stdDev)));
         }
         Neighbor neighbor = new Neighbor(features);
         neighbors.add(neighbor);
      }

      return new Neighborhood(neighbors);
   }
}
