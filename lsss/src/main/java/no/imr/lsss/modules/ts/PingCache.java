package no.imr.lsss.modules.ts;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.util.ts.TSDetection;
import no.imr.korona.util.ts.TSDetector;
import no.imr.tools.range.FloatRange;

import java.util.List;
import java.util.stream.IntStream;

final class PingCache implements BaseTsPingCache {
   private final List<List<TSData>> channelIndexToTsData;

   PingCache(RegionManager regionManager, Ping ping, Region region, int channelCount, TsDataComputer tsDataComputer) {
      channelIndexToTsData = IntStream.rangeClosed(1, channelCount)
            .mapToObj(channel -> {
               List<FloatRange> depthRanges = regionManager.getDepthRangesForChannel(region, ping, channel).getFloatRanges();
               return tsDataComputer.compute(ping, channel, depthRanges);
            })
            .toList();
   }

   @Override
   public List<TSData> getTsData(int channel) {
      return channelIndexToTsData.get(channel - 1);
   }

   static List<TSData> computeTSData(Ping ping, int channel, List<FloatRange> depthRanges, TSDetector tsDetector) {
      ChannelData channelData = ping.getChannelData(channel);
      if (channelData == null) {
         return List.of();
      }
      return depthRanges.stream()
            .flatMap(depthRange -> tsDetector.getAcceptedTsDetections(ping, channelData, depthRange).stream())
            .map(tsDetection -> createTSData(tsDetection, ping, channelData.getPowerData()))
            .toList();
   }

   private static TSData createTSData(TSDetection tsDetection, Ping ping, PowerData powerData) {
      return new TSData(
            powerData.getSv()[tsDetection.peakIndex()],
            powerData.getMechanicalAlongAngle(tsDetection.peakIndex()),
            powerData.getMechanicalAthwartAngle(tsDetection.peakIndex()),
            powerData.getTSC(tsDetection.peakIndex()),
            powerData.getTSU(tsDetection.peakIndex()),
            powerData.getSampleDepth(tsDetection.peakIndex()),
            powerData.getSampleRange(tsDetection.peakIndex()),
            powerData.getNTDate(),
            ping.getPingIndex().getGeographicalPosition()
      );
   }
}
