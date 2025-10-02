package no.imr.lsss.modules.broadband.ts;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.util.ts.TSDetector;
import no.imr.lsss.modules.ts.BaseTsPingCache;
import no.imr.tools.range.FloatRange;

import java.util.List;
import java.util.stream.IntStream;

final class BroadbandTsPingCache implements BaseTsPingCache {
   private final List<BroadbandTsChannelCache> channelCaches;

   BroadbandTsPingCache(RegionManager regionManager, Ping ping, Region region, TSDetector tsDetector, BroadbandTsModule broadbandTsModule) {
      int channelCount = ping.getRawFileConfiguration().getTransducerCount();
      channelCaches = IntStream.rangeClosed(1, channelCount)
            .mapToObj(channel -> {
               List<FloatRange> depthRanges = regionManager.getDepthRangesForChannel(region, ping, channel).getFloatRanges();
               return new BroadbandTsChannelCache(ping, channel, depthRanges, tsDetector, broadbandTsModule);
            })
            .toList();
   }

   BroadbandTsChannelCache getChannelCache(int channel) {
      return channelCaches.get(channel - 1);
   }

   @Override
   public List<BroadbandTsData> getTsData(int channel) {
      return getChannelCache(channel).getTsData();
   }
}
