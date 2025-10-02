package no.imr.lsss.modules.broadband;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import org.jspecify.annotations.Nullable;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;

/**
 * For holding data per region.
 *
 * @param <T> the ping cache type
 */
public final class BroadbandRegionCache<T> {
   // Must be concurrent since e.g. BroadbandTsModuleOverlay iterates over the map returned by BroadbandTsModule
   private final NavigableMap<PingIndex, T> pingMap = new ConcurrentSkipListMap<>();

   public BroadbandRegionCache() {
   }

   public void clearPingRange(PingRange pingRange) {
      pingMap.subMap(pingRange.begin(), pingRange.end()).clear();
   }

   public void retainPingRange(PingRange pingRange) {
      pingMap.keySet().removeIf(Predicate.not(pingRange::contains));
   }

   public @Nullable T getPing(PingIndex pingIndex) {
      return pingMap.get(pingIndex);
   }

   public void putPing(PingIndex pingIndex, T pingCache) {
      pingMap.put(pingIndex, pingCache);
   }

   public NavigableMap<PingIndex, T> getPingMap() {
      return pingMap;
   }
}
