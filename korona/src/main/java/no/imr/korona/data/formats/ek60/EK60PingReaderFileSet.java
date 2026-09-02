package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.datagrams.MruDatagram;
import no.imr.korona.data.formats.ek60.io.EndOfInputHandler;
import no.imr.korona.data.formats.ek60.io.FileDatagramReader;
import no.imr.korona.data.formats.missing.MissingBot0Datagram;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads pings from an {@link EK60FileSet}.
 */
final class EK60PingReaderFileSet extends EK60PingReader {
   private final FileDatagramReader idxReader;
   private final @Nullable FileDatagramReader botReader;
   private final FileDatagramReader rawReader;

   private final IdxCorrectionFilter idxCorrectionFilter;
   private final PingConfiguration pingConfiguration;
   private @Nullable Idx0Datagram currentIdx;
   private @Nullable MruDatagram latestMru;
   private long endOffset;

   EK60PingReaderFileSet(EK60FileSet ek60FileSet, EndOfInputHandler endOfInputHandler, DatagramTypeManager datagramTypeManager, boolean realtime) throws IOException {
      idxReader = newFileDatagramReader(ek60FileSet.getIdx(), datagramTypeManager, realtime);
      idxReader.setEndOfInputHandler(endOfInputHandler);

      if (Files.exists(ek60FileSet.getBot())) {
         botReader = newFileDatagramReader(ek60FileSet.getBot(), datagramTypeManager, realtime);
         botReader.setEndOfInputHandler(endOfInputHandler);
      } else {
         botReader = null;
      }

      rawReader = newFileDatagramReader(ek60FileSet.getRaw(), datagramTypeManager, realtime);
      rawReader.setEndOfInputHandler(endOfInputHandler);

      skipConfiguration(idxReader, Idx0Datagram.class);
      if (botReader != null) {
         skipConfiguration(botReader, Bot0Datagram.class);
      }

      pingConfiguration = PingConfigurationReader.read(rawReader);
      rawReader.setAcceptPredicate(new PingConfigurationAcceptPredicate(pingConfiguration));

      idxCorrectionFilter = new IdxCorrectionFilter(idxReader);

      currentIdx = idxCorrectionFilter.nextDatagram();
   }

   private static void skipConfiguration(FileDatagramReader datagramReader, Class<? extends BaseDatagram> clazz) throws IOException {
      while (true) {
         long position = datagramReader.getPosition();
         BaseDatagram datagram = datagramReader.nextDatagram();
         if (datagram == null) {
            return;
         }
         if (clazz.isInstance(datagram)) {
            datagramReader.setPosition(position);
            return;
         }
      }
   }

   @Override
   public Path getFile() {
      return rawReader.getFile();
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pingConfiguration;
   }

   @Override
   public void close() throws IOException {
      idxReader.close();
      if (botReader != null) {
         botReader.close();
      }
      rawReader.close();
   }

   @Override
   public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
      return nextPing(asyncHandle, true);
   }

   @Override
   public @Nullable Ping nextEmptyPing(AsyncHandle asyncHandle) throws IOException {
      return nextPing(asyncHandle, false);
   }

   private @Nullable Ping nextPing(AsyncHandle asyncHandle, boolean readRaw) throws IOException {
      if (currentIdx == null) {
         return null;
      }

      Idx0Datagram nextIdx = idxCorrectionFilter.nextDatagram();

      Bot0Datagram bot0Datagram = readBot0Datagram(currentIdx, asyncHandle);
      bot0Datagram.setInstant(currentIdx.getInstant());

      List<BaseDatagram> datagrams = new ArrayList<>();
      endOffset = nextIdx != null ? nextIdx.getFileOffset() : rawReader.getSize();
      if (readRaw) {
         rawReader.setPosition(currentIdx.getFileOffset());
         while (rawReader.getPosition() < endOffset) {
            if (asyncHandle.isCancelled()) {
               return null;
            }
            BaseDatagram datagram = rawReader.nextDatagram(asyncHandle);
            if (datagram == null) {
               break;
            }
            datagram.setInstant(currentIdx.getInstant());
            if (datagram instanceof MruDatagram mruDatagram) {
               latestMru = mruDatagram;
            }
            datagrams.add(datagram);
         }
      }

      Ping ping = new DefaultPing(pingConfiguration, currentIdx, bot0Datagram);
      ping.addAll(new PingConversion(getFile(), pingConfiguration, datagrams, () -> latestMru).getPingItems());

      currentIdx = nextIdx;

      return ping;
   }

   private Bot0Datagram readBot0Datagram(Idx0Datagram currentIdx, AsyncHandle asyncHandle) throws IOException {
      BaseDatagram datagram = botReader != null ? botReader.nextDatagram(asyncHandle) : null;
      if (datagram instanceof Bot0Datagram bot0Datagram) {
         return bot0Datagram;
      } else {
         return new MissingBot0Datagram(pingConfiguration.getRawFileConfiguration(), currentIdx);
      }
   }

   @Override
   public long getEndOffset() {
      return endOffset;
   }

   @Override
   public long getPosition() {
      return endOffset;
   }

   @Override
   public void skipToEnd(AsyncHandle asyncHandle) throws IOException {
      long currentSize = rawReader.getSize();
      while (true) {
         Ping ping = nextEmptyPing(asyncHandle);
         if (ping == null) {
            break;
         }
         if (endOffset >= currentSize) {
            break;
         }
      }
   }
}
