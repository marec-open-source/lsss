package no.imr.lsss.modules.broadband.ts;

import no.imr.korona.computation.broadband.BroadbandTsByFrequency;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.util.ts.TSDetection;
import no.imr.korona.util.ts.TSDetector;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.range.FloatRange;

import java.util.List;
import java.util.stream.Stream;

final class BroadbandTsChannelCache {
   private final boolean broadband;
   private final List<BroadbandTsData> tsData;

   BroadbandTsChannelCache(Ping ping, int channel, List<FloatRange> depthRanges, TSDetector tsDetector, BroadbandTsModule broadbandTsModule) {
      BroadbandData broadbandData = ping.getBroadbandData(channel);
      if (broadbandData == null) {
         broadband = false;
         tsData = List.of();
         return;
      }
      broadband = true;

      Stream<Target> targets = depthRanges.stream()
            .flatMap(depthRange -> {
               List<TSDetection> tsDetections = tsDetector.getAcceptedTsDetections(ping, broadbandData, depthRange);
               return switch (broadbandTsModule.targetExtentMode.getValue()) {
                  case AUTOMATIC -> {
                     yield tsDetections.stream()
                           .map(tsCandidate -> new Target(broadbandData.getSampleDepth(tsCandidate.peakIndex()),
                                 FloatRange.of(broadbandData.getSampleDepth(tsCandidate.beginIndex()), broadbandData.getSampleDepth(tsCandidate.endIndex()))));
                  }
                  case MANUAL -> {
                     yield tsDetections.stream()
                           .map(targetCandidate -> {
                              float depth = broadbandData.getSampleDepth(targetCandidate.peakIndex());
                              return new Target(depth, broadbandTsModule.getManualTargetDepthRange(depth));
                           });
                  }
                  case REGION -> {
                     yield tsDetections.isEmpty()
                           ? Stream.empty()
                           : Stream.of(new Target(broadbandData.getSampleDepth(tsDetections.getFirst().peakIndex()), depthRange));
                  }
               };
            });

      tsData = computeTsData(broadbandData, broadbandTsModule, targets);
   }

   static List<BroadbandTsData> computeTsData(BroadbandData broadbandData, BroadbandTsModule broadbandTsModule, Stream<Target> targets) {
      BroadbandTsByFrequency tsByFrequency = new BroadbandTsByFrequency(broadbandData);
      FloatRange maxFrequencyRange = tsByFrequency.getMaxFrequencyRange();
      float deltaFrequency = broadbandTsModule.frequencyResolution.getFloatValue() * 1000;
      FloatRange frequencyRange = broadbandData.getFrequencyRange()
            .shrinkByFraction(broadbandTsModule.frequencyWindowing.getFloatValue() / 100)
            .intersection(maxFrequencyRange)
            .shrinkToMultipleOf(deltaFrequency);

      return targets
            .map(target -> {
               BroadbandTsByFrequency.Result result = tsByFrequency.calculate(target.depthRange, frequencyRange);
               float[] values = result.values();
               values = ArrayMath.fourierResample(values, Math.round(frequencyRange.getSize() / deltaFrequency) + 1);
               float depth = Float.isNaN(target.depth) ? broadbandData.getSampleDepth(result.sampleIndex()) : target.depth;
               return new BroadbandTsData(depth, target.depthRange, values, frequencyRange);
            })
            .toList();
   }

   boolean isBroadband() {
      return broadband;
   }

   List<BroadbandTsData> getTsData() {
      return tsData;
   }

   record Target(float depth, FloatRange depthRange) {
   }
}
