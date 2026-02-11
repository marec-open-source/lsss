package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.DataException;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.formats.ek60.io.EndOfInputHandler;
import no.imr.korona.data.formats.ek60.io.FileDatagramReader;
import no.imr.korona.data.formats.ek60.io.FileDatagramWriter;
import no.imr.korona.data.formats.ek60.io.RandomAccessDatagramReader;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndexCorrectionOptions;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.OtherIdxPingItem;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.geo.Earth;
import no.imr.tools.io.FileUtils;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class EK60Utils {
   private EK60Utils() {
   }

   public static @Nullable IdxFile createIdxFile(DatagramTypeManager datagramTypeManager, EK60FileSet ek60FileSet, long beginPingNumber,
                                                 AsyncHandle asyncHandle, ProgressHandler progressHandler,
                                                 PingIndexCorrectionOptions pingIndexCorrectionOptions, boolean collapseSequentialPinging) throws IOException {
      try (EK60PingReader pingReader = new EK60PingReaderRawOnly(ek60FileSet.getRaw(), EndOfInputHandler.noWait(), beginPingNumber, pingIndexCorrectionOptions, datagramTypeManager, false)) {
         ScheduledFuture<?> future = Exec.scheduleWithFixedDelay(() -> progressHandler.setProgress(pingReader.getReadFraction()), 100, 100, TimeUnit.MILLISECONDS);
         try {
            List<Idx0Datagram> idx0Datagrams = new ArrayList<>();
            List<PingItem> otherIdxPingItems = new ArrayList<>();
            Utils.getAllOfType(pingReader.getPingConfiguration().getOtherConfigurationItems(), OtherIdxPingItem.class).forEach(otherIdxPingItems::add);
            Set<Integer> channels = new HashSet<>();
            while (!asyncHandle.isCancelled()) {
               Ping ping = pingReader.nextPing(asyncHandle);
               if (ping == null) {
                  break;
               }
               ping.getPingItems(OtherIdxPingItem.class).forEach(otherIdxPingItems::add);
               progressHandler.setProgress(pingReader.getReadFraction());
               if (collapseSequentialPinging) {
                  Set<Integer> newChannels = ping.getNonNullChannelDatas()
                        .map(ChannelData::getChannel)
                        .collect(Collectors.toSet());
                  if (idx0Datagrams.isEmpty() || newChannels.stream().anyMatch(channels::contains)) {
                     Idx0Datagram idx0Datagram = (Idx0Datagram) ping.getPingIndex();
                     idx0Datagram.setPingNumber(beginPingNumber + idx0Datagrams.size());
                     idx0Datagrams.add(idx0Datagram);
                     channels.clear();
                  }
                  channels.addAll(newChannels);
               } else {
                  Idx0Datagram idx0Datagram = (Idx0Datagram) ping.getPingIndex();
                  idx0Datagrams.add(idx0Datagram);
               }
            }
            if (asyncHandle.isCancelled()) {
               return null;
            }
            return new IdxFile(ek60FileSet.getIdx(), idx0Datagrams, pingReader.getPingConfiguration().getRawFileConfiguration(), otherIdxPingItems, null);
         } finally {
            future.cancel(false);
         }
      }
   }

   public static Stream<EK60SegmentHandle> getEK60WithMissingIdx(Collection<? extends SegmentHandle> segmentHandles) {
      return Utils.getAllOfType(segmentHandles, EK60SegmentHandle.class)
            .filter(ek60SegmentHandle -> !Files.exists(ek60SegmentHandle.getEK60FileSet().getIdx()));
   }

   static void write(IdxFile idxFile) throws IOException {
      Path tmpFile = FileUtils.addSuffix(idxFile.file(), ".tmp");
      try (FileDatagramWriter datagramWriter = new FileDatagramWriter(tmpFile)) {
         datagramWriter.writeDatagrams(idxFile.rawFileConfiguration().toDatagrams());
         for (PingItem pingItem : idxFile.otherPingItems()) {
            datagramWriter.writeDatagrams(pingItem.toDatagrams());
         }
         datagramWriter.writeDatagrams(idxFile.idx0Datagrams());
      }
      Files.deleteIfExists(idxFile.file());
      FileUtils.move(tmpFile, idxFile.file());
   }

   public static SegmentInfo createSegmentInfo(Path idxFile, DatagramTypeManager datagramTypeManager) throws IOException {
      if (!IdxFile.useIdxFiles) {
         return createMissingIdxSegmentInfo(idxFile);
      }
      try (RandomAccessDatagramReader datagramReader = new FileDatagramReader(idxFile, datagramTypeManager)) {
         return createSegmentInfo(datagramReader, idxFile, datagramTypeManager);
      } catch (IOException e) {
         if (FileUtils.notExists(e, idxFile)) {
            return createMissingIdxSegmentInfo(idxFile);
         }
         throw e;
      }
   }

   private static SegmentInfo createMissingIdxSegmentInfo(Path aIdxFile) throws IOException {
      IdxFile idxFile = MissingIdxFileHandler.load(aIdxFile);
      return createSegmentInfo(idxFile.rawFileConfiguration(), idxFile.idx0Datagrams().getFirst(), idxFile.idx0Datagrams().getLast());
   }

   static SegmentInfo createSegmentInfo(RandomAccessDatagramReader datagramReader, Path aIdxFile, DatagramTypeManager datagramTypeManager) throws IOException {
      List<BaseDatagram> configurationDatagrams = new ArrayList<>();
      Idx0Datagram firstIdx = null;
      while (true) {
         BaseDatagram datagram = datagramReader.nextDatagram();
         if (datagram == null) {
            break;
         }
         if (datagram instanceof Idx0Datagram idx0Datagram) {
            firstIdx = idx0Datagram;
            break;
         }
         configurationDatagrams.add(datagram);
      }

      PingConfiguration pingConfiguration = null;
      if (!configurationDatagrams.isEmpty() && IdxFile.useIdxFiles) {
         try {
            pingConfiguration = PingConfigurationReader.toPingConfiguration(configurationDatagrams, aIdxFile);
         } catch (DataException _) {
            // This can happen with EK80 generated idx-files containing a XML0/Version datagram.
            // Use raw file instead.
         }
      }
      if (pingConfiguration == null) {
         pingConfiguration = PingConfigurationReader.read(new EK60FileSet(aIdxFile).getRaw(), datagramTypeManager);
      }
      RawFileConfiguration rawFileConfiguration = pingConfiguration.getRawFileConfiguration();

      if (firstIdx == null) {
         return new SegmentInfo(rawFileConfiguration, PingRange.EMPTY_RANGE);
      }

      Idx0Datagram lastIdx = readLastIdx0Datagram(datagramReader, aIdxFile);

      if (!isAcceptablePingRange(lastIdx, firstIdx)) {
         // Reading the entire idx file will apply vessel distance spike filter
         IdxFile idxFile = IdxFile.load(aIdxFile, datagramTypeManager, NoticeHandler.ignore());
         List<Idx0Datagram> idxDatagrams = idxFile.idx0Datagrams();
         if (!idxDatagrams.isEmpty()) {
            firstIdx = idxDatagrams.getFirst();
            lastIdx = idxDatagrams.getLast();
         }
      }

      return createSegmentInfo(rawFileConfiguration, firstIdx, lastIdx);
   }

   private static SegmentInfo createSegmentInfo(RawFileConfiguration rawFileConfiguration, Idx0Datagram firstIdx, Idx0Datagram lastIdx) {
      PingRange pingRange = firstAndLastToPingRange(firstIdx, lastIdx);
      return new SegmentInfo(rawFileConfiguration, pingRange);
   }

   static PingRange firstAndLastToPingRange(Idx0Datagram firstIdx, Idx0Datagram lastIdx) {
      Idx0Datagram end = new Idx0Datagram(lastIdx);

      // Increase ping number by 1 and time by 1 ms to avoid exclusion from ping range.
      end.setPingNumber(lastIdx.getPingNumber() + 1);
      end.setTimeInMillis(lastIdx.getTimeInMillis() + 1);
      // Expand vessel distance as well? Not guaranteed to be increasing.

      return PingRange.of(firstIdx, end);
   }

   private static boolean isAcceptablePingRange(Idx0Datagram lastIdx, Idx0Datagram firstIdx) {
      double deltaVesselDistance = lastIdx.getVesselDistance() - firstIdx.getVesselDistance();
      double minVesselDistance = 0;

      GeoPoint firstGeoPos = firstIdx.getGeographicalPosition();
      GeoPoint lastGeoPos = lastIdx.getGeographicalPosition();
      if (firstGeoPos != null && lastGeoPos != null) {
         minVesselDistance = Utils.meterToNmi(Earth.getApproximateDistance(firstGeoPos, lastGeoPos));
         minVesselDistance /= 2; // to be on the safe side
      }

      return deltaVesselDistance >= minVesselDistance && KoronaUtils.getKnots(firstIdx, lastIdx) <= KoronaUtils.MAX_KNOTS;
   }

   private static Idx0Datagram readLastIdx0Datagram(RandomAccessDatagramReader datagramReader, Path idxFile) throws IOException {
      // Try several times in case the input ends with a partially written Idx0Datagram
      for (int i = 0; i < Idx0Datagram.SIZE_ON_FILE; i++) {
         datagramReader.setPosition(datagramReader.getSize() - Idx0Datagram.SIZE_ON_FILE - i);
         BaseDatagram datagram = datagramReader.nextDatagram();
         if (datagram instanceof Idx0Datagram idx0Datagram) {
            // todo: Add notice if i != 0
            return idx0Datagram;
         }
      }
      throw new DataException("Error reading last index datagram in: " + idxFile);
   }
}
