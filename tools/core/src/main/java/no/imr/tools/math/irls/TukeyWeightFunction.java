package no.imr.tools.math.irls;

import no.imr.tools.math.Median;

import java.util.List;

public final class TukeyWeightFunction implements AdaptableWeightFunction {
   private static final double MINIMUM_ALLOWED_MEDIAN_RESIDUAL = 1e-7;

   private final double constant;
   private double residualNormalization;

   public TukeyWeightFunction(double constant) {
      this.constant = constant;
   }

   @Override
   public double computeWeight(double residual) {
      if (Math.abs(residual) <= residualNormalization) {
         double r = residual / residualNormalization;
         double a = 1 - r * r;
         return a * a;
      }
      return 0;
   }

   @Override
   public void adapt(List<WeightedObservation> observations) {
      double[] absResiduals = new double[observations.size()];
      int i = 0;
      for (WeightedObservation observation : observations) {
         absResiduals[i] = Math.abs(observation.getResidual());
         i++;
      }
      double median = Median.quickSelect(absResiduals);
      median = Math.max(median, MINIMUM_ALLOWED_MEDIAN_RESIDUAL);
      residualNormalization = median * constant;
   }
}
