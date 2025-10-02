package no.imr.lsss.modules.korona.region;

import no.imr.korona.data.datagrams.RegionInfoDatagram;
import no.imr.korona.data.datagrams.RegionTableOfContentsDatagram;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.lsss.modules.korona.DataFilesCache;
import no.imr.lsss.modules.korona.DataObjectLoader;
import no.imr.tools.concurrent.ExecutorObservation;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Finds all KORONA regions in a background process.
 */
final class KoronaRegionLoader {
   private KoronaRegionLoader() {
   }

   static DataObjectLoader<KoronaRegionLSSS> make(ExecutorObservation executorObservation, DataFileSet dataFileSet, DataFilesCache<KoronaRegionLSSS> dataFilesCache, Consumer<? super KoronaRegionLSSS> listener) {
      return new DataObjectLoader<>(executorObservation, dataFileSet, dataFilesCache, listener, RegionTableOfContentsDatagram.class, KoronaRegionLoader::koronaRegions);
   }

   private static Stream<KoronaRegionLSSS> koronaRegions(DataFileSet dataFileSet, Ping ping) {
      return ping.getPingItems(RegionInfoDatagram.class)
            .filter(RegionInfoDatagram::isAccepted)
            .map(regionInfoDatagram -> {
               return new KoronaRegionLSSS(regionInfoDatagram, ping, dataFileSet);
            });
   }
}
