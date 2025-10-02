package no.imr.lsss.modules.thresholdresponse;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.range.FloatRange;

final class PingCache {
   private final float[][] channelIndexToVerticallyIntegratedSv;

   PingCache(RegionManager regionManager, Ping ping, Region region, int channelCount) {
      channelIndexToVerticallyIntegratedSv = new float[channelCount][Histogram.CELL_COUNT];

      for (ChannelData channelData : ping.getChannelDatas()) {
         if (channelData == null) {
            continue;
         }
         PowerData powerData = channelData.getPowerData();
         int channel = powerData.getChannel();
         float[] svArray = powerData.getSv();
         float[] logSvArray = powerData.getLogSv();
         float[] verticallyIntegratedSv = channelIndexToVerticallyIntegratedSv[channel - 1];

         for (FloatRange depthRange : regionManager.getDepthRangesForChannel(region, ping, channel)) {
            float iBeginAsFloat = powerData.depthToClampedSampleIndexAsFloat(depthRange.min());
            float iEndAsFloat = powerData.depthToClampedSampleIndexAsFloat(depthRange.max());

            int iBegin = (int) Math.ceil(iBeginAsFloat);
            int iEnd = (int) Math.floor(iEndAsFloat);

            //    iBeginAsFloat                                  iEndAsFloat
            //          |  iBegin                            iEnd    |
            //          |    |                                |      |
            // ---|----------|----------|----------|----------|----------|---> Samples

            if (iBegin > iEnd) {
               // => Begin and end are in the same sample.
               float logSv = logSvArray[iEnd];
               int i = Histogram.logSvToIndex(logSv);
               verticallyIntegratedSv[i] += svArray[iEnd] * (iEndAsFloat - iBeginAsFloat);
               continue;
            }
            if (iBegin > 0) {
               float logSv = logSvArray[iBegin - 1];
               int i = Histogram.logSvToIndex(logSv);
               verticallyIntegratedSv[i] += svArray[iBegin - 1] * (iBegin - iBeginAsFloat);
            }
            for (int iSv = iBegin; iSv < iEnd; iSv++) {
               float logSv = logSvArray[iSv];
               int i = Histogram.logSvToIndex(logSv);
               verticallyIntegratedSv[i] += svArray[iSv];
            }
            if (iEnd < svArray.length) {
               float logSv = logSvArray[iEnd];
               int i = Histogram.logSvToIndex(logSv);
               verticallyIntegratedSv[i] += svArray[iEnd] * (iEndAsFloat - iEnd);
            }
         }

         ArrayMath.multiply(verticallyIntegratedSv, powerData.getSampleDistance());
      }
   }

   float[] getVerticallyIntegratedSv(int channelIndex) {
      return channelIndexToVerticallyIntegratedSv[channelIndex];
   }
}
