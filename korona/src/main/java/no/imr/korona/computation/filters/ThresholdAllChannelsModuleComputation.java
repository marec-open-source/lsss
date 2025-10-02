package no.imr.korona.computation.filters;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.misc.FloatPredicate;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.RangeSet;

import java.util.Arrays;

final class ThresholdAllChannelsModuleComputation extends ConcurrentPingModuleComputation {
   private final ThresholdAllChannelsModule module;

   ThresholdAllChannelsModuleComputation(ThresholdAllChannelsModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);

      this.module = module;
   }

   @Override
   protected void processPing(Ping ping) {
      FloatPredicate logSvPredicate = makeLogSvPredicate(module.criterion.getValue(), module.threshold.getFloatValue());

      float minKHz = module.minFrequency.getValue().orElse(Float.NEGATIVE_INFINITY);
      float maxKHz = module.maxFrequency.getValue().orElse(Float.POSITIVE_INFINITY);
      FloatPredicate kHzPredicate = kHz -> (kHz >= minKHz) && (kHz <= maxKHz);

      RangeSet<Float> depthRanges = new ArrayRangeSet<>();
      ping.getNonNullPowerDatas()
            .filter(powerData -> kHzPredicate.test(powerData.getTransducer().getKHz()))
            .forEach(powerData -> {
               addDepthRanges(depthRanges, powerData, logSvPredicate);
            });
      ping.getNonNullPowerDatas().forEach(powerData -> {
         maskDepthRanges(depthRanges, powerData, module.replacement.getFloatValue());
      });
   }

   private static FloatPredicate makeLogSvPredicate(ThresholdAllChannelsModule.Criterion criterion, float threshold) {
      return switch (criterion) {
         case ABOVE_OR_EQUAL -> logSv -> logSv >= threshold;
         case ABOVE          -> logSv -> logSv > threshold;
         case BELOW          -> logSv -> logSv < threshold;
         case BELOW_OR_EQUAL -> logSv -> logSv <= threshold;
      };
   }

   private static void addDepthRanges(RangeSet<Float> depthRanges, PowerData powerData, FloatPredicate logSvPredicate) {
      float[] logSv = powerData.getLogSv();
      int iBegin = 0;
      while (true) { // Search for all intervals
         while (true) { // Search for start of interval
            if (iBegin >= logSv.length) {
               // Did not find start of interval
               return;
            }
            if (logSvPredicate.test(logSv[iBegin])) {
               // Found start of interval
               break;
            }
            iBegin++;
         }
         int iEnd = iBegin + 1;
         while (iEnd < logSv.length && logSvPredicate.test(logSv[iEnd])) { // Search for end of interval
            iEnd++;
         }
         depthRanges.add(powerData.getSampleDepth(iBegin), powerData.getSampleDepth(iEnd));
         iBegin = iEnd + 1;
      }
   }

   private static void maskDepthRanges(RangeSet<Float> depthRanges, PowerData powerData, float logSvValue) {
      depthRanges.forEach(depthRange -> {
         int iBegin = powerData.depthToClampedSampleIndex(depthRange.begin());
         int iEnd = powerData.depthToClampedSampleIndex(depthRange.end());
         Arrays.fill(powerData.getLogSv(), iBegin, iEnd, logSvValue);
      });
   }
}
