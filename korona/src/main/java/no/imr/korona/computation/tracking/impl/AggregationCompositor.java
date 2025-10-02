package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.Compositor;
import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.TargetCandidate;

import java.util.List;

public final class AggregationCompositor implements Compositor {
   public AggregationCompositor() {
   }

   @Override
   public Measurement compose(List<TargetCandidate> targetCandidates) {
      double rangeSum = 0;
      double alongSum = 0;
      double athwartSum = 0;
      double tsSum = 0;
      double weightSum = 0;

      for (TargetCandidate targetCandidate : targetCandidates) {
         double weight = Math.exp(-targetCandidate.getGateDistanceSq());
         Measurement measurement = targetCandidate.getMeasurement();
         rangeSum += weight * measurement.range();
         alongSum += weight * measurement.alongshipAngle();
         athwartSum += weight * measurement.athwartshipAngle();
         tsSum += weight * measurement.ts();
         weightSum += weight;
      }

      return new Measurement(
            (float) (rangeSum / weightSum),
            (float) (alongSum / weightSum),
            (float) (athwartSum / weightSum),
            (float) (tsSum / weightSum));
   }
}
