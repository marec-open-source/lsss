package no.imr.lsss.modules.schoolparameter.perchannel;

import no.imr.tools.math.Histogram1D;

final class HistogramTruncatedMean {
   private HistogramTruncatedMean() {
   }

   static float getTruncatedMean(Histogram1D histogram1D, float truncationFactor) {
      int lowerBin = histogram1D.getLowerQuantileIndex(truncationFactor);
      int upperBin = histogram1D.getUpperQuantileIndex(truncationFactor);
      return computeLinearMean(histogram1D, lowerBin, upperBin);
   }

   private static float computeLinearMean(Histogram1D histogram1D, int lowerBin, int upperBin) {
      long sumOfWeights = 0;
      double weightedSum = 0;
      for (int bin = lowerBin; bin <= upperBin; bin++) {
         float binValue = HistogramMedian.computeLinearBinValue(histogram1D, bin);
         int weight = histogram1D.getCounts()[bin];
         weightedSum += binValue * weight;
         sumOfWeights += weight;
      }
      return (float) (weightedSum / sumOfWeights);
   }
}
