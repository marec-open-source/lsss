package no.imr.tools.math.irls;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class IrlsTest {
   @Test
   void testIRLS() {
      OneParameterModel fittableModel = new OneParameterModel();
      IRLS irls = new IRLS(new TukeyWeightFunction(6), 10, 1e-2, fittableModel);
      List<Observation> observations = new ArrayList<>();
      observations.add(new Observation(0.5, 0.5));
      observations.add(new Observation(0.6, 0.6));
      observations.add(new Observation(1.2, 1.2));
      irls.fitModel(observations);
      double parameterWithoutOutlier = fittableModel.parameter;
      observations.add(new Observation(0.1, 1.3)); //outlier
      irls.fitModel(observations);
      double parameterWithOutlier = fittableModel.parameter;

      assertEquals(parameterWithoutOutlier, parameterWithOutlier);
   }

   /**
    * A one-parameter linear model y = a * x.
    */
   private static final class OneParameterModel implements FittableModel {
      private double parameter;

      private OneParameterModel() {
      }

      @Override
      public boolean fitModel(List<WeightedObservation> observations) {
         //Weighted least squares fit of the model y = a * x;
         double num = 0;
         double denom = 0;
         for (WeightedObservation weightedObservation : observations) {
            Observation observation = weightedObservation.getObservation();
            num += weightedObservation.getWeight() * observation.x() * observation.y();
            denom += weightedObservation.getWeight() * observation.x() * observation.x();
         }
         if (denom == 0) {
            return false;
         }
         parameter = num / denom;
         return true;
      }

      @Override
      public double evaluate(double x) {
         return parameter * x;
      }
   }
}
