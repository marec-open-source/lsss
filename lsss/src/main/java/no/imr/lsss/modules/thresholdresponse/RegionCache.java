package no.imr.lsss.modules.thresholdresponse;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

final class RegionCache {
   private final NavigableMap<PingIndex, PingCache> pingMap = new TreeMap<>();
   private final float[][] channelIndexToHorizontallyIntegratedSv;

   RegionCache(int channelCount) {
      channelIndexToHorizontallyIntegratedSv = new float[channelCount][Histogram.CELL_COUNT];
   }

   NavigableMap<PingIndex, PingCache> getPingMap() {
      return pingMap;
   }

   float[] getHorizontallyIntegratedSv(int channelIndex) {
      return channelIndexToHorizontallyIntegratedSv[channelIndex];
   }

   void update(PingRange visiblePingRange, PingMapping pingMapping) {
      Utils.fill(channelIndexToHorizontallyIntegratedSv, 0);

      PingIndex from = visiblePingRange.begin();
      List<Map.Entry<PingIndex, PingCache>> entries = new ArrayList<>(pingMap.entrySet());
      for (int iEntry = 0, n = entries.size(); iEntry < n; iEntry++) {
         Map.Entry<PingIndex, PingCache> entry = entries.get(iEntry);
         PingCache pingCache = entry.getValue();

         PingIndex to = (iEntry + 1 == n) ? visiblePingRange.end() : entries.get(iEntry + 1).getKey();
         float distance = (float) pingMapping.distance(from, to);
         from = to;

         for (int channelIndex = 0; channelIndex < channelIndexToHorizontallyIntegratedSv.length; channelIndex++) {
            float[] horizontallyIntegratedSv = channelIndexToHorizontallyIntegratedSv[channelIndex];
            float[] verticallyIntegratedSv = pingCache.getVerticallyIntegratedSv(channelIndex);

            for (int i = 0; i < Histogram.CELL_COUNT; i++) {
               horizontallyIntegratedSv[i] += distance * verticallyIntegratedSv[i];
            }
         }
      }
   }
}
