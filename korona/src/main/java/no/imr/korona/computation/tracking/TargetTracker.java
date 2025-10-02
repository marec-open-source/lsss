package no.imr.korona.computation.tracking;

import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.StateVector;
import no.imr.korona.computation.tracking.data.TargetCandidate;
import no.imr.korona.computation.tracking.data.TargetPoint;
import no.imr.korona.computation.tracking.data.Track;
import no.imr.korona.computation.tracking.data.TrackPoint;
import no.imr.korona.data.ping.Ping;
import no.imr.tools.range.FloatRange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TargetTracker {
   private final TrackingListener trackingListener;
   private final PositionFunction positionFunction;
   private final TargetCandidateExtractor targetCandidateExtractor;
   private final Predictor predictor;
   private final Associator associator;
   private final Compositor compositor;
   private final Estimator estimator;
   private final Terminator terminator;
   private final Validator validator;
   private final Initiator initiator;
   private final GateFunction gateFunction;
   private final TrackIdGenerator trackIdGenerator;
   private final Set<Track> tracks = new LinkedHashSet<>();

   TargetTracker(TrackingListener trackingListener, PositionFunction positionFunction, TargetCandidateExtractor targetCandidateExtractor,
                 Predictor predictor, Associator associator,
                 Initiator initiator, Estimator estimator, Terminator terminator, Validator validator, Compositor compositor,
                 GateFunction gateFunction, TrackIdGenerator trackIdGenerator) {
      this.trackingListener = trackingListener;
      this.positionFunction = positionFunction;
      this.targetCandidateExtractor = targetCandidateExtractor;
      this.predictor = predictor;
      this.associator = associator;
      this.initiator = initiator;
      this.terminator = terminator;
      this.validator = validator;
      this.estimator = estimator;
      this.compositor = compositor;
      this.gateFunction = gateFunction;
      this.trackIdGenerator = trackIdGenerator;
   }

   public void track(Ping ping, FloatRange depthRange) {
      positionFunction.update(ping);

      for (Track track : tracks) {
         StateVector prediction = predictor.predict(track, ping.getPingIndex());
         track.getPoints().add(new TrackPoint(ping.getPingIndex(), new TargetPoint(prediction, positionFunction)));
      }

      List<TargetCandidate> targetCandidates = targetCandidateExtractor.extract(ping, depthRange);
      associator.associate(tracks, targetCandidates, gateFunction);

      for (Map.Entry<Track, List<TargetCandidate>> entry : createAssociationMap(targetCandidates).entrySet()) {
         List<TargetCandidate> trackTargetCandidates = entry.getValue();
         Track track = entry.getKey();

         TargetCandidate last = trackTargetCandidates.getLast();
         int totalSampleCount = last.getSampleIndex() - trackTargetCandidates.getFirst().getSampleIndex() + last.getSampleCount();
         int sampleCount = getSampleCount(trackTargetCandidates);
         int missingSamples = totalSampleCount - sampleCount;
         if (missingSamples > associator.getMaxMissingSamples()) {
            tracks.remove(track);
            terminate(track);
            continue;
         }

         Measurement compositeMeasurement = compositor.compose(trackTargetCandidates);
         TrackPoint currentTrackPoint = track.getLastPoint();
         currentTrackPoint.setMeasurement(compositeMeasurement);
         currentTrackPoint.setRangeRange(getRangeRange(trackTargetCandidates));
         currentTrackPoint.setSampleCount(sampleCount);
         StateVector estimate = estimator.estimate(track, currentTrackPoint, positionFunction);
         currentTrackPoint.setEstimate(new TargetPoint(estimate, positionFunction));
         trackingListener.onNewTrackPoint(track);
      }

      Set<Track> terminatedTracks = new HashSet<>();
      for (Track track : tracks) {
         if (terminator.shouldTerminate(track)) {
            terminatedTracks.add(track);
            terminate(track);
         }
      }
      tracks.removeAll(terminatedTracks);

      List<Track> newTracks = initiator.initiateNewTracks(ping, targetCandidates, positionFunction, trackIdGenerator);
      tracks.addAll(newTracks);
      for (Track track : newTracks) {
         trackingListener.onNewTrackPoint(track);
      }
   }

   public void end() {
      for (Track track : tracks) {
         terminate(track);
      }
      tracks.clear();
   }

   private void terminate(Track track) {
      boolean valid = validator.isValid(track);
      trackingListener.onTrackTermination(track, valid);
   }

   private static Map<Track, List<TargetCandidate>> createAssociationMap(List<TargetCandidate> targetCandidates) {
      Map<Track, List<TargetCandidate>> map = new LinkedHashMap<>();
      for (TargetCandidate targetCandidate : targetCandidates) {
         Track track = targetCandidate.getTrack();
         if (track != null) {
            List<TargetCandidate> trackTargetCandidates = map.computeIfAbsent(track, k -> new ArrayList<>());
            trackTargetCandidates.add(targetCandidate);
         }
      }
      return map;
   }

   public static int getSampleCount(List<TargetCandidate> targetCandidates) {
      int sampleCount = 0;
      for (TargetCandidate targetCandidate : targetCandidates) {
         sampleCount += targetCandidate.getSampleCount();
      }
      return sampleCount;
   }

   public static FloatRange getRangeRange(List<TargetCandidate> targetCandidates) {
      float min = targetCandidates.getFirst().getRangeRange().min();
      float max = targetCandidates.getLast().getRangeRange().max();
      return FloatRange.of(min, max);
   }
}
