package no.imr.lsss.modules.ts;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.Utils;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

final class RegionCache {
   final NavigableMap<PingIndex, PingCache> pingMap = new ConcurrentSkipListMap<>();
   final int[][] tsHistogram;

   RegionCache(int channelCount, int histogramBins) {
      tsHistogram = new int[channelCount][histogramBins];
   }

   void update(float minTS, float deltaTS) {
      Utils.fill(tsHistogram, 0);
      for (PingCache pingCache : pingMap.values()) {
         for (int channelIndex = 0; channelIndex < tsHistogram.length; channelIndex++) {
            int[] channelTsHistogram = tsHistogram[channelIndex];
            for (TSData tsData : pingCache.getTsData(channelIndex + 1)) {
               int i = (int) Math.floor((tsData.tsc() - minTS) / deltaTS);
               if (i >= 0 && i < channelTsHistogram.length) {
                  channelTsHistogram[i]++;
               }
            }
         }
      }
   }

   void clearPingRange(PingRange pingRange) {
      pingMap.subMap(pingRange.begin(), pingRange.end()).clear();
   }
}
