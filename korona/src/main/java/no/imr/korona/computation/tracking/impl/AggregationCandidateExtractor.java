package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.TargetCandidateExtractor;
import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.TargetCandidate;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.range.FloatRange;

import java.util.ArrayList;
import java.util.List;

public final class AggregationCandidateExtractor implements TargetCandidateExtractor {
   private final int channel;
   private final FloatRange tscRange;
   private final float maxDirectivityCorrection;

   public AggregationCandidateExtractor(int channel, FloatRange tscRange, float maxDirectivityCorrection) {
      this.channel = channel;
      this.tscRange = tscRange;
      this.maxDirectivityCorrection = maxDirectivityCorrection;
   }

   @Override
   public List<TargetCandidate> extract(Ping ping, FloatRange depthRange) {
      PowerData powerData = ping.getPowerData(channel);
      if (powerData == null) {
         return List.of();
      }

      List<TargetCandidate> targetCandidates = new ArrayList<>();

      int begin = powerData.depthToClampedSampleIndex(depthRange.min());
      int end = powerData.depthToClampedSampleIndex(depthRange.max());
      for (int i = begin; i < end; i++) {
         float directivityCorrection = powerData.getDirectivityCorrection(i);
         if (directivityCorrection > maxDirectivityCorrection) {
            continue;
         }
         float tsc = powerData.getTSU(i) + directivityCorrection;
         if (!tscRange.contains(tsc)) {
            continue;
         }
         Measurement measurement = new Measurement(powerData, i, tsc);
         float minRange = powerData.getSampleRange(i);
         float maxRange = powerData.getSampleRange(i + 1);
         targetCandidates.add(new TargetCandidate(i, 1, FloatRange.of(minRange, maxRange), measurement));
      }

      return targetCandidates;
   }
}
