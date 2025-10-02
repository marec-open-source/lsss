package no.imr.korona.computation.broadband.splitting;

import no.imr.korona.computation.ConfigFileSettingsException;
import no.imr.korona.computation.broadband.BroadbandSvByFrequency;
import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFilterModuleConfig;
import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFiltersFileService;
import no.imr.korona.computation.broadband.notchfilter.BroadbandTemporalNotchFilterConfig;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.RangeSet;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

final class BroadbandSplitterGlidingWindow {
   private final BroadbandSplitterModule module;
   private final RangeSet<Float> usableFrequencies = new ArrayRangeSet<>();

   BroadbandSplitterGlidingWindow(BroadbandSplitterModule module, PingConfiguration pingConfiguration) throws ConfigFileSettingsException {
      this.module = module;

      usableFrequencies.add(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
      removeNotchFrequencies(pingConfiguration);
   }

   private void removeNotchFrequencies(PingConfiguration pingConfiguration) throws ConfigFileSettingsException {
      Path notchFilterFile = module.getOptionalConfigFile(BroadbandNotchFiltersFileService.NAME);
      if (notchFilterFile != null) {
         Element element;
         try {
            element = XmlUtils.readDocument(notchFilterFile).getRootElement();
         } catch (IOException e) {
            throw new ConfigFileSettingsException(module, BroadbandNotchFiltersFileService.NAME, notchFilterFile, e.getMessage());
         }
         BroadbandNotchFilterModuleConfig broadbandNotchFilterModuleConfig = new BroadbandNotchFilterModuleConfig(element);
         Instant instant = pingConfiguration.getRawFileConfiguration().getInstant();
         broadbandNotchFilterModuleConfig.getBroadbandTemporalNotchFilterConfigs().stream()
               .filter(config -> config.isValid(instant))
               .map(BroadbandTemporalNotchFilterConfig::getBroadbandNotchFilterConfig)
               .forEach(notchFilterConfig -> {
                  FloatRange skipFrequencyRange = FloatRange.ofCenterAndSize(notchFilterConfig.rejectionFrequency(), notchFilterConfig.bandwidth());
                  usableFrequencies.remove(skipFrequencyRange.min(), skipFrequencyRange.max());
               });
      }
   }

   void computeSv(BroadbandData broadbandData, List<PowerData> powerDatas, List<BroadbandSplitterModuleComputation.FrequencyBand> frequencyBands, int downsamplingFactor, AsyncHandle asyncHandle) {
      List<List<FloatRange>> usableFrequencyRanges = frequencyBands.stream()
            .map(frequencyBand -> {
               FloatRange frequencyRange = frequencyBand.frequencyRange();
               return usableFrequencies.stream(frequencyRange.toRange())
                     .map(FloatRange::of)
                     .toList();
            })
            .toList();

      float pulseLength = broadbandData.getSoundVelocity() * broadbandData.getPulseDuration();
      float fftWindowRadius = module.fftWindowSize.getFloatValue() * pulseLength / 2;
      float fftSampleCountRadius = fftWindowRadius / broadbandData.getSampleDistance();

      BroadbandSvByFrequency svByFrequency = new BroadbandSvByFrequency(broadbandData);
      FloatRange broadbandDataFrequencyRange = broadbandData.getFrequencyRange();
      int downsampledCount = broadbandData.getCount() / downsamplingFactor;
      for (int iDownSampled = 0; iDownSampled < downsampledCount; iDownSampled++) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         float iOrigCenter = (iDownSampled + 0.5f) * downsamplingFactor;
         int iOrigBegin = Math.max(Math.round(iOrigCenter - fftSampleCountRadius), 0);
         int iOrigEnd = Math.min(Math.round(iOrigCenter + fftSampleCountRadius), broadbandData.getCount());
         float[] sv = svByFrequency.calculate(iOrigBegin, iOrigEnd, broadbandDataFrequencyRange);
         for (int iPowerData = 0; iPowerData < powerDatas.size(); iPowerData++) {
            PowerData powerData = powerDatas.get(iPowerData);
            double sum = 0;
            int len = 0;
            for (FloatRange frequencyRange : usableFrequencyRanges.get(iPowerData)) {
               int a = Math.round(sv.length * broadbandDataFrequencyRange.valueToFraction(frequencyRange.min()));
               int b = Math.round(sv.length * broadbandDataFrequencyRange.valueToFraction(frequencyRange.max()));
               sum += ArrayMath.sum(sv, a, b);
               len += b - a;
            }
            powerData.getSv()[iDownSampled] = len > 0 ? (float) (sum / len) : 0;
         }
      }
   }
}
