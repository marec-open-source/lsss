package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.Predictor;
import no.imr.korona.computation.tracking.data.StateVector;
import no.imr.korona.computation.tracking.data.TargetPoint;
import no.imr.korona.computation.tracking.data.Track;
import no.imr.korona.computation.tracking.data.TrackPoint;
import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.time.NTDate;

public final class SimplePredictor implements Predictor {
   public SimplePredictor() {
   }

   @Override
   public StateVector predict(Track track, PingIndex pingIndex) {
      TrackPoint point = track.getLastPointWithEstimate();
      float dSec = (pingIndex.getNTDate() - point.getPingIndex().getNTDate()) / (float) NTDate.UNITS_PER_SECOND;
      TargetPoint estimate = point.getEstimate();
      assert estimate != null;
      StateVector stateVector = estimate.stateVector();
      Vec3 p = stateVector.position();
      Vec3 v = stateVector.velocity();
      Vec3 predictedPos = p.plus(v.times(dSec));
      return new StateVector(predictedPos, stateVector.velocity(), stateVector.ts());
   }
}
