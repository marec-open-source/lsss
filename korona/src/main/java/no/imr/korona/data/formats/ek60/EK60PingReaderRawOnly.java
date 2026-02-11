package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.BaseDepDatagram;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.Fil1Datagram;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.datagrams.MruDatagram;
import no.imr.korona.data.datagrams.Nme0Datagram;
import no.imr.korona.data.datagrams.Nqp0Datagram;
import no.imr.korona.data.datagrams.Pin0Datagram;
import no.imr.korona.data.datagrams.Xml0Datagram;
import no.imr.korona.data.formats.ek60.io.EndOfInputHandler;
import no.imr.korona.data.formats.ek60.io.FileDatagramReader;
import no.imr.korona.data.formats.missing.MissingBot0Datagram;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndexCorrectionFilter;
import no.imr.korona.data.ping.PingIndexCorrectionOptions;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ping reader using a raw file only.
 */
final class EK60PingReaderRawOnly extends EK60PingReader {
   private final PingIndexCorrectionOptions pingIndexCorrectionOptions;
   private final FileDatagramReader rawReader;

   private PingConfiguration pingConfiguration;
   private final List<PositionAndDatagram> bufferedDatagrams = new ArrayList<>();
   private @Nullable PingAssembler pingAssembler;
   private @Nullable MruDatagram latestMru;
   private long pingNumber;
   private long endOffset;
   private boolean readingEmptyPing;
   private PingIndexCorrectionFilter pingIndexCorrectionFilter;
   private boolean allowPingIndexBuffering = true;

   EK60PingReaderRawOnly(Path file, EndOfInputHandler endOfInputHandler, long beginPingNumber,
                         PingIndexCorrectionOptions pingIndexCorrectionOptions,
                         DatagramTypeManager datagramTypeManager, boolean realtime) throws IOException {
      this.pingIndexCorrectionOptions = pingIndexCorrectionOptions;
      rawReader = newFileDatagramReader(file, datagramTypeManager, realtime);
      rawReader.setEndOfInputHandler(endOfInputHandler);

      pingNumber = beginPingNumber;
      pingIndexCorrectionFilter = createPingIndexCorrectionFilter();

      List<BaseDatagram> configurationDatagrams = new ArrayList<>();
      Set<String> fil1ChannelIds = new HashSet<>();
      while (true) {
         long position = rawReader.getPosition();
         BaseDatagram datagram = rawReader.nextDatagram();
         if (datagram == null) {
            break;
         } else if (!bufferedDatagrams.isEmpty() || isPingDatagram(datagram)) {
            bufferedDatagrams.add(new PositionAndDatagram(position, datagram));
            endOffset = bufferedDatagrams.getFirst().position;
            if (datagram.isSampleDatagram()) {
               pingAssembler = new RawPingAssembler(datagram);
               break;
            }
         } else if (datagram instanceof Pin0Datagram pin0) {
            bufferedDatagrams.add(new PositionAndDatagram(position, datagram));
            pingAssembler = new Pin0PingAssembler(pin0);
            break;
         } else {
            if (datagram instanceof Fil1Datagram fil1Datagram) {
               fil1ChannelIds.add(fil1Datagram.channelId);
            }
            configurationDatagrams.add(datagram);
         }
      }
      pingConfiguration = PingConfigurationReader.toPingConfiguration(configurationDatagrams, rawReader.getFile());
      if (!fil1ChannelIds.isEmpty()) {
         Set<String> missingFil1ChannelIds = PingConfigurationReader.findMissingFil1ChannelIds(fil1ChannelIds, pingConfiguration.getRawFileConfiguration());
         if (!missingFil1ChannelIds.isEmpty()) {
            // We have some FIL1 datagrams, but not for all WBT channels. Happens on WBT mini.
            long position = rawReader.getPosition();
            PingConfigurationReader.readMissingFil1Datagrams(rawReader, missingFil1ChannelIds, configurationDatagrams, pingConfiguration.getRawFileConfiguration());
            pingConfiguration = PingConfigurationReader.toPingConfiguration(configurationDatagrams, rawReader.getFile());
            rawReader.setPosition(position);
         }
      }
      rawReader.setAcceptPredicate(new PingConfigurationAcceptPredicate(pingConfiguration));
      pingIndexCorrectionFilter.initFromPingConfiguration();
   }

   private PingIndexCorrectionFilter createPingIndexCorrectionFilter() {
      PingIndexCorrectionFilter filter = new PingIndexCorrectionFilter(new PingSource() {
         @Override
         public PingConfiguration getPingConfiguration() {
            return pingConfiguration;
         }

         @Override
         public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
            return getPingFromFile(asyncHandle);
         }

         @Override
         public void close() throws IOException {
            EK60PingReaderRawOnly.this.close();
         }
      });
      filter.setAllowBuffering(allowPingIndexBuffering);
      filter.setPingIndexCorrectionOptions(pingIndexCorrectionOptions);
      return filter;
   }

   static boolean isPingDatagram(BaseDatagram datagram) {
      return datagram.isSampleDatagram()
            || datagram instanceof MruDatagram
            || datagram instanceof Nqp0Datagram
            || datagram instanceof BaseDepDatagram
            || datagram instanceof Xml0Datagram xml0Datagram && xml0Datagram.getDocument().getRootElement().getName().equals("Parameter");
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
      rawReader.close();
   }

   @Override
   public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
      return pingIndexCorrectionFilter.nextPing(asyncHandle);
   }

   @Override
   public @Nullable Ping nextEmptyPing(AsyncHandle asyncHandle) throws IOException {
      readingEmptyPing = true;
      try {
         return nextPing(asyncHandle);
      } finally {
         readingEmptyPing = false;
      }
   }

   private @Nullable Ping getPingFromFile(AsyncHandle asyncHandle) throws IOException {
      return pingAssembler != null ? pingAssembler.nextPing(asyncHandle) : null;
   }

   private sealed interface PingAssembler {
      @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException;
   }

   private final class RawPingAssembler implements PingAssembler {
      private BaseDatagram raw;

      private RawPingAssembler(BaseDatagram raw) {
         this.raw = raw;
      }

      @Override
      public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
         while (true) {
            if (asyncHandle.isCancelled()) {
               return null;
            }
            long position = rawReader.getPosition();
            BaseDatagram datagram = rawReader.nextDatagram(asyncHandle);
            if (datagram == null) {
               // End of input: Create last ping.
               endOffset = rawReader.getSize();
               return createPing(raw.getNTDate(), Long.MAX_VALUE);
            } else if (datagram.isSampleDatagram() && datagram.getNTDate() != raw.getNTDate()) {
               // Found raw with different time => create ping.
               Ping ping = createPing(raw.getNTDate(), datagram.getNTDate());
               raw = datagram;
               bufferedDatagrams.add(new PositionAndDatagram(position, datagram));
               endOffset = bufferedDatagrams.getFirst().position;
               return ping;
            } else {
               if (datagram instanceof MruDatagram mruDatagram) {
                  latestMru = mruDatagram;
               }
               // Add to current ping.
               bufferedDatagrams.add(new PositionAndDatagram(position, datagram));
            }
         }
      }
   }

   private final class Pin0PingAssembler implements PingAssembler {
      private Pin0Datagram pin0;

      private Pin0PingAssembler(Pin0Datagram pin0) {
         this.pin0 = pin0;
      }

      @Override
      public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
         while (true) {
            if (asyncHandle.isCancelled()) {
               return null;
            }
            long position = rawReader.getPosition();
            BaseDatagram datagram = rawReader.nextDatagram(asyncHandle);
            if (datagram == null) {
               // End of input: Create last ping.
               endOffset = rawReader.getSize();
               return createPing(pin0.getNTDate(), Long.MAX_VALUE);
            } else if (datagram instanceof Pin0Datagram pin0Datagram) {
               // Start of next ping.
               Ping ping = createPing(pin0.getNTDate(), Long.MAX_VALUE);
               pin0 = pin0Datagram;
               bufferedDatagrams.add(new PositionAndDatagram(position, datagram));
               endOffset = bufferedDatagrams.getFirst().position;
               return ping;
            } else {
               // Add to current ping.
               bufferedDatagrams.add(new PositionAndDatagram(position, datagram));
            }
         }
      }
   }

   private @Nullable Ping createPing(long pingNTDate, long endNTDate) {
      if (bufferedDatagrams.isEmpty()) {
         return null;
      }

      List<PositionAndDatagram> datagramsInThisPing;
      if (endNTDate < Long.MAX_VALUE) {
         int endIndex = bufferedDatagrams.size();
         while (endIndex > 0) {
            BaseDatagram datagram = bufferedDatagrams.get(endIndex - 1).datagram;
            if ((datagram.isSampleDatagram() || datagram.getNTDate() < endNTDate) &&
                  !(datagram instanceof Nme0Datagram)) {
               break;
            }
            endIndex--;
         }
         datagramsInThisPing = bufferedDatagrams.subList(0, endIndex);
      } else {
         // End of input: Use all remaining datagrams
         datagramsInThisPing = bufferedDatagrams;
      }

      Idx0Datagram idx0Datagram = new Idx0Datagram(pingNTDate, pingNumber++, 0, null, datagramsInThisPing.getFirst().position);
      Bot0Datagram bot0Datagram = new MissingBot0Datagram(pingConfiguration.getRawFileConfiguration(), idx0Datagram);
      Ping ping = new DefaultPing(pingConfiguration, idx0Datagram, bot0Datagram);
      if (!readingEmptyPing) {
         List<BaseDatagram> datagrams = new ArrayList<>(datagramsInThisPing.size());
         datagramsInThisPing.forEach(positionAndDatagram -> datagrams.add(positionAndDatagram.datagram));
         ping.addAll(new PingConversion(getFile(), pingConfiguration, datagrams, () -> latestMru).getPingItems());
      }
      datagramsInThisPing.clear();
      return ping;
   }

   @Override
   public float getReadFraction() {
      try {
         return (float) rawReader.getPosition() / (float) rawReader.getSize();
      } catch (IOException _) {
         return super.getReadFraction();
      }
   }

   @Override
   public long getEndOffset() {
      return endOffset;
   }

   @Override
   public long getPosition() throws IOException {
      return rawReader.getPosition();
   }

   @Override
   public void skipToEnd(AsyncHandle asyncHandle) throws IOException {
      endOffset = rawReader.getSize();
      rawReader.setPosition(endOffset);

      // Loose old state
      latestMru = null;
      pingIndexCorrectionFilter = createPingIndexCorrectionFilter();
      nextEmptyPing(asyncHandle);
   }

   @Override
   public void disallowPingIndexBuffering() {
      allowPingIndexBuffering = false;
      pingIndexCorrectionFilter.setAllowBuffering(false);
   }

   private record PositionAndDatagram(long position, BaseDatagram datagram) {
   }
}
