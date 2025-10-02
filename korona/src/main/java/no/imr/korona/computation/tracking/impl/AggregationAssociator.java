package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.Associator;
import no.imr.korona.computation.tracking.GateFunction;
import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.TargetCandidate;
import no.imr.korona.computation.tracking.data.Track;

import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class AggregationAssociator implements Associator {
   private final int maxMissingSamples;

   public AggregationAssociator(int maxMissingSamples) {
      this.maxMissingSamples = maxMissingSamples;
   }

   @Override
   public int getMaxMissingSamples() {
      return maxMissingSamples;
   }

   @Override
   public void associate(Collection<Track> tracks, List<TargetCandidate> targetCandidates, GateFunction gateFunction) {
      NavigableMap<Float, Integer> rangeToCandidateIndex = makeRangeToIndexMap(targetCandidates);

      for (Track track : tracks) {
         Measurement predictedMeasurement = track.getLastPoint().getPrediction().measurement();
         float predictedRange = predictedMeasurement.range();
         float minRange = predictedRange - gateFunction.getUnacceptableRangeDistance();
         float maxRange = predictedRange + gateFunction.getUnacceptableRangeDistance();
         for (int i = rangeToCandidateIndex.floorEntry(minRange).getValue(); i < targetCandidates.size(); i++) {
            TargetCandidate targetCandidate = targetCandidates.get(i);
            Measurement candidateMeasurement = targetCandidate.getMeasurement();
            if (candidateMeasurement.range() > maxRange) {
               break;
            }
            float gateDistance2 = gateFunction.evaluate2(predictedMeasurement, candidateMeasurement);
            if (gateDistance2 <= 1 && gateDistance2 < targetCandidate.getGateDistanceSq()) {
               targetCandidate.setGateDistanceSq(gateDistance2);
               targetCandidate.setTrack(track);
            }
         }
      }
   }

   private static NavigableMap<Float, Integer> makeRangeToIndexMap(List<TargetCandidate> targetCandidates) {
      NavigableMap<Float, Integer> map = new TreeMap<>();
      for (int i = targetCandidates.size() - 1; i >= 0; i--) {
         TargetCandidate targetCandidate = targetCandidates.get(i);
         map.put(targetCandidate.getMeasurement().range(), i);
      }
      map.put(Float.NEGATIVE_INFINITY, 0);
      return map;
   }
}
