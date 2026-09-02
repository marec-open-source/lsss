package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.TargetCandidateExtractor;
import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.TargetCandidate;
import no.imr.korona.data.ping.Ping;
import no.imr.tools.range.FloatRange;

import java.util.ArrayList;
import java.util.List;

public final class FilteringTargetCandidateExtractor implements TargetCandidateExtractor {
   private final TargetCandidateExtractor targetCandidateExtractor;
   private final FloatRange tsRange;
   private final float maxDepth;
   private final float maxAlongshipAngleRad;
   private final float maxAthwartshipAngleRad;

   public FilteringTargetCandidateExtractor(TargetCandidateExtractor targetCandidateExtractor,
                                            FloatRange tsRange, float maxDepth, float maxAlongshipAngle, float maxAthwartshipAngle) {
      this.targetCandidateExtractor = targetCandidateExtractor;
      this.tsRange = tsRange;
      this.maxDepth = maxDepth;
      maxAlongshipAngleRad = (float) Math.toRadians(maxAlongshipAngle);
      maxAthwartshipAngleRad = (float) Math.toRadians(maxAthwartshipAngle);
   }

   @Override
   public List<TargetCandidate> extract(Ping ping, FloatRange depthRange) {
      List<TargetCandidate> candidates = targetCandidateExtractor.extract(ping, depthRange);
      List<TargetCandidate> filteredCandidates = new ArrayList<>(candidates.size());
      for (TargetCandidate candidate : candidates) {
         Measurement measurement = candidate.getMeasurement();
         if (measurement.range() > maxDepth) {
            continue;
         }
         if (!tsRange.contains(measurement.tsc())) {
            continue;
         }
         if (Math.abs(measurement.alongshipAngleRad()) > maxAlongshipAngleRad) {
            continue;
         }
         if (Math.abs(measurement.athwartshipAngleRad()) > maxAthwartshipAngleRad) {
            continue;
         }
         filteredCandidates.add(candidate);
      }
      return filteredCandidates;
   }
}
