package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.Estimator;
import no.imr.korona.computation.tracking.PositionFunction;
import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.StateVector;
import no.imr.korona.computation.tracking.data.TargetPoint;
import no.imr.korona.computation.tracking.data.Track;
import no.imr.korona.computation.tracking.data.TrackPoint;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.time.TimeUtils;

public final class AlphaBetaEstimator implements Estimator {
   private final float alpha;
   private final float beta;

   public AlphaBetaEstimator(float alpha, float beta) {
      this.alpha = alpha;
      this.beta = beta;
   }

   @Override
   public StateVector estimate(Track track, TrackPoint trackPoint, PositionFunction positionFunction) {
      TargetPoint prediction = trackPoint.getPrediction();
      Measurement measurement = trackPoint.getMeasurement();

      Vec3 measuredPos = positionFunction.toGlobalPosition(measurement);
      Vec3 predictedPos = prediction.stateVector().position();
      Vec3 residual = measuredPos.minus(predictedPos);

      Vec3 pos = predictedPos.plus(residual.times(alpha));

      TrackPoint ref = track.getLastPointWithEstimate();
      float dt = (float) TimeUtils.toSeconds(ref.getPingIndex().getInstant(), trackPoint.getPingIndex().getInstant());

      Vec3 predictedVelocity = prediction.stateVector().velocity();
      Vec3 velocity = predictedVelocity.plus(residual.times(beta / dt));

      return new StateVector(pos, velocity, measurement.tsc());
   }
}
