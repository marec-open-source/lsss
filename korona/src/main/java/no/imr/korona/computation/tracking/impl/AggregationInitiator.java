package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.GateFunction;
import no.imr.korona.computation.tracking.Initiator;
import no.imr.korona.computation.tracking.PositionFunction;
import no.imr.korona.computation.tracking.TargetTracker;
import no.imr.korona.computation.tracking.TrackIdGenerator;
import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.TargetCandidate;
import no.imr.korona.computation.tracking.data.Track;
import no.imr.korona.data.ping.Ping;

import java.util.ArrayList;
import java.util.List;

public final class AggregationInitiator implements Initiator {
   private final GateFunction initiationGateFunction;
   private final int minLength;

   public AggregationInitiator(GateFunction initiationGateFunction, int minLength) {
      this.initiationGateFunction = initiationGateFunction;
      this.minLength = minLength;
   }

   @Override
   public List<Track> initiateNewTracks(Ping ping, List<TargetCandidate> targetCandidates, PositionFunction positionFunction, TrackIdGenerator trackIdGenerator) {
      List<Track> tracks = new ArrayList<>();
      List<TargetCandidate> connected = new ArrayList<>();
      for (TargetCandidate targetCandidate : targetCandidates) {
         if (targetCandidate.getTrack() != null) {
            continue;
         }
         if (connected.isEmpty()) {
            connected.add(targetCandidate);
            continue;
         }
         TargetCandidate previous = connected.getLast();
         float gateDistance2 = initiationGateFunction.evaluate2(previous.getMeasurement(), targetCandidate.getMeasurement());
         if (gateDistance2 >= 1) {
            newTrack(tracks, ping, positionFunction, connected, trackIdGenerator);
            connected.clear();
         }
         connected.add(targetCandidate);
      }

      newTrack(tracks, ping, positionFunction, connected, trackIdGenerator);

      return tracks;
   }

   private void newTrack(List<Track> tracks, Ping ping, PositionFunction positionFunction, List<TargetCandidate> targetCandidates, TrackIdGenerator trackIdGenerator) {
      if (targetCandidates.size() < minLength) {
         return;
      }

      Measurement measurement = getMeanMeasurement(targetCandidates);
      tracks.add(new Track(trackIdGenerator, ping, positionFunction, measurement,
            TargetTracker.getRangeRange(targetCandidates), TargetTracker.getSampleCount(targetCandidates)));
   }

   private static Measurement getMeanMeasurement(List<TargetCandidate> targetCandidates) {
      double range = 0;
      double along = 0;
      double athwart = 0;
      double tsc = 0;
      for (TargetCandidate targetCandidate : targetCandidates) {
         Measurement m = targetCandidate.getMeasurement();
         range += m.range();
         along += m.alongshipAngleRad();
         athwart += m.athwartshipAngleRad();
         tsc += m.tsc();
      }
      int n = targetCandidates.size();
      return new Measurement(
            (float) range / n,
            (float) along / n,
            (float) athwart / n,
            (float) tsc / n
      );
   }
}
