package no.imr.korona.data.formats.ek60;

import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Cds0Datagram;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.datagrams.Xml0Datagram;
import no.imr.korona.data.formats.ek60.io.FileDatagramWriter;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes datagrams to file.
 */
public final class EK60Writer implements Closeable {
   private final FileDatagramWriter rawWriter;
   private final @Nullable FileDatagramWriter idxWriter;
   private final @Nullable FileDatagramWriter botWriter;

   public EK60Writer(Path directory, String rawFileName, PingConfiguration pingConfiguration) throws IOException {
      this(directory, rawFileName, pingConfiguration, "", Mode.ALL_FILES);
   }

   public EK60Writer(Path directory, String rawFileName, PingConfiguration pingConfiguration, String extraSuffix, Mode mode) throws IOException {
      FileUtils.createDirectories(directory);

      Path rawFile = directory.resolve(rawFileName + extraSuffix);
      Log.global.info("Opening " + rawFile);
      rawWriter = new FileDatagramWriter(rawFile);

      for (PingItem pingItem : pingConfiguration.getConfigurationItems()) {
         for (BaseDatagram datagram : pingItem.toDatagrams()) {
            rawWriter.writeDatagram(datagram);
         }
      }
      rawWriter.writeDatagram(new Xml0Datagram(pingConfiguration.getRawFileConfiguration().getNTDate(), XmlUtils.toDocument(KoronaUtils.createProcessingInfoXml())));

      if (mode == Mode.ALL_FILES) {
         String baseName = FileUtils.baseName(rawFileName);
         idxWriter = new FileDatagramWriter(directory.resolve(baseName + EK60DataFormatPlugin.IDX_SUFFIX + extraSuffix));
         botWriter = new FileDatagramWriter(directory.resolve(baseName + EK60DataFormatPlugin.BOT_SUFFIX + extraSuffix));

         List<BaseDatagram> datagrams = pingConfiguration.getRawFileConfiguration().toDatagrams();
         idxWriter.writeDatagrams(datagrams);
         botWriter.writeDatagrams(datagrams);
      } else {
         idxWriter = null;
         botWriter = null;
      }
   }

   public void writeModuleConfiguration(PingConfiguration pingConfiguration, ModuleContainer moduleContainer) throws IOException {
      rawWriter.writeDatagram(new Cds0Datagram(pingConfiguration.getRawFileConfiguration().getNTDate(), XmlUtils.toDocument(moduleContainer.toXml())));
   }

   public void write(Ping ping) throws IOException {
      long fileOffset = rawWriter.getFileChannel().position();
      for (PingItem pingItem : ping.getPingItems()) {
         for (BaseDatagram datagram : pingItem.toDatagrams()) {
            rawWriter.writeDatagram(datagram);
         }
      }
      if (idxWriter != null) {
         idxWriter.writeDatagram(new Idx0Datagram(ping.getPingIndex(), fileOffset));
      }
      if (botWriter != null) {
         botWriter.writeDatagram(ping.getBot0Datagram());
      }
   }

   public void flush() throws IOException {
      flush(rawWriter);
      flush(idxWriter);
      flush(botWriter);
   }

   private static void flush(@Nullable FileDatagramWriter fileDatagramWriter) throws IOException {
      if (fileDatagramWriter != null) {
         fileDatagramWriter.flush();
      }
   }

   @Override
   public void close() throws IOException {
      if (rawWriter.isOpen()) {
         Log.global.info("Closing " + rawWriter.getFile());
      }
      close(rawWriter);
      close(idxWriter);
      close(botWriter);
   }

   private static void close(@Nullable FileDatagramWriter fileDatagramWriter) throws IOException {
      if (fileDatagramWriter != null) {
         fileDatagramWriter.close();
      }
   }

   public enum Mode {
      ALL_FILES, ONLY_RAW
   }
}
