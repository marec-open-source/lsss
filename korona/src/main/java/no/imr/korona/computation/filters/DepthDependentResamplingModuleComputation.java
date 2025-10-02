package no.imr.korona.computation.filters;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.range.FloatRange;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class DepthDependentResamplingModuleComputation extends ConcurrentPingModuleComputation {
   private final DepthDependentResamplingModule module;
   private final Set<Integer> channels;

   DepthDependentResamplingModuleComputation(DepthDependentResamplingModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);

      this.module = module;
      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      List<RawFileTransducer> transducers = pingConfiguration.getRawFileConfiguration().getTransducers();
      FloatRange kHzRange = FloatRange.of(
            module.minFrequency.getFloatValue(),
            module.maxFrequency.getValue().orElse(Float.POSITIVE_INFINITY)
      );
      channels = IntStream.rangeClosed(1, transducers.size())
            .filter(channel -> kHzRange.containsIncludingEnd(transducers.get(channel - 1).getKHz()))
            .boxed()
            .collect(Collectors.toSet());
   }

   @Override
   protected void processPing(Ping ping) {
      for (int channel : channels) {
         PowerData powerData = ping.getPowerData(channel);
         if (powerData != null) {
            float bottomDepth = (float) ping.getBot0Datagram().getChannelDepths()[channel - 1];
            resample(powerData, bottomDepth);
         }
      }
   }

   private void resample(PowerData powerData, float bottomDepth) {
      // Let d = original sample distance
      // Let e(r) = c * (r / 100)^k = new sample distance
      // where c = configured sample distance distance at 100m
      // n(r) = e(r) / d = c * (r / 100)^k / d = number of samples averaged at range r
      // => r(n) = 100 * (n * d / c)^(1/k) = (n * f)^(1/k)
      // where f = 100^k * d / c

      float[] sv = powerData.getSv();
      int bottomIndex = bottomDepth > 0 ? powerData.depthToSampleIndex(bottomDepth) : Integer.MAX_VALUE;
      int n = 1;
      double k = 2;
      float factor = (float) Math.pow(100, k) * powerData.getSampleDistance() / module.sampleDistanceAtRange100.getFloatValue();
      int i = powerData.rangeToClampedSampleIndex((float) Math.pow((n + 0.5f) * factor, 1 / k));
      while (i < sv.length) {
         n++;
         int iNextN = powerData.rangeToClampedSampleIndex((float) Math.pow((n + 0.5f) * factor, 1 / k));
         while (i < iNextN) {
            int end = Math.min(i + n, sv.length);
            if (i < bottomIndex && end > bottomIndex) {
               float meanSv = ArrayMath.mean(sv, i, bottomIndex);
               Arrays.fill(sv, i, bottomIndex, meanSv);
               i = bottomIndex;
            }
            float meanSv = ArrayMath.mean(sv, i, end);
            Arrays.fill(sv, i, end, meanSv);
            i = end;
         }
      }
      powerData.setSv(sv);
   }

   /* ORIG
   private void resample(PowerData powerData, float bottomDepth) {
      float[] sv = powerData.getSv();
      int bottomIndex = bottomDepth > 0 ? powerData.depthToSampleIndex(bottomDepth) : Integer.MAX_VALUE;
      int n = 1;
      float factor = 100 * powerData.getSampleDistance() / module.sampleDistanceAtRange100.getFloatValue();
      int i = powerData.rangeToClampedSampleIndex((n + 0.5f) * factor);
      while (i < sv.length) {
         n++;
         int iNextN = powerData.rangeToClampedSampleIndex((n + 0.5f) * factor);
         while (i < iNextN) {
            int end = Math.min(i + n, sv.length);
            if (i < bottomIndex && end > bottomIndex) {
               float meanSv = ArrayMath.mean(sv, i, bottomIndex);
               Arrays.fill(sv, i, bottomIndex, meanSv);
               i = bottomIndex;
            }
            float meanSv = ArrayMath.mean(sv, i, end);
            Arrays.fill(sv, i, end, meanSv);
            i = end;
         }
      }
      powerData.setSv(sv);
   }
    */
}
