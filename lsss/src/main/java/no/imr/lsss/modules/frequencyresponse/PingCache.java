package no.imr.lsss.modules.frequencyresponse;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.util.SvSum;
import no.imr.tools.math.WelfordsMethod;
import no.imr.tools.range.FloatRange;

final class PingCache {
   final SvSum svSum;

   PingCache(RegionManager regionManager, Region region, Ping ping, int channelCount, FloatRange svRange) {
      svSum = new SvSum(channelCount);

      for (ChannelData channelData : ping.getChannelDatas()) {
         if (channelData == null) {
            continue;
         }
         PowerData powerData = channelData.getPowerData();
         int channel = powerData.getChannel();
         float[] svArray = powerData.getSv();

         WelfordsMethod welfordsMethod = svSum.getWelfordsMethod()[channel - 1];

         for (FloatRange depthRange : regionManager.getDepthRangesForChannel(region, ping, channel)) {
            int iBegin = powerData.depthToClampedSampleIndex(depthRange.min());
            int iEnd = powerData.depthToClampedSampleIndex(depthRange.max());

            for (int iSv = iBegin; iSv < iEnd; iSv++) {
               float sv = svArray[iSv];
               if (svRange.contains(sv)) {
                  welfordsMethod.update(sv);
               } else {
                  welfordsMethod.update(0);
                  // Ref ticket #137: Use all pixels
               }
            }
         }
      }
   }
}
