package no.imr.lsss.modules.frequencyresponse;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.util.SvSum;

import java.util.NavigableMap;
import java.util.TreeMap;

final class RegionCache {
   final NavigableMap<PingIndex, PingCache> pingMap = new TreeMap<>();
   final SvSum svSum;

   RegionCache(int channelCount) {
      svSum = new SvSum(channelCount);
   }

   void update() {
      svSum.reset();

      for (PingCache pingCache : pingMap.values()) {
         svSum.accumulate(pingCache.svSum);
      }
   }
}
