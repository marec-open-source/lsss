package no.imr.korona.util.ts;

import no.imr.korona.data.datagrams.subdatagrams.ts.TsDatagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;

import java.util.List;

public final class DatagramTSDetector implements TSDetector {
   public DatagramTSDetector() {
   }

   @Override
   public List<TSDetection> getAcceptedTsDetections(Ping ping, ChannelData channelData, int beginIndex, int endIndex) {
      return ping.getPingItems(TsDatagram.class)
            .filter(tsDatagram -> tsDatagram.getChannel() == channelData.getChannel())
            .flatMap(tsDatagram -> tsDatagram.getDetections().stream())
            .<TSDetection>mapMulti((detection, consumer) -> {
               int peakIndex = channelData.depthToSampleIndex(detection.peakDepth);
               if (peakIndex < beginIndex || peakIndex >= endIndex) {
                  return;
               }
               int tsBeginIndex = channelData.depthToSampleIndex(detection.depthRange.min());
               int tsEndIndex = channelData.depthToSampleIndex(detection.depthRange.max());
               consumer.accept(new TSDetection(tsBeginIndex, peakIndex, tsEndIndex));
            })
            .toList();
   }
}
