package no.imr.lsss.modules.ts;

import no.imr.korona.data.datagrams.subdatagrams.ts.TsDatagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.util.ts.TSDetector;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.IntRangeSet;

import java.util.List;

@FunctionalInterface
interface TsDataComputer {
   List<TSData> compute(Ping ping, int channel, List<FloatRange> depthRanges);

   static TsDataComputer detector(TSDetector tsDetector) {
      return (ping, channel, depthRanges) -> PingCache.computeTSData(ping, channel, depthRanges, tsDetector);
   }

   static TsDataComputer korona() {
      return TsDataComputer::computeKoronaTsData;
   }

   private static List<TSData> computeKoronaTsData(Ping ping, int channel, List<FloatRange> depthRanges) {
      ChannelData channelData = ping.getChannelData(channel);
      if (channelData == null) {
         return List.of();
      }
      IntRangeSet indexes = new IntRangeSet();
      for (FloatRange depthRange : depthRanges) {
         int iBegin = channelData.depthToSampleIndex(depthRange.min());
         int iEnd = channelData.depthToSampleIndex(depthRange.max());
         indexes.add(iBegin, iEnd);
      }
      return ping.getPingItems(TsDatagram.class)
            .filter(tsDatagram -> tsDatagram.getChannel() == channel)
            .flatMap(tsDatagram -> tsDatagram.getDetections().stream())
            .filter(detection -> {
               // Filter on rounded sample index, like TSDetector does.
               int i = channelData.depthToSampleIndex(detection.peakDepth);
               return indexes.contains(i);
            })
            .map(detection -> {
               return new TSData(
                     detection.sv,
                     detection.alongshipAngle,
                     detection.athwartshipAngle,
                     detection.tsc,
                     detection.tsu,
                     detection.peakDepth,
                     channelData.depthToRange(detection.peakDepth),
                     ping.getInstant(),
                     ping.getPingIndex().getGeographicalPosition()
               );
            })
            .toList();
   }
}
