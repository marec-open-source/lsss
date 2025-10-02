package no.imr.korona.util.ts;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.tools.range.FloatRange;

import java.util.List;

public interface TSDetector {
   List<TSDetection> getAcceptedTsDetections(Ping ping, ChannelData channelData, int beginIndex, int endIndex);

   default List<TSDetection> getAcceptedTsDetections(Ping ping, ChannelData channelData, FloatRange depthRange) {
      int beginIndex = channelData.depthToClampedSampleIndex(depthRange.min());
      int endIndex = channelData.depthToClampedSampleIndex(depthRange.max());
      return getAcceptedTsDetections(ping, channelData, beginIndex, endIndex);
   }
}
