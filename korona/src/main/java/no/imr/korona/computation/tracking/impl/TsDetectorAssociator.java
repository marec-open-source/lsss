package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.Associator;
import no.imr.korona.computation.tracking.GateFunction;
import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.TargetCandidate;
import no.imr.korona.computation.tracking.data.Track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class TsDetectorAssociator implements Associator {
   public TsDetectorAssociator() {
   }

   @Override
   public int getMaxMissingSamples() {
      // Not relevant. Only a single detected target is considered => No missing samples.
      return 0;
   }

   @Override
   public void associate(Collection<Track> tracks, List<TargetCandidate> targetCandidates, GateFunction gateFunction) {
      List<Track> sortedTracks = new ArrayList<>(tracks);
      sortedTracks.sort(Comparator.comparingDouble(track -> track.getLastPoint().getPrediction().measurement().range()));

      List<TargetCandidate> sortedTargetCandidates = new ArrayList<>(targetCandidates);
      sortedTargetCandidates.sort(Comparator.comparingDouble(candidate -> candidate.getMeasurement().range()));

      int iCandidate = 0;
      for (Track track : sortedTracks) {
         Measurement predictedMeasurement = track.getLastPoint().getPrediction().measurement();
         float predictedRange = predictedMeasurement.range();
         float minRange = predictedRange - gateFunction.getUnacceptableRangeDistance();
         while (iCandidate < sortedTargetCandidates.size() && sortedTargetCandidates.get(iCandidate).getMeasurement().range() < minRange) {
            iCandidate++;
         }
         float maxRange = predictedRange + gateFunction.getUnacceptableRangeDistance();
         float bestGateDistanceSq = Float.POSITIVE_INFINITY;
         int iBestCandidate = -1;
         for (int i = 0; i < sortedTargetCandidates.size(); i++) {
            TargetCandidate candidate = sortedTargetCandidates.get(i);
            Measurement candidateMeasurement = candidate.getMeasurement();
            if (candidateMeasurement.range() > maxRange) {
               break;
            }
            float gateDistanceSq = gateFunction.evaluate2(predictedMeasurement, candidateMeasurement);
            if (gateDistanceSq < bestGateDistanceSq) {
               bestGateDistanceSq = gateDistanceSq;
               iBestCandidate = i;
            }
         }
         if (bestGateDistanceSq <= 1) {
            TargetCandidate bestCandidate = sortedTargetCandidates.get(iBestCandidate);
            bestCandidate.setGateDistanceSq(bestGateDistanceSq);
            bestCandidate.setTrack(track);
            iCandidate = iBestCandidate + 1;
         }
      }
   }
}
