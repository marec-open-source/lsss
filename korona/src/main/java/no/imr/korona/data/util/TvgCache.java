package no.imr.korona.data.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.imr.korona.data.ping.items.channel.ChannelData;

import java.time.Duration;

/**
 * Cached TVG.
 */
public final class TvgCache {
   private static final LoadingCache<TVG.Parameters, TVG> CACHE = CacheBuilder.newBuilder()
         .maximumSize(100)
         .expireAfterAccess(Duration.ofMinutes(10))
         .build(CacheLoader.from(TVG::new));

   private TvgCache() {
   }

   public static TvgArray getTvgArray(ChannelData channelData) {
      return CACHE.getUnchecked(new TVG.Parameters(channelData))
            .getTvgArray(channelData.getOffset(), channelData.getCount());
   }
}
