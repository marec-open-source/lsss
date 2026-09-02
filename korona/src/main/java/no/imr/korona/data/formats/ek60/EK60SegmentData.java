package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.datagrams.DatagramType;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.datagrams.Mru0Datagram;
import no.imr.korona.data.datagrams.Mru1Datagram;
import no.imr.korona.data.datagrams.MruDatagram;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.util.DataUtils;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Reads pings from EK60 data.
 */
class EK60SegmentData extends SegmentData {
   private static final int MAX_PING_SEARCHING_FOR_MRU = 10;

   private final RawFile rawFile;
   private final List<Idx0Datagram> idx0Datagrams;
   private final List<PingItem> otherIdxPingItems;
   private final @Nullable WrapAround wrapAround;
   private final List<Bot0Datagram> bot0Datagrams;
   private final Map<Idx0Datagram, Optional<MruDatagram>> missingMru = new ConcurrentHashMap<>();
   private long fileEndOffset;

   EK60SegmentData(RawFile rawFile, List<Idx0Datagram> idx0Datagrams, List<PingItem> otherIdxPingItems, @Nullable WrapAround wrapAround, List<Bot0Datagram> bot0Datagrams) throws IOException {
      this.rawFile = rawFile;
      this.idx0Datagrams = idx0Datagrams;
      this.otherIdxPingItems = otherIdxPingItems;
      this.wrapAround = wrapAround;
      this.bot0Datagrams = bot0Datagrams;
      fileEndOffset = rawFile.size();
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return rawFile.getPingConfiguration();
   }

   @Override
   public void close() throws IOException {
      rawFile.close();
   }

   @Override
   public List<Idx0Datagram> getPingIndices() {
      return idx0Datagrams;
   }

   @Override
   public List<PingItem> getOtherIdxPingItems() {
      return otherIdxPingItems;
   }

   @Override
   public @Nullable WrapAround getWrapAround() {
      return wrapAround;
   }

   @Override
   public List<Bot0Datagram> getBot0Datagrams() {
      return bot0Datagrams;
   }

   @Override
   public PingData loadPingData(PingIndex pingIndex, AsyncHandle asyncHandle) throws IOException {
      int index = (int) (pingIndex.getPingNumber() - idx0Datagrams.getFirst().getPingNumber());
      List<BaseDatagram> datagrams = loadDatagrams(index, _ -> true, asyncHandle);
      PingData pingData = new PingData(getPingConfiguration());
      PingConversion pingConversion = new PingConversion(rawFile.getFile(), getPingConfiguration(), datagrams, () -> getMissingMruDatagram(index, asyncHandle));
      pingData.addAll(pingConversion.getPingItems());
      return pingData;
   }

   private List<BaseDatagram> loadDatagrams(int index, Predicate<DatagramType> readPredicate, AsyncHandle asyncHandle) throws IOException {
      long startOffset = idx0Datagrams.get(index).getFileOffset();
      long endOffset = index + 1 < idx0Datagrams.size() ? idx0Datagrams.get(index + 1).getFileOffset() : fileEndOffset;
      return rawFile.loadDatagrams(startOffset, endOffset, readPredicate, asyncHandle);
   }

   void setFileEndOffset(long fileEndOffset) {
      this.fileEndOffset = fileEndOffset;
   }

   private @Nullable MruDatagram getMissingMruDatagram(int index, AsyncHandle asyncHandle) {
      return missingMru.computeIfAbsent(idx0Datagrams.get(index), idx0Datagram -> {
         MruDatagram lastMru = searchForMruDatagram(index, -1, asyncHandle);
         MruDatagram nextMru = searchForMruDatagram(index, 1, asyncHandle);
         if (lastMru == null) {
            return Optional.ofNullable(nextMru);
         }
         if (nextMru == null) {
            return Optional.of(lastMru);
         }
         return Optional.of(DataUtils.interpolateMru(lastMru, nextMru, idx0Datagram.getInstant()));
      }).orElse(null);
   }

   private @Nullable MruDatagram searchForMruDatagram(int referenceIndex, int direction, AsyncHandle asyncHandle) {
      int index = referenceIndex;
      for (int i = 0; i < MAX_PING_SEARCHING_FOR_MRU; i++) {
         index += direction;
         if (index < 0 || index >= idx0Datagrams.size()) {
            break;
         }
         List<BaseDatagram> datagrams;
         try {
            datagrams = loadDatagrams(index, Set.of(Mru0Datagram.TYPE, Mru1Datagram.TYPE)::contains, asyncHandle);
         } catch (IOException _) {
            // Ignore error in this situation.
            break;
         }
         MruDatagram mru = Utils.getFirstOrNull(datagrams, MruDatagram.class);
         if (mru != null) {
            return mru;
         }
      }
      return null;
   }
}
