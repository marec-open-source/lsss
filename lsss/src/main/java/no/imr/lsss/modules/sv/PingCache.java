package no.imr.lsss.modules.sv;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.TvgArray;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.tools.range.FloatRange;

final class PingCache {
   final PerFrequencyData[] perFrequencyData;

   PingCache(RegionManager regionManager, Ping ping, Region region, int channelCount, boolean useTVG) {
      perFrequencyData = new PerFrequencyData[channelCount];

      for (int channel = 1; channel <= channelCount; channel++) {
         PerFrequencyData perFrequencyData = new PerFrequencyData();
         this.perFrequencyData[channel - 1] = perFrequencyData;

         PowerData powerData = ping.getPowerData(channel);
         if (powerData == null) {
            continue;
         }
         float[] logSvArray = powerData.getLogSv();
         float[] svArray = powerData.getSv();
         TvgArray tvg = powerData.getTVGArray();

         int[] channelHistogram = perFrequencyData.histogram;
         for (FloatRange depthRange : regionManager.getDepthRangesForChannel(region, ping, channel)) {
            int iBegin = powerData.depthToClampedSampleIndex(depthRange.min());
            int iEnd = powerData.depthToClampedSampleIndex(depthRange.max());

            for (int iSv = iBegin; iSv < iEnd; iSv++) {
               float value = useTVG ? logSvArray[iSv] : PowerData.svToLogSv(svArray[iSv] / tvg.get(iSv));
               int i = Math.clamp((int) Math.floor((value - SvDistributionModule.MIN_LOG_SV) / SvDistributionModule.DELTA_LOG_SV), 0, SvDistributionModule.N_LOG_SV - 1);
               channelHistogram[i]++;
               perFrequencyData.svSum += svArray[iSv];
               perFrequencyData.count++;
            }
         }
      }
   }
}
