package no.imr.lsss.modules.plankton;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * The combined histograms for all pixels in the visible ping range in a region.
 */
final class RegionCache {
   private final NavigableMap<PingIndex, PingCache> pingMap = new TreeMap<>();
   private final HistogramMap histogramMap = new HistogramMap();

   RegionCache() {
   }

   NavigableMap<PingIndex, PingCache> getPingMap() {
      return pingMap;
   }

   HistogramMap getHistogramMap() {
      return histogramMap;
   }

   void update(PingRange pingRange, PingMapping pingMapping) {
      histogramMap.clear();

      PingIndex from = pingRange.begin();
      Map.Entry<PingIndex, PingCache> previousEntry = null;
      for (Map.Entry<PingIndex, PingCache> entry : pingMap.entrySet()) {
         if (previousEntry != null) {
            PingIndex to = entry.getKey();
            float weightFactor = (float) pingMapping.distance(from, to);
            histogramMap.accumulate(previousEntry.getValue().getHistogramMap(), weightFactor);
            from = to;
         }
         previousEntry = entry;
      }

      if (previousEntry != null) {
         PingIndex to = pingRange.end();
         float weightFactor = (float) pingMapping.distance(from, to);
         histogramMap.accumulate(previousEntry.getValue().getHistogramMap(), weightFactor);
      }
   }
}
