package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.DataException;
import no.imr.korona.data.DatagramSource;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.formats.ek60.io.ByteBufferDatagramReader;
import no.imr.korona.data.formats.ek60.io.RandomAccessDatagramReader;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.UnionList;
import no.imr.tools.io.FileUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The EK60 index file.
 */
public final class IdxFile {
   public static boolean useIdxFiles = true;

   private final Path file;
   private final List<Idx0Datagram> idx0Datagrams;
   private final RawFileConfiguration rawFileConfiguration;
   private final List<PingItem> otherPingItems;
   private final @Nullable WrapAround wrapAround;

   IdxFile(Path file, List<Idx0Datagram> idx0Datagrams, RawFileConfiguration rawFileConfiguration, List<PingItem> otherPingItems, @Nullable WrapAround wrapAround) {
      this.file = file;
      this.idx0Datagrams = idx0Datagrams;
      this.rawFileConfiguration = rawFileConfiguration;
      this.otherPingItems = otherPingItems;
      this.wrapAround = wrapAround;
   }

   private IdxFile(NoticeHandler noticeHandler, Path file, DatagramTypeManager datagramTypeManager) throws IOException {
      this.file = file;

      try (RandomAccessDatagramReader datagramReader = new ByteBufferDatagramReader(FileUtils.toByteBuffer(file, ByteOrder.LITTLE_ENDIAN), datagramTypeManager)) {
         List<BaseDatagram> configurationDatagrams = new ArrayList<>();
         List<Idx0Datagram> firstIdx = List.of();
         while (true) {
            BaseDatagram datagram = datagramReader.nextDatagram();
            if (datagram == null) {
               break;
            }
            if (datagram instanceof Idx0Datagram idx0Datagram) {
               firstIdx = List.of(idx0Datagram);
               break;
            }
            configurationDatagrams.add(datagram);
         }

         PingConfiguration pingConfiguration;
         try {
            pingConfiguration = PingConfigurationReader.toPingConfiguration(configurationDatagrams, file);
         } catch (DataException e) {
            pingConfiguration = PingConfigurationReader.read(new EK60FileSet(file).getRaw(), datagramTypeManager);
         }
         rawFileConfiguration = pingConfiguration.getRawFileConfiguration();

         long remaining = datagramReader.getSize() - datagramReader.getPosition();
         ArrayList<Idx0Datagram> idxDatagrams = new ArrayList<>(1 + (int) (remaining / Idx0Datagram.getSize()));

         IdxCorrectionFilter idxCorrectionFilter = new IdxCorrectionFilter(
               DatagramSource.concat(DatagramSource.ofDatagrams(firstIdx), datagramReader));
         while (true) {
            Idx0Datagram idx0Datagram = idxCorrectionFilter.nextDatagram();
            if (idx0Datagram == null) {
               break;
            }
            idxDatagrams.add(idx0Datagram);
         }

         if (idxCorrectionFilter.getMissingCount() != 0) {
            noticeHandler.addNotice("Created " + idxCorrectionFilter.getMissingCount() + " missing ping indices");
         }
         /*
         if (idxCorrectionFilter.getIgnoredCount() != 0) {
            noticeHandler.addNotice("Ignored " + idxCorrectionFilter.getIgnoredCount() + " IDX0 datagrams with non-increasing ping number");
         }
         */
         if (idxCorrectionFilter.getSpikeCount() != 0) {
            noticeHandler.addNotice("Removed " + idxCorrectionFilter.getSpikeCount() + " vessel distance spikes");
         }
         if (idxCorrectionFilter.getWrapAround() != null) {
            noticeHandler.addNotice("Vessel distance wrap around at " + idxCorrectionFilter.getWrapAround().vesselDistance() + " nmi");
         }
         if (idxCorrectionFilter.getTimeCorrectionCount() != 0) {
            noticeHandler.addNotice("Corrected time on " + idxCorrectionFilter.getTimeCorrectionCount() + " pings");
         }

         idxDatagrams.trimToSize();
         idx0Datagrams = idxDatagrams;
         otherPingItems = new UnionList<>(pingConfiguration.getOtherConfigurationItems(), new PingConversion(file, idxCorrectionFilter.getOtherDatagrams()).getPingItems());
         wrapAround = idxCorrectionFilter.getWrapAround();
      }
   }

   public static IdxFile load(Path file, DatagramTypeManager datagramTypeManager, NoticeHandler noticeHandler) throws IOException {
      if (!useIdxFiles) {
         return MissingIdxFileHandler.load(file);
      }
      try {
         return new IdxFile(noticeHandler, file, datagramTypeManager);
      } catch (IOException e) {
         if (FileUtils.notExists(e, file)) {
            return MissingIdxFileHandler.load(file);
         }
         throw e;
      }
   }

   public Path getFile() {
      return file;
   }

   public RawFileConfiguration getRawFileConfiguration() {
      return rawFileConfiguration;
   }

   public List<Idx0Datagram> getIdx0Datagrams() {
      return idx0Datagrams;
   }

   public List<PingItem> getOtherPingItems() {
      return otherPingItems;
   }

   public @Nullable WrapAround getWrapAround() {
      return wrapAround;
   }

   @Override
   public String toString() {
      return file.toString();
   }
}
