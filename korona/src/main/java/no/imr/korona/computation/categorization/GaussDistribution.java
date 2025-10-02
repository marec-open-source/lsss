package no.imr.korona.computation.categorization;

import no.imr.korona.computation.feature.Feature;
import no.imr.korona.computation.feature.FeatureExtractor;
import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.logging.Log;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.math.Sum;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GaussDistribution {
   public static final int MAX_EM_ITERATIONS = 10;
   private static final float EM_CONVERGENCE_CRITERION = 0.1f;

   private final ComputationTreeNode computationTreeRoot = new ComputationTreeNode();
   private final float[] actualValueMinusMeans; // to avoid allocation in probability calculation
   private CovarianceMatrix covarianceMatrix;
   private Means means;

   /**
    * Used for storing inverses of covariance matrices.
    */
   private static final class ComputationTreeNode {
      private @Nullable ComputationMemory computationMemory;
      private final Map<String, ComputationTreeNode> featureToNode = new HashMap<>();

      private ComputationTreeNode() {
      }

      private ComputationTreeNode getChild(Feature feature) {
         ComputationTreeNode child = featureToNode.get(feature.name());
         if (child == null) {
            child = new ComputationTreeNode();
            featureToNode.put(feature.name(), child);
         }
         return child;
      }
   }

   private static final class CovarianceMatrix {
      private final List<String> allFeatureNames;
      private final Map<FeaturePair, Number> covariances;
      private final Map<FeaturePair, Sum> covarianceSums = new HashMap<>();
      private final Map<List<String>, RealMatrix> cachedRegressionMatrices = new HashMap<>();

      private CovarianceMatrix(List<String> allFeatureNames) {
         this.allFeatureNames = allFeatureNames;
         covariances = new HashMap<>();
      }

      private CovarianceMatrix(List<String> allFeatureNames, Map<FeaturePair, Number> covariances) {
         this.allFeatureNames = allFeatureNames;
         this.covariances = covariances;
      }

      private void addCovariance(FeaturePair key, double value) {
         Sum sum = covarianceSums.computeIfAbsent(key, k -> new Sum());
         sum.add(value);
      }

      private void computeCovariances() {
         covariances.clear();
         for (Map.Entry<FeaturePair, Sum> entry : covarianceSums.entrySet()) {
            Sum sum = entry.getValue();
            if (sum.getCount() > 1) {
               double covariance = sum.getSum() / (sum.getCount() - 1);
               covariances.put(entry.getKey(), covariance);
            }
         }
      }

      private List<Feature> computeMissingValues(List<Feature> availableFeatures, List<String> missingFeatures,
                                                 Map<String, Number> means) {
         RealMatrix regressionMatrix = getEMRegressionMatrix(missingFeatures);
         //sort available features in the same order as allFeatureNames
         availableFeatures.sort(Comparator.comparingInt(feature -> allFeatureNames.indexOf(feature.name())));
         //Sort missing features in the same order as allFeatureNames
         missingFeatures.sort(Comparator.comparingInt(allFeatureNames::indexOf));
         double[] availableMinusMean = new double[availableFeatures.size()];
         int i = 0;
         for (Feature availableFeature : availableFeatures) {
            availableMinusMean[i] = availableFeature.value() - means.get(availableFeature.name()).doubleValue();
            i++;
         }
         double[] missingEstimates = regressionMatrix.preMultiply(availableMinusMean);
         i = 0;
         for (String missingFeature : missingFeatures) {
            missingEstimates[i] += means.get(missingFeature).doubleValue();
            i++;
         }
         List<Feature> result = new ArrayList<>();
         i = 0;
         for (String missingFeature : missingFeatures) {
            result.add(new Feature(missingFeature, missingEstimates[i]));
            i++;
         }
         return result;
      }

      private RealMatrix getEMRegressionMatrix(List<String> missingFeatures) {
         if (covariances.isEmpty()) {
            //Matrix has not been computed. The regression coefficients can all be set to zero.
            int columnDim = missingFeatures.size();
            int rowDim = allFeatureNames.size() - missingFeatures.size();
            RealMatrix result = new Array2DRowRealMatrix(rowDim, columnDim);
            cachedRegressionMatrices.put(missingFeatures, result);
            return result;
         }
         RealMatrix realMatrix = cachedRegressionMatrices.get(missingFeatures);
         if (realMatrix != null) {
            return realMatrix;
         }
         RealMatrix sigmaAA = getSigmaAA(missingFeatures);
         RealMatrix sigmaAM = getSigmaAM(missingFeatures);

         DecompositionSolver sigmaAASolver = new LUDecomposition(sigmaAA).getSolver();
         if (!sigmaAASolver.isNonSingular()) {
            //Return an empty matrix (no regression possible)
            int columnDim = missingFeatures.size();
            int rowDim = allFeatureNames.size() - missingFeatures.size();
            RealMatrix result = new Array2DRowRealMatrix(rowDim, columnDim);
            cachedRegressionMatrices.put(missingFeatures, result);
            return result;
         }

         RealMatrix result = sigmaAASolver.getInverse().multiply(sigmaAM);
         cachedRegressionMatrices.put(missingFeatures, result);
         return result;
      }

      private RealMatrix getSigmaAA(List<String> missingFeatures) {
         int dim = allFeatureNames.size() - missingFeatures.size();
         double[][] values = new double[dim][dim];
         int i = 0;
         for (String featureNameX : allFeatureNames) {
            if (!missingFeatures.contains(featureNameX)) {
               int j = 0;
               for (String featureNameY : allFeatureNames) {
                  if (!missingFeatures.contains(featureNameY)) {
                     Number number = covariances.get(new FeaturePair(featureNameX, featureNameY));
                     if (number != null) {
                        values[i][j] = number.doubleValue();
                     }
                     j++;
                  }
               }
               i++;
            }
         }
         return new Array2DRowRealMatrix(values);
      }

      private RealMatrix getSigmaAM(List<String> missingFeatures) {
         int columnDim = missingFeatures.size();
         int rowDim = allFeatureNames.size() - missingFeatures.size();
         double[][] values = new double[rowDim][columnDim];
         int i = 0;
         for (String featureNameX : allFeatureNames) {
            if (!missingFeatures.contains(featureNameX)) {
               int j = 0;
               for (String featureNameY : missingFeatures) {
                  values[i][j] = covariances.get(new FeaturePair(featureNameX, featureNameY)).doubleValue();
                  j++;
               }
               i++;
            }
         }
         return new Array2DRowRealMatrix(values);
      }

      private float maxRelativeDiff(CovarianceMatrix otherCovarianceMatrix) {
         float maxDiff = 0;
         for (Map.Entry<FeaturePair, Number> entry : covariances.entrySet()) {
            Number otherNumber = otherCovarianceMatrix.covariances.get(entry.getKey());
            if (otherNumber == null) {
               return Float.MAX_VALUE;
            }
            double thisValue = entry.getValue().doubleValue();
            double otherValue = otherNumber.doubleValue();
            if (thisValue == 0 && otherValue == 0) {
               continue;
            }
            if (otherValue == 0) {
               return Float.MAX_VALUE;
            }
            maxDiff = Math.max(maxDiff, (float) (Math.abs(thisValue - otherValue) / Math.abs(otherValue)));
         }
         return maxDiff;
      }

      private float determinant() {
         RealMatrix fullMatrix = getSigmaAA(new ArrayList<>());
         return (float) new LUDecomposition(fullMatrix).getDeterminant();
      }
   }

   private static final class Means {
      private final Map<String, Sum> featureToMeanSums = new HashMap<>();
      private final Map<String, Number> featureToMeans = new HashMap<>();

      private Means() {
      }

      private void updateMeanSums(Neighbor neighbor) {
         for (Feature feature : neighbor.getFeatures()) {
            Sum sum = featureToMeanSums.computeIfAbsent(feature.name(), k -> new Sum());
            sum.add(feature.value());
         }
      }

      private void computeMeans() {
         featureToMeans.clear();
         for (Map.Entry<String, Sum> entry : featureToMeanSums.entrySet()) {
            Sum sum = entry.getValue();
            double mean = sum.getAverage();
            featureToMeans.put(entry.getKey(), mean);
         }
      }

      private void computeMeans(Neighborhood neighborhood) {
         for (Neighbor neighbor : neighborhood.getNeighbors()) {
            updateMeanSums(neighbor);
         }
         computeMeans();
      }

      private float maxRelativeDiff(Means otherMeans) {
         float maxDiff = 0;
         for (Map.Entry<String, Number> entry : featureToMeans.entrySet()) {
            Number otherNumber = otherMeans.featureToMeans.get(entry.getKey());
            if (otherNumber == null) {
               return Float.MAX_VALUE;
            }
            double thisValue = entry.getValue().doubleValue();
            double otherValue = otherNumber.doubleValue();
            if (thisValue == 0 && otherValue == 0) {
               continue;
            }
            if (otherValue == 0) {
               return Float.MAX_VALUE;
            }
            maxDiff = Math.max(maxDiff, (float) (Math.abs(thisValue - otherValue) / Math.abs(otherValue)));
         }
         return maxDiff;
      }
   }

   /**
    * Constructs a distribution by estimating means and covariances
    * using the neighbors in a neighborhood.
    *
    * @param neighborhood a neighborhood
    */
   public GaussDistribution(Neighborhood neighborhood) {
      this(List.of(neighborhood));
   }

   public GaussDistribution(Collection<Neighborhood> neighborhoods) {
      this(neighborhoods, new AsyncHandle(), ProgressHandler.ignore());
   }

   public GaussDistribution(Collection<Neighborhood> neighborhoods, AsyncHandle asyncHandle, ProgressHandler progressHandler) {
      means = new Means();
      Means nextMeans = new Means();
      //first compute means for all neighborhoods
      for (Neighborhood neighborhood : neighborhoods) {
         means.computeMeans(neighborhood);
      }

      List<String> allFeatureNames = new ArrayList<>(means.featureToMeans.keySet());
      covarianceMatrix = new CovarianceMatrix(allFeatureNames);
      CovarianceMatrix nextCovarianceMatrix = new CovarianceMatrix(allFeatureNames);

      if (means.featureToMeans.isEmpty()) {
         actualValueMinusMeans = Utils.EMPTY_FLOAT_ARRAY;
         return; //nothing to do
      }

      float maxDiff = Float.MAX_VALUE;
      mainLoop:
      for (int i = 0; i < MAX_EM_ITERATIONS; i++) {
         for (Neighborhood neighborhood : neighborhoods) {
            if (asyncHandle.isCancelled()) {
               break mainLoop;
            }
            updateNextCovarianceMatrix(neighborhood, means, covarianceMatrix, nextMeans, nextCovarianceMatrix);
         }
         nextCovarianceMatrix.computeCovariances();
         nextMeans.computeMeans();

         //Convergence if the maximum relative difference of the means and of the variances and covariances
         //is less than the criterion. The test is performed after the second iteration, when all vectors for all
         //iteration steps are initialized.
         if (i >= 1) {
            maxDiff = Math.max(nextMeans.maxRelativeDiff(means), nextCovarianceMatrix.maxRelativeDiff(covarianceMatrix));
         }
         covarianceMatrix = nextCovarianceMatrix;
         means = nextMeans;
         progressHandler.setProgress((i + 1) / (double) MAX_EM_ITERATIONS);
         if (maxDiff < EM_CONVERGENCE_CRITERION) {
            break;
         }
         nextCovarianceMatrix = new CovarianceMatrix(allFeatureNames);
         nextMeans = new Means();
      }
      actualValueMinusMeans = new float[means.featureToMeans.size()];
   }

   /**
    * Creates a new distribution from XML.
    *
    * @param categoryElement an XML element
    */
   public GaussDistribution(Element categoryElement) {
      means = new Means();
      List<String> featureNames = new ArrayList<>();
      Map<FeaturePair, Number> covariances = new HashMap<>();
      // iterate over all features for a category
      for (Element featureElement : categoryElement.elements(Configurator.XML.FEATURE)) {
         String featureName = featureElement.attributeValue(Configurator.XML.NAME);
         featureNames.add(featureName);
         //means.put(featureName, Float.valueOf(featureElement.attributeValue(Configurator.XML.MEAN)));
         means.featureToMeans.put(featureName, Float.valueOf(featureElement.attributeValue(Configurator.XML.MEAN)));
         // iterate over all covariances for a feature
         for (Element covarianceElement : featureElement.elements(Configurator.XML.COVARIANCE)) {
            String otherFeatureName = covarianceElement.attributeValue(Configurator.XML.NAME);
            covariances.put(new FeaturePair(featureName, otherFeatureName), Float.valueOf(covarianceElement.getTextTrim()));
         }
      }
      covarianceMatrix = new CovarianceMatrix(featureNames, covariances);
      actualValueMinusMeans = new float[means.featureToMeans.size()];
   }

   /**
    * Returns this distribution as an XML element.
    *
    * @return this distribution as an XML element
    */
   public Element toXml() {
      Element element = DocumentHelper.createElement(Configurator.XML.GAUSS);
      for (String featureNameX : means.featureToMeans.keySet()) {
         Element featureElement = element.addElement(Configurator.XML.FEATURE)
               .addAttribute(Configurator.XML.NAME, featureNameX)
               .addAttribute(Configurator.XML.MEAN, Utils.toString(getMean(featureNameX)));
         for (String featureNameY : means.featureToMeans.keySet()) {
            featureElement.addElement(Configurator.XML.COVARIANCE)
                  .addAttribute(Configurator.XML.NAME, featureNameY)
                  .addText(Utils.toString(getCovariance(featureNameX, featureNameY)));
         }
      }
      return element;
   }

   /**
    * Returns the number of features in a collection of features
    * that this distribution is valid for.
    *
    * @param featureExtractors a collection of FeatureExtractors
    * @return the number of features in a collection of features
    */
   public int getValidFeatureCount(Collection<FeatureExtractor> featureExtractors) {
      int validFeatureCount = 0;
      for (FeatureExtractor featureExtractor : featureExtractors) {
         if (isValid(featureExtractor.getFeatureName())) {
            validFeatureCount++;
         }
      }
      return validFeatureCount;
   }

   /**
    * Returns the quantile value of this distribution for a specified outlier fraction.
    *
    * @param outlierFraction an outlier fraction
    * @param activeFeatures  the {@link FeatureExtractor}s to use
    * @return the quantile value of this distribution for a specified outlier fraction
    */
   public float getQuantile(float outlierFraction, Collection<FeatureExtractor> activeFeatures) {
      return GaussUtils.quantileValue(outlierFraction, getValidFeatureCount(activeFeatures));
   }

   /**
    * Returns the feature names in this distribution.
    *
    * @return the feature names in this distribution
    */
   public Collection<String> getFeatureNames() {
      return means.featureToMeans.keySet();
   }

   public boolean hasMean(String featureName) {
      return means.featureToMeans.containsKey(featureName);
   }

   /**
    * Returns the mean for a specified feature.
    *
    * @param featureName the name of the feature
    * @return the mean value, or {@code NaN} if this distribution is not valid for
    * the specified feature.
    */
   public float getMean(String featureName) {
      Number value = means.featureToMeans.get(featureName);
      return value != null ? value.floatValue() : Float.NaN;
   }

   /**
    * Returns the variance for a specified features.
    *
    * @param featureName a feature name
    * @return the variance, or {@code NaN} if this distribution is not valid for
    * the specified feature
    */
   public float getVariance(String featureName) {
      return getCovariance(featureName, featureName);
   }

   /**
    * Returns the covariance for two specified features.
    *
    * @param featureNameX one feature name
    * @param featureNameY another feature name
    * @return the covariance, or {@code NaN} if this distribution is not valid for
    * the specified features
    */
   public float getCovariance(String featureNameX, String featureNameY) {
      Number value = covarianceMatrix.covariances.get(new FeaturePair(featureNameX, featureNameY));
      return value != null ? value.floatValue() : Float.NaN;
   }

   /**
    * Returns {@code true} if and only if this distribution has no valid features.
    *
    * @return {@code true} if and only if this distribution has no valid features
    */
   public boolean isEmpty() {
      return covarianceMatrix.covariances.isEmpty();
   }

   /**
    * Returns {@code true} if and only if this distribution is valid for a specified feature.
    *
    * @param featureName a feature name
    * @return {@code true} if and only if this distribution is valid for a specified feature
    */
   public boolean isValid(String featureName) {
      return isValid(featureName, featureName);
   }

   /**
    * Returns {@code true} if and only if this distribution is valid for a specified
    * pair of features.
    *
    * @param featureNameX a feature name
    * @param featureNameY another feature name
    * @return {@code true} if and only if this distribution is valid for a specified
    * pair of features
    */
   public boolean isValid(String featureNameX, String featureNameY) {
      return covarianceMatrix.covariances.containsKey(new FeaturePair(featureNameX, featureNameY));
   }

   private int validCount(Collection<Feature> features) {
      int n = 0;
      for (Feature feature : features) {
         if (isValid(feature.name())) {
            n++;
         }
      }
      return n;
   }

   /**
    * The result of a calculation of the probability for a feature list.
    *
    * @param normalized   the normalized probability value (integral = 1)
    * @param unnormalized the bounded probability value (max = 1)
    * @param normalizer   the normalizing factor
    * @see GaussDistribution#probability(Collection)
    */
   public record Probability(float normalized, float unnormalized, float normalizer) {
   }

   /**
    * Computes the probability for a specified collection of features.
    *
    * @param features the features
    * @return the probability for a specified collection of features,
    * or {@code null} if it cannot be computed
    */
   public @Nullable Probability probability(Collection<Feature> features) {
      int n = 0;
      ComputationTreeNode node = computationTreeRoot;
      for (Feature feature : features) {
         String featureName = feature.name();
         if (isValid(featureName)) {
            actualValueMinusMeans[n++] = feature.value() - getMean(featureName);
            node = node.getChild(feature);
         }
      }

      if (node.computationMemory == null) {
         node.computationMemory = new ComputationMemory(this, features);
      }

      return node.computationMemory.probability(actualValueMinusMeans);
   }

   private static final class ComputationMemory {
      /**
       * -0.5 * inverse (covariance matrix).
       */
      private final float @Nullable [][] matrix;
      private final float normalizer;

      private ComputationMemory(GaussDistribution distribution, Collection<Feature> features) {
         int n = distribution.validCount(features);
         if (n == 0) {
            matrix = null;
            normalizer = Float.NaN;
            return;
         }
         double[][] validCovariances = new double[n][n];
         int i = 0;
         for (Feature featureX : features) {
            String featureNameX = featureX.name();
            if (distribution.isValid(featureNameX)) {
               int j = 0;
               for (Feature featureY : features) {
                  String featureNameY = featureY.name();
                  if (distribution.isValid(featureNameY)) {
                     validCovariances[i][j] = distribution.getCovariance(featureNameX, featureNameY);
                     j++;
                  }
               }
               i++;
            }
         }
         Array2DRowRealMatrix m = new Array2DRowRealMatrix(validCovariances, false);
         LUDecomposition lu = new LUDecomposition(m);
         double det = lu.getDeterminant();
         if (det <= 0) {
            Log.global.fine("det = " + det + ", " + features);
            matrix = null;
            normalizer = Float.NaN;
            return;
         }
         RealMatrix inverse = lu.getSolver().getInverse();
         double[][] data = inverse.getData();
         for (double[] row : data) {
            ArrayMath.multiply(row, -0.5);
         }
         matrix = Utils.toFloats(data);
         normalizer = 1 / (float) Math.sqrt(Math.pow(2 * Math.PI, n) * det);
      }

      private @Nullable Probability probability(float[] x) {
         if (matrix == null) {
            return null;
         } else {
            float pUnnormalized = (float) Math.exp(GaussUtils.symmetricNorm(x, matrix));
            float pNormalized = normalizer * pUnnormalized;
            return new Probability(pNormalized, pUnnormalized, normalizer);
         }
      }
   }

   /**
    * Update the next covariance matrix by filling missing values in neighbors based on the
    * current covariance matrix.
    *
    * @param neighborhood            a set of neighbors
    * @param currentMeans            current mean of the features
    * @param currentCovarianceMatrix current covariance matrix
    * @param nextMeans               the means to be updated in this iteration
    * @param nextCovarianceMatrix    the covariance matrix to be updated in this iteration
    */
   private static void updateNextCovarianceMatrix(Neighborhood neighborhood,
                                                  Means currentMeans, CovarianceMatrix currentCovarianceMatrix,
                                                  Means nextMeans, CovarianceMatrix nextCovarianceMatrix) {
      if (neighborhood.getNeighbors().isEmpty()) {
         return;
      }
      for (Neighbor neighbor : neighborhood.getNeighbors()) {
         Neighbor filledNeighbor = neighbor;
         List<String> missingFeatures = new ArrayList<>();
         for (String featureName : currentMeans.featureToMeans.keySet()) {
            if (neighbor.getFeature(featureName) == null) {
               missingFeatures.add(featureName);
            }
         }
         if (!missingFeatures.isEmpty()) {
            List<Feature> features = new ArrayList<>();
            features.addAll(neighbor.getFeatures());
            features.addAll(currentCovarianceMatrix.computeMissingValues(features, missingFeatures, currentMeans.featureToMeans));
            filledNeighbor = new Neighbor(features);
         }
         assert filledNeighbor.getFeatures().size() == currentMeans.featureToMeans.size() : "Not enough features";
         nextMeans.updateMeanSums(filledNeighbor);
         for (Feature featureX : filledNeighbor.getFeatures()) {
            for (Feature featureY : filledNeighbor.getFeatures()) {
               FeaturePair key = new FeaturePair(featureX.name(), featureY.name());
               double xMean = currentMeans.featureToMeans.get(featureX.name()).doubleValue();
               double yMean = currentMeans.featureToMeans.get(featureY.name()).doubleValue();
               nextCovarianceMatrix.addCovariance(key, (featureX.value() - xMean) * (featureY.value() - yMean));
            }
         }
      }
   }

   public float getCovarianceMatrixDeterminant() {
      return covarianceMatrix.determinant();
   }

   private record FeaturePair(String featureNameX, String featureNameY) {
   }
}
