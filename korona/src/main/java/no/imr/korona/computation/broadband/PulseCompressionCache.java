package no.imr.korona.computation.broadband;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

public final class PulseCompressionCache {
   private static final LoadingCache<PulseCompressionConfig, PulseCompression> PULSE_COMPRESSION_CACHE = CacheBuilder.newBuilder()
         .maximumSize(100)
         .expireAfterAccess(5, TimeUnit.MINUTES)
         .build(new CacheLoader<>() {
            @Override
            public PulseCompression load(PulseCompressionConfig config) {
               return new PulseCompression(config);
            }
         });

   private PulseCompressionCache() {
   }

   public static PulseCompression getPulseCompression(PulseCompressionConfig config) {
      return PULSE_COMPRESSION_CACHE.getUnchecked(config);
   }
}
