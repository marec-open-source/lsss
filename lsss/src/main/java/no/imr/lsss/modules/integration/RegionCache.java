package no.imr.lsss.modules.integration;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

final class RegionCache {
   private final NavigableMap<PingIndex, @Nullable PingCache> pingMap = new TreeMap<>();
   private volatile List<IntegrationCurvePoint> curve = List.of();

   RegionCache() {
   }

   PingCache getOrCreatePingCache(PingIndex pingIndex) {
      return pingMap.computeIfAbsent(pingIndex, _ -> new PingCache());
   }

   void putNullIfAbsent(PingIndex pingIndex) {
      if (!pingMap.containsKey(pingIndex)) {
         pingMap.put(pingIndex, null);
      }
   }

   NavigableMap<PingIndex, @Nullable PingCache> getPingMap() {
      return pingMap;
   }

   List<IntegrationCurvePoint> getCurve() {
      return curve;
   }

   void updateCurve(PingRange pingRange, PingMapping pingMapping) {
      if (pingRange.isEmpty()) {
         curve = List.of();
         return;
      }

      double accumulatedDistance = 0;
      double horizontallyIntegratedSvTotal = 0;
      double horizontallyIntegratedSvPelagic = 0;
      double horizontallyIntegratedSvBottom = 0;

      int n = pingMap.size();
      List<IntegrationCurvePoint> curve = new ArrayList<>(n + 1);
      List<Map.Entry<PingIndex, @Nullable PingCache>> entries = new ArrayList<>(pingMap.entrySet());

      for (int i = 0; i < n; i++) {
         Map.Entry<PingIndex, @Nullable PingCache> entry = entries.get(i);
         PingCache pingCache = entry.getValue();
         PingIndex from = (i == 0) ? pingRange.begin() : entry.getKey();
         PingIndex to = (i + 1 == n) ? pingRange.end() : entries.get(i + 1).getKey();
         curve.add(new IntegrationCurvePoint(from,
               pingCache,
               (float) accumulatedDistance,
               (float) horizontallyIntegratedSvTotal,
               (float) horizontallyIntegratedSvPelagic,
               (float) horizontallyIntegratedSvBottom));
         if (pingCache != null) {
            double distance = pingMapping.distance(from, to);
            accumulatedDistance += distance;
            horizontallyIntegratedSvTotal += pingCache.getVerticallyIntegratedSvTotal() * distance;
            horizontallyIntegratedSvPelagic += pingCache.getVerticallyIntegratedSvPelagic() * distance;
            horizontallyIntegratedSvBottom += pingCache.getVerticallyIntegratedSvBottom() * distance;
         }
      }

      curve.add(new IntegrationCurvePoint(pingRange.end(),
            null,
            (float) accumulatedDistance,
            (float) horizontallyIntegratedSvTotal,
            (float) horizontallyIntegratedSvPelagic,
            (float) horizontallyIntegratedSvBottom));

      this.curve = List.copyOf(curve);
   }
}
