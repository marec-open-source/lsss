package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.Initiator;
import no.imr.korona.computation.tracking.PositionFunction;
import no.imr.korona.computation.tracking.TrackIdGenerator;
import no.imr.korona.computation.tracking.data.TargetCandidate;
import no.imr.korona.computation.tracking.data.Track;
import no.imr.korona.data.ping.Ping;

import java.util.ArrayList;
import java.util.List;

public final class TsDetectorInitiator implements Initiator {
   public TsDetectorInitiator() {
   }

   @Override
   public List<Track> initiateNewTracks(Ping ping, List<TargetCandidate> targetCandidates, PositionFunction positionFunction, TrackIdGenerator trackIdGenerator) {
      List<Track> tracks = new ArrayList<>();
      for (TargetCandidate targetCandidate : targetCandidates) {
         if (targetCandidate.getTrack() == null) {
            tracks.add(new Track(trackIdGenerator, ping, positionFunction, targetCandidate.getMeasurement(), targetCandidate.getRangeRange(), targetCandidate.getSampleCount()));
         }
      }
      return tracks;
   }
}
