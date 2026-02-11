package no.imr.korona.computation.categorization;

import no.imr.korona.computation.feature.Feature;
import no.imr.tools.logging.Log;
import no.imr.tools.plot.Graph;
import org.apache.commons.statistics.distribution.ChiSquaredDistribution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility functions.
 */
public final class GaussUtils {
   private GaussUtils() {
   }

   /**
    * Returns the quantile in the standard normal distribution for a specified
    * outlier fraction. The outlier fraction is the probability for a sample
    * to be further from the mean than the quantile.
    * <pre>
    * P(|X| &gt; q) = outlier fraction.
    * </pre>
    *
    * @param outlierFraction  the outlier fraction, between 0 and 1
    * @param numberOfFeatures the number of features
    * @return the quantile
    */
   public static float quantileValue(float outlierFraction, int numberOfFeatures) {
      if (outlierFraction == 0) {
         return Float.MAX_VALUE;
      } else if (numberOfFeatures <= 0) {
         return Float.MAX_VALUE;
      } else {
         ChiSquaredDistribution chiSq = ChiSquaredDistribution.of(numberOfFeatures);
         return (float) Math.sqrt(chiSq.inverseCumulativeProbability(1 - outlierFraction));
      }
   }

   /**
    * Returns the probability density value for the quantile value corresponding to
    * a specified outlier fraction.
    *
    * @param outlierFraction  the outlier fraction, between 0 and 1
    * @param numberOfFeatures the number of features
    * @return the probability density value
    * @see #quantileValue(float, int)
    */
   public static float outlierFractionProbability(float outlierFraction, int numberOfFeatures) {
      if (outlierFraction == 0) {
         return 0;
      } else if (numberOfFeatures == 0) {
         return 0; // todo: 0 or 1?
      } else {
         ChiSquaredDistribution chiSq = ChiSquaredDistribution.of(numberOfFeatures);
         double quantile = chiSq.inverseCumulativeProbability(1 - outlierFraction);
         return (float) Math.exp(-0.5 * quantile);
      }
   }

   /**
    * Computes x<sup>T</sup>Ax.
    *
    * @param x a vector
    * @param A a matrix
    * @return x<sup>T</sup>Ax
    */
   public static float norm(float[] x, float[][] A) {
      int n = A.length;
      float sum = 0;
      for (int i = 0; i < n; i++) {
         float[] Ai = A[i];
         float sum_j_Aij_xj = 0;
         for (int j = 0; j < n; j++) {
            sum_j_Aij_xj += Ai[j] * x[j];
         }
         sum += x[i] * sum_j_Aij_xj;
      }
      return sum;
   }

   /**
    * Computes x<sup>T</sup>Ax for symmetric A.
    *
    * @param x a vector
    * @param A a symmetric matrix
    * @return x<sup>T</sup>Ax
    */
   public static float symmetricNorm(float[] x, float[][] A) {
      int n = A.length;
      float sumDiag = 0;    // = sum A_ii * x_i * x_i
      float sumOffDiag = 0; // = sum A_ij * x_i * x_j for j > i
      for (int i = 0; i < n; i++) {
         float xi = x[i];
         float[] Ai = A[i];
         sumDiag += Ai[i] * xi * xi;
         float sum_j_Aij_xj = 0;
         for (int j = i + 1; j < n; j++) {
            sum_j_Aij_xj += Ai[j] * x[j];
         }
         sumOffDiag += xi * sum_j_Aij_xj;
      }
      return sumDiag + 2 * sumOffDiag;
   }

   /**
    * Assume the Gauss distribution comes from the target category and the neighborhood comes from the source category,
    * the function computes the overlap between the target and source categories by computing
    * the fraction of the points in the source category neighborhood that are
    * inside the ellipse determining whether points are accepted as the
    * target category.
    *
    * @param gaussDistribution the distribution
    * @param neighborhood      the neighborhood
    * @param outlierFraction   quantile determining the ellipse in the target category
    * @return a number between 0 and 1, where 0 means no overlaps and 1 full overlap
    */
   public static float computeOverlap(GaussDistribution gaussDistribution, Neighborhood neighborhood, float outlierFraction, Collection<String> featureNames) {
      if (gaussDistribution.isEmpty() || neighborhood.getNeighbors().isEmpty()) {
         return 0;
      }
      int count = 0;
      Collection<String> targetFeatures = gaussDistribution.getFeatureNames();
      // Find number of features in the target Gauss distribution, that are also in the featureNames collection.
      int numberOfFeatures = 0;
      for (String featureName : featureNames) {
         if (targetFeatures.contains(featureName)) {
            numberOfFeatures++;
         }
      }
      float probabilityThreshold = outlierFractionProbability(outlierFraction, numberOfFeatures);
      for (Neighbor nb : neighborhood.getNeighbors()) {
         GaussDistribution.Probability p = gaussDistribution.probability(nb.getFeaturesByNames(featureNames));
         if (p != null && p.unnormalized() > probabilityThreshold) {
            count++;
         }
      }
      return (float) count / neighborhood.getNeighbors().size();
   }

   /**
    * Computes the fraction of the category source that can be categorized as the targets categories.
    *
    * @param source          source category
    * @param targets         target categories
    * @param outlierFraction outlier fraction
    * @return list of categorized targets
    */
   public static List<Float> fractionCategorizedAs(Category source, Collection<Category> targets, float outlierFraction, Collection<String> featureNames) {
      Neighborhood nbh = source.getPixelCategoryDistribution().getNeighborhood();
      List<Float> result = new ArrayList<>();
      int[] counts = new int[targets.size()];
      int numberOfNeighborsInSource = 0;

      for (Neighbor nb : nbh.getNeighbors()) {
         float maxP = 0;
         int i = 0;
         int bestIndex = -1;
         Collection<Feature> features = nb.getFeaturesByNames(featureNames);
         if (features.isEmpty()) {
            continue;
         }
         float targetProbabilityThreshold = outlierFractionProbability(outlierFraction,
               features.size());
         numberOfNeighborsInSource++;
         for (Category target : targets) {
            if (target.getPixelCategoryDistribution().getGaussDistribution().isEmpty()) {
               i++;
               continue;
            }
            GaussDistribution.Probability pTarget = target.getPixelCategoryDistribution().getGaussDistribution().probability(features);
            if (pTarget != null && pTarget.unnormalized() >= targetProbabilityThreshold && pTarget.normalized() >= maxP) {
               bestIndex = i;
               maxP = pTarget.normalized();
            }
            i++;
         }
         if (bestIndex >= 0) {
            counts[bestIndex]++;
         }
      }
      for (int count : counts) {
         if (numberOfNeighborsInSource == 0) {
            result.add(Float.NaN);
         } else {
            result.add((float) count / numberOfNeighborsInSource);
         }
      }
      //add a result for unknown-categorization
      if (numberOfNeighborsInSource == 0) {
         result.add(Float.NaN);
      } else {
         int countSum = 0;
         for (int count : counts) {
            countSum += count;
         }
         result.add(1 - (float) countSum / numberOfNeighborsInSource);
      }
      return result;
   }

   /**
    * Draws a rotated ellipse on a graph.
    *
    * @param graph    the graph to draw the ellipse on
    * @param centerX  center point in the x-coordinates
    * @param centerY  center point in the x-coordinates
    * @param radiusX  semi axis in the x-direction
    * @param radiusY  semi axis in the y-direction
    * @param rotation rotation in radians
    */
   public static void drawEllipse(Graph graph, double centerX, double centerY,
                                  double radiusX, double radiusY, double rotation) {
      graph.addSeparator();
      double c = Math.cos(rotation);
      double s = Math.sin(rotation);
      for (int i = 0, n = 200; i <= n; i++) {
         double theta = i * 2 * Math.PI / n;
         double x = radiusX * Math.cos(theta);
         double y = radiusY * Math.sin(theta);
         double xRotated = c * x - s * y;
         double yRotated = s * x + c * y;
         graph.addPoint(centerX + xRotated, centerY + yRotated);
      }
   }

   /**
    * Draws a contour ellipse for a Gauss distribution for a given quantile.
    * Reference: Edwards and Penny, 'Calculus and analytic geometry', section 10.7, page 490.
    *
    * @param graph    the graph to draw the ellipse on
    * @param meanX    mean value for x
    * @param meanY    mean value for y
    * @param covXX    variance for x
    * @param covXY    covariance between x and y
    * @param covYY    variance for y
    * @param quantile the quantile
    */
   public static void drawGaussEllipse(Graph graph, double meanX, double meanY,
                                       double covXX, double covXY, double covYY,
                                       double quantile) {
      if (covXX == 0 || covYY == 0) {
         Log.global.fine("Degenerated ellipse: covXX = " + covXX + ", covYY = " + covYY);
         drawEllipse(graph, meanX, meanY, quantile * Math.sqrt(covXX), quantile * Math.sqrt(covYY), 0);
         return;
      }
      double rho = covXY / Math.sqrt(covXX * covYY);
      if (Math.abs(rho) > 1) {
         Log.global.fine("Unacceptable correlation = " + rho);
         return;
      }
      if (Math.abs(rho) == 1) {
         Log.global.fine("Degenerated ellipse, correlation = " + rho);
         return;
      }
      double tmp = 1 / (1 - rho * rho);
      double a = tmp / covXX;
      double b = -tmp * 2 * covXY / (covXX * covYY);
      double c = tmp / covYY;
      double discriminant = b * b - 4 * a * c;
      if (discriminant >= 0) {
         Log.global.warning("Unacceptable discriminant = " + discriminant);
         return;
      }
      double alpha = b == 0 ? 0 : Math.PI / 4 - Math.atan((a - c) / b) / 2;
      double cos = Math.cos(alpha);
      double sin = Math.sin(alpha);
      double at = a * cos * cos + b * cos * sin + c * sin * sin;
      double ct = a * sin * sin - b * sin * cos + c * cos * cos;
      drawEllipse(graph, meanX, meanY, quantile / Math.sqrt(at), quantile / Math.sqrt(ct), alpha);
   }
}
