package no.imr.tools.math.irls;

import no.imr.tools.logging.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Solves an iteratively re-weighted least squares problem.
 */
public final class IRLS {
   private final AdaptableWeightFunction weightFunction;
   private final int maxIteration;
   private final double xi;
   private final FittableModel fittableModel;

   public IRLS(AdaptableWeightFunction weightFunction, int maxIteration, double xi, FittableModel fittableModel) {
      this.weightFunction = weightFunction;
      this.maxIteration = maxIteration;
      this.xi = xi;
      this.fittableModel = fittableModel;
   }

   public void fitModel(List<Observation> observations) {
      List<WeightedObservation> weightedObservations = new ArrayList<>();
      for (Observation observation : observations) {
         weightedObservations.add(new WeightedObservation(observation));
      }

      double chiSquared = 0;
      for (int i = 0; i < maxIteration; i++) {
         if (!fittableModel.fitModel(weightedObservations)) {
            Log.global.warning("Unable to fit model");
            break;
         }
         fittableModel.updateResiduals(weightedObservations);
         double previousChiSquared = chiSquared;
         chiSquared = calculateChiSquared(weightedObservations);
         double chiChange = Math.abs(chiSquared - previousChiSquared);
         if (chiChange < xi * previousChiSquared) {
            break;
         }

         weightFunction.adapt(weightedObservations);
         for (WeightedObservation weightedObservation : weightedObservations) {
            weightedObservation.setWeight(weightFunction.computeWeight(weightedObservation.getResidual()));
         }
      }
   }

   private static double calculateChiSquared(List<WeightedObservation> weightedObservations) {
      double chiSquared = 0;
      for (WeightedObservation weightedObservation : weightedObservations) {
         if (weightedObservation.getWeight() > Double.MIN_VALUE) {
            double r = weightedObservation.getResidual();
            chiSquared += r * r;
         }
      }
      return chiSquared;
   }
}
