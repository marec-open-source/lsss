package no.imr.lsss.modules.broadband.sv;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.tools.range.FloatRange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BroadbandSvPingCache {
   private final Map<Integer, BroadbandSvChannelCache> channelCaches = new HashMap<>();

   BroadbandSvPingCache(RegionManager regionManager, Ping ping, Region region, BroadbandSvModule broadbandSvModule) {
      int channelCount = ping.getRawFileConfiguration().getTransducerCount();
      for (int channel = 1; channel <= channelCount; channel++) {
         List<FloatRange> depthRanges = regionManager.getDepthRangesForChannel(region, ping, channel).getFloatRanges();
         channelCaches.put(channel, BroadbandSvChannelCache.from(ping, channel, depthRanges, broadbandSvModule));
      }
   }

   BroadbandSvChannelCache getChannelCache(int channel) {
      return channelCaches.get(channel);
   }
}
