package no.imr.lsss.modules.broadband.sv;

import no.imr.korona.computation.broadband.BroadbandSvByFrequency;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.range.FloatRange;

import java.util.ArrayList;
import java.util.List;

record BroadbandSvChannelCache(
      List<BroadbandSvData> svData,
      FloatRange frequencyRange,
      List<FloatRange> tooSmallDepthRanges,
      float narrowbandSv
) {
   static BroadbandSvChannelCache from(Ping ping, int channel, List<FloatRange> depthRanges, BroadbandSvModule broadbandSvModule) {
      BroadbandData broadbandData = ping.getBroadbandData(channel);
      if (broadbandData == null) {
         PowerData powerData = ping.getPowerData(channel);
         if (powerData != null) {
            float frequency = powerData.getTransducer().getFrequency();
            float narrowbandSv = (float) depthRanges.stream()
                  .mapToDouble(depthRange -> {
                     return broadbandSvModule.useTVG.getBooleanValue()
                           ? powerData.getVerticalIntegralSv(depthRange, FloatRange.ALL) / depthRange.getSize()
                           : powerData.getVerticalIntegralNoise(depthRange, FloatRange.ALL) / depthRange.getSize();
                  })
                  .sum();
            return new BroadbandSvChannelCache(List.of(), FloatRange.of(frequency, frequency), List.of(), narrowbandSv);
         } else {
            return new BroadbandSvChannelCache(List.of(), FloatRange.EMPTY_RANGE, List.of(), Float.NaN);
         }
      }

      BroadbandSvByFrequency svByFrequency = new BroadbandSvByFrequency(broadbandData);
      svByFrequency.setUseTVG(broadbandSvModule.useTVG.getBooleanValue());
      float deltaFrequency = broadbandSvModule.frequencyResolution.getFloatValue() * 1000;
      FloatRange frequencyRange = broadbandData.getFrequencyRange()
            .shrinkByFraction(broadbandSvModule.frequencyWindowing.getFloatValue() / 100)
            .intersection(svByFrequency.getMaxFrequencyRange())
            .shrinkToMultipleOf(deltaFrequency);

      List<FloatRange> tooSmallDepthRanges = new ArrayList<>();
      List<BroadbandSvData> svData = depthRanges.stream()
            .flatMap(depthRange -> {
               List<FloatRange> windowDepthRanges = svByFrequency.windowDepthRanges(depthRange,
                     broadbandSvModule.depthResolution.getFloatValue(),
                     broadbandSvModule.depthMargin.getFloatValue(),
                     broadbandSvModule.fftWindowSize.getFloatValue());
               if (windowDepthRanges.isEmpty()) {
                  tooSmallDepthRanges.add(depthRange);
               }
               return windowDepthRanges.stream();
            })
            .map(depthRange -> {
               float[] values = svByFrequency.calculate(depthRange, frequencyRange);
               values = ArrayMath.resample(values, Math.round(frequencyRange.getSize() / deltaFrequency) + 1);
               return new BroadbandSvData(depthRange, values);
            })
            .toList();
      return new BroadbandSvChannelCache(svData, frequencyRange, List.copyOf(tooSmallDepthRanges), Float.NaN);
   }
}
