package no.imr.korona.computation.broadband.transferfunction;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFilterConfig;
import no.imr.tools.range.FloatRange;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class NotchFilterCache {
   private static final LoadingCache<NotchFilterConfig, TransferFunction> CACHE = CacheBuilder.newBuilder()
         .maximumSize(100)
         .expireAfterAccess(5, TimeUnit.MINUTES)
         .build(CacheLoader.from(TransferFunctionUtils::generateNotchFilterFromConfig));

   private NotchFilterCache() {
   }

   public static TransferFunction getFilteredTransmitSignal(List<BroadbandNotchFilterConfig> broadbandNotchFilterConfigs, FloatRange frequencyRange) {
      return getFilteredTransmitSignal(new NotchFilterConfig(broadbandNotchFilterConfigs, frequencyRange));
   }

   public static TransferFunction getFilteredTransmitSignal(NotchFilterConfig config) {
      return CACHE.getUnchecked(config);
   }
}
