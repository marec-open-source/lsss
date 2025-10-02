package no.imr.tools.math.irls;

import java.util.List;

public interface AdaptableWeightFunction {
   double computeWeight(double residual);

   void adapt(List<WeightedObservation> observations);
}
