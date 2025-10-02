package no.imr.lsss.modules.korona.tracking;

import no.imr.korona.data.datagrams.Cas0Datagram;
import no.imr.korona.data.datagrams.Cat0Datagram;
import no.imr.korona.data.datagrams.TNF0Datagram;
import no.imr.korona.data.datagrams.TTC0Datagram;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.modules.korona.DataFilesCache;
import no.imr.lsss.modules.korona.DataObjectLoader;
import no.imr.tools.concurrent.ExecutorObservation;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class TrackInfoLoader {
   private TrackInfoLoader() {
   }

   static DataObjectLoader<TrackInfo> make(ExecutorObservation executorObservation, DataFileSet dataFileSet, DataFilesCache<TrackInfo> dataFilesCache, Consumer<? super TrackInfo> listener) {
      return new DataObjectLoader<>(executorObservation, dataFileSet, dataFilesCache, listener, TTC0Datagram.class, TrackInfoLoader::tracks);
   }

   private static Stream<TrackInfo> tracks(DataFileSet dataFileSet, Ping ping) {
      Map<Integer, Cat0Datagram> cat0Datagrams = ping.getPingItems(Cat0Datagram.class)
            .collect(Collectors.toMap(Cas0Datagram::getRegionId, Function.identity()));
      return ping.getPingItems(TNF0Datagram.class)
            .filter(TNF0Datagram::isValid)
            .map(tnf0Datagram -> {
               Cat0Datagram cat0Datagram = cat0Datagrams.get(tnf0Datagram.getId());
               PingIndex begin = dataFileSet.getPingIndexOrNull(ping.getPingNumber() - tnf0Datagram.getPingsSinceFirst());
               if (begin == null) {
                  return null;
               }
               PingIndex end = dataFileSet.getPingIndexOrNull(ping.getPingNumber() - tnf0Datagram.getPingsSinceLast() + 1);
               if (end == null) {
                  return null;
               }
               return new TrackInfo(new TrackId(ping, tnf0Datagram.getId()), cat0Datagram, PingRange.of(begin, end));
            })
            .filter(Objects::nonNull);
   }
}
