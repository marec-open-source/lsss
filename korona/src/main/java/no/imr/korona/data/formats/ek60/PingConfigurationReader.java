package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.DataException;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.Fil1Datagram;
import no.imr.korona.data.datagrams.Phy0Datagram;
import no.imr.korona.data.datagrams.Sin0Datagram;
import no.imr.korona.data.datagrams.Ver0Datagram;
import no.imr.korona.data.datagrams.Xml0Datagram;
import no.imr.korona.data.datagrams.subdatagrams.config.ExtraRawFileConfiguration;
import no.imr.korona.data.formats.ek60.calibration.CalibrationFile;
import no.imr.korona.data.formats.ek60.io.FileDatagramReader;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlParse;
import no.imr.tools.xml.XmlParseException;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class PingConfigurationReader {
   private PingConfigurationReader() {
   }

   static PingConfiguration read(Path file, DatagramTypeManager datagramTypeManager) throws IOException {
      try (FileDatagramReader datagramReader = new FileDatagramReader(file, datagramTypeManager)) {
         return read(datagramReader);
      }
   }

   static PingConfiguration read(FileDatagramReader datagramReader) throws IOException {
      List<BaseDatagram> datagrams = new ArrayList<>();
      Set<String> fil1ChannelIds = new HashSet<>();
      while (true) {
         BaseDatagram datagram = datagramReader.nextDatagram();
         if (datagram == null) {
            break;
         }
         if (EK60PingReaderRawOnly.isPingDatagram(datagram)) {
            break;
         }
         if (datagram instanceof Fil1Datagram fil1Datagram) {
            fil1ChannelIds.add(fil1Datagram.channelId);
         }
         datagrams.add(datagram);
      }

      PingConfiguration pingConfiguration = toPingConfiguration(datagrams, datagramReader.getFile());
      if (!fil1ChannelIds.isEmpty()) {
         Set<String> missingFil1ChannelIds = findMissingFil1ChannelIds(fil1ChannelIds, pingConfiguration.getRawFileConfiguration());
         if (!missingFil1ChannelIds.isEmpty()) {
            // We have some FIL1 datagrams, but not for all WBT channels. Happens on WBT mini.
            readMissingFil1Datagrams(datagramReader, missingFil1ChannelIds, datagrams, pingConfiguration.getRawFileConfiguration());
            pingConfiguration = toPingConfiguration(datagrams, datagramReader.getFile());
         }
      }
      return pingConfiguration;
   }

   static Set<String> findMissingFil1ChannelIds(Set<String> fil1ChannelIds, RawFileConfiguration rawFileConfiguration) {
      return rawFileConfiguration.getTransducers().stream()
            .filter(RawFileTransducer::isWBT)
            .map(RawFileTransducer::getChannelId)
            .filter(Predicate.not(fil1ChannelIds::contains))
            .collect(Collectors.toSet());
   }

   static void readMissingFil1Datagrams(FileDatagramReader datagramReader, Set<String> missingFil1ChannelIds, List<BaseDatagram> datagrams, RawFileConfiguration rawFileConfiguration) throws IOException {
      Set<Instant> pingTimes = new HashSet<>();
      while (true) {
         BaseDatagram datagram = datagramReader.nextDatagram();
         if (datagram == null) {
            return;
         }
         if (datagram instanceof Fil1Datagram fil1Datagram) {
            missingFil1ChannelIds.remove(fil1Datagram.channelId);
            datagrams.add(fil1Datagram);
            continue; // There might be more FIL1 datagrams for this channel id
         }
         if (EK60PingReaderRawOnly.isPingDatagram(datagram)) {
            if (missingFil1ChannelIds.isEmpty()) {
               return;
            }
            pingTimes.add(datagram.getInstant());
            if (pingTimes.size() >= rawFileConfiguration.getTransducerCount()) {
               // Did not get all FIL1 datagrams, but have read enough pings => Give up.
               Log.global.warning("Did not find FIL1 datagrams for all WBT channels in " + datagramReader.getFile() + ": " + missingFil1ChannelIds);
               return;
            }
         }
      }
   }

   static PingConfiguration toPingConfiguration(List<BaseDatagram> datagrams, Path file) throws DataException {
      List<PingItem> configurationItems = new PingConversion(file, datagrams).getPingItems();
      initRawFileConfiguration(configurationItems, file);
      return new PingConfiguration(configurationItems);
   }

   private static void initRawFileConfiguration(List<PingItem> configurationItems, Path file) throws DataException {
      RawFileConfiguration rawFileConfiguration = getOrCreateRawFileConfiguration(configurationItems, file);
      if (rawFileConfiguration == null) {
         throw new DataException("Missing configuration datagrams in file " + file);
      }
      ExtraRawFileConfiguration extraRawFileConfiguration = Utils.getFirstOrNull(configurationItems, ExtraRawFileConfiguration.class);
      if (extraRawFileConfiguration != null) {
         rawFileConfiguration.applyExtraRawFileConfiguration(extraRawFileConfiguration);
      }
      rawFileConfiguration.setDataFile(file);
      CalibrationFile calibrationFile = CalibrationFile.forDirectory(file.getParent());
      if (calibrationFile.exists()) {
         rawFileConfiguration.setCalibrationFile(calibrationFile);
      }
   }

   private static @Nullable RawFileConfiguration getOrCreateRawFileConfiguration(List<PingItem> configurationItems, Path file) {
      for (int i = 0; i < configurationItems.size(); i++) {
         PingItem item = configurationItems.get(i);
         if (item instanceof RawFileConfiguration rawFileConfiguration) {
            if (i > 0) {
               Collections.swap(configurationItems, i, 0);
            }
            tryParseEnvironmentXml(configurationItems, file, rawFileConfiguration);
            return rawFileConfiguration;
         }
      }
      RawFileConfiguration rawFileConfiguration = rawFileConfigurationFromXml0(configurationItems, file);
      if (rawFileConfiguration != null) {
         return rawFileConfiguration;
      }
      return rawFileConfigurationFromSin0(configurationItems);
   }

   private static void tryParseEnvironmentXml(List<PingItem> configurationItems, Path file, RawFileConfiguration rawFileConfiguration) {
      Xml0Datagram xml0Configuration = getXml0Datagram(configurationItems, "Configuration");
      if (xml0Configuration == null) {
         return;
      }
      Xml0Datagram xml0Environment = getXml0Datagram(configurationItems, "Environment");
      if (xml0Environment == null) {
         return;
      }
      try {
         Element configuration = xml0Configuration.getDocument().getRootElement();
         Element environment = xml0Environment.getDocument().getRootElement();
         Element header = XmlParse.element(configuration, "Header");
         String fileFormatVersion = XmlParse.stringAttribute(header, "FileFormatVersion", "");
         RawFileConfiguration.Xml0Info xml0Info = Xml0DatagramFactory.createXml0Info(environment, fileFormatVersion);
         rawFileConfiguration.setXml0Info(xml0Info);

         configurationItems.remove(xml0Configuration);
         configurationItems.remove(xml0Environment);
      } catch (XmlParseException e) {
         Log.global.warning("Error parsing configuration datagrams in file " + file + ": " + e);
      }
   }

   private static @Nullable RawFileConfiguration rawFileConfigurationFromXml0(List<PingItem> configurationItems, Path file) {
      Xml0Datagram xml0Configuration = getXml0Datagram(configurationItems, "Configuration");
      if (xml0Configuration == null) {
         return null;
      }
      Xml0Datagram xml0Environment = getXml0Datagram(configurationItems, "Environment");
      if (xml0Environment == null) {
         return null;
      }
      Xml0Datagram xml0InitialParameter = getXml0Datagram(configurationItems, "InitialParameter");
      try {
         Element configuration = xml0Configuration.getDocument().getRootElement();
         Element environment = xml0Environment.getDocument().getRootElement();
         Element initialParameter = xml0InitialParameter != null ? xml0InitialParameter.getDocument().getRootElement() : null;
         RawFileConfiguration rawFileConfiguration = Xml0DatagramFactory.createRawFileConfiguration(xml0Configuration.getInstant(),
               configuration, environment, initialParameter, configurationItems, file);
         configurationItems.addFirst(rawFileConfiguration);

         configurationItems.remove(xml0Configuration);
         configurationItems.remove(xml0Environment);

         return rawFileConfiguration;
      } catch (XmlParseException e) {
         Log.global.warning("Error parsing configuration datagrams in file " + file + ": " + e);
         return null;
      }
   }

   private static @Nullable Xml0Datagram getXml0Datagram(List<PingItem> pingItems, String rootElementName) {
      return Utils.getAllOfType(pingItems, Xml0Datagram.class)
            .filter(xml0 -> xml0.getDocument().getRootElement().getName().equals(rootElementName))
            .findFirst()
            .orElse(null);
   }

   private static @Nullable RawFileConfiguration rawFileConfigurationFromSin0(List<PingItem> configurationItems) {
      Sin0Datagram sin0Datagram = Utils.getFirstOrNull(configurationItems, Sin0Datagram.class);
      if (sin0Datagram == null) {
         return null;
      }
      Ver0Datagram ver0Datagram = Utils.getFirstOrNull(configurationItems, Ver0Datagram.class);
      if (ver0Datagram == null) {
         return null;
      }
      Phy0Datagram phy0Datagram = Utils.getFirstOrNull(configurationItems, Phy0Datagram.class);
      if (phy0Datagram == null) {
         return null;
      }
      RawFileConfiguration rawFileConfiguration = Sin0DatagramFactory.createRawFileConfiguration(sin0Datagram, ver0Datagram, phy0Datagram);
      configurationItems.addFirst(rawFileConfiguration);
      return rawFileConfiguration;
   }
}
