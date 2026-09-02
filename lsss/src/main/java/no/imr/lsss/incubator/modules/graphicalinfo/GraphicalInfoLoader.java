package no.imr.lsss.incubator.modules.graphicalinfo;

import no.imr.korona.data.datagrams.subdatagrams.graphical.GraphicalInfoSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.graphical.GraphicalInfoTocSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.graphical.OffsetPolygon;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.PingRangeBuilder;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.lsss.modules.korona.DataFilesCache;
import no.imr.lsss.modules.korona.DataObjectLoader;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.ExecutorObservation;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

final class GraphicalInfoLoader {
   private GraphicalInfoLoader() {
   }

   static DataObjectLoader<GraphicalInfo> make(ExecutorObservation executorObservation, DataFileSet dataFileSet, DataFilesCache<GraphicalInfo> dataFilesCache, Consumer<? super GraphicalInfo> listener) {
      return new DataObjectLoader<>(executorObservation, dataFileSet, dataFilesCache, listener, GraphicalInfoTocSubDatagram.class, GraphicalInfoLoader::graphicalInfos);
   }

   private static Stream<GraphicalInfo> graphicalInfos(DataFileSet dataFileSet, Ping ping) {
      return ping.getPingItems(GraphicalInfoSubDatagram.class)
            .map(graphicalInfoSubDatagram -> createGraphicalInfo(dataFileSet, ping.getInstant(), graphicalInfoSubDatagram));
   }

   private static GraphicalInfo createGraphicalInfo(DataFileSet dataFileSet, Instant instant, GraphicalInfoSubDatagram graphicalInfoSubDatagram) {
      PingIndex referenceIndex = dataFileSet.getClosestPingIndex(PingMapping.instantToTimeValue(instant), PingMapping.TIME);
      PingRangeBuilder pingRangeBuilder = new PingRangeBuilder();
      List<String> datagramTexts = graphicalInfoSubDatagram.getTexts();
      List<EchogramGraphicalInfo> graphicalInfos = graphicalInfoSubDatagram.getGraphicalObjects().stream()
            .<EchogramGraphicalInfo>map(graphicalObject -> {
               List<String> texts = Utils.toList(graphicalObject.texts(), datagramTexts);
               return switch (graphicalObject) {
                  case OffsetPolygon offsetPolygon -> {
                     List<EchogramPoint> echogramPoints = offsetPolygon.points().stream()
                           .map(echogramOffsetPoint -> {
                              long pingNumber = referenceIndex.getPingNumber() + echogramOffsetPoint.pingNumberOffset();
                              PingIndex pingIndex = dataFileSet.getPingIndexClamped(pingNumber);
                              pingRangeBuilder.add(pingIndex);
                              return new EchogramPoint(pingIndex, echogramOffsetPoint.depth());
                           })
                           .toList();
                     yield new EchogramPolygon(echogramPoints, offsetPolygon.color(), offsetPolygon.filled(), texts);
                  }
               };
            })
            .toList();
      PingRange pingRange = pingRangeBuilder.build(dataFileSet);
      return new GraphicalInfo(pingRange, graphicalInfos);
   }
}
