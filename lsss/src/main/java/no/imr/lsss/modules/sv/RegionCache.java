package no.imr.lsss.modules.sv;

import no.imr.korona.data.ping.PingIndex;

import java.util.NavigableMap;
import java.util.TreeMap;

final class RegionCache {
   final NavigableMap<PingIndex, PingCache> pingMap = new TreeMap<>();
   final PerFrequencyData[] perFrequencyData;

   RegionCache(int channelCount) {
      perFrequencyData = PerFrequencyData.newArray(channelCount);
   }

   void update() {
      for (PerFrequencyData perFrequencyData : perFrequencyData) {
         perFrequencyData.clear();
      }

      for (PingCache pingCache : pingMap.values()) {
         for (int channelIndex = 0; channelIndex < perFrequencyData.length; channelIndex++) {
            perFrequencyData[channelIndex].accumulate(pingCache.perFrequencyData[channelIndex]);
         }
      }
   }
}
