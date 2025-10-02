package no.imr.tools.math.irls;

import java.util.List;

public interface FittableModel {

   boolean fitModel(List<WeightedObservation> observations);

   double evaluate(double x);

   default void updateResiduals(List<WeightedObservation> observations) {
      for (WeightedObservation weightedObservation : observations) {
         Observation observation = weightedObservation.getObservation();
         weightedObservation.setResidual(observation.y() - evaluate(observation.x()));
      }
   }

   /**
    * Computes an estimate of the variance from observations with non-zero weights.
    *
    * @param observations the weighted observations
    * @return the estimate of variance
    */
   default double computeEffectiveSampleVariance(List<WeightedObservation> observations) {
      int nonZeroWeights = 0;
      double var = 0;
      for (WeightedObservation weightedObservation : observations) {
         if (weightedObservation.getWeight() > Double.MIN_VALUE) {
            nonZeroWeights++;
            Observation observation = weightedObservation.getObservation();
            double residual = evaluate(observation.x()) - observation.y();
            var += residual * residual;
         }
      }
      return nonZeroWeights > 1 ? var / (nonZeroWeights - 1) : 0;
   }
}
