package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.formats.ek60.io.ByteBufferDatagramReader;
import no.imr.korona.data.formats.ek60.io.RandomAccessDatagramReader;
import no.imr.korona.data.formats.missing.MissingBot0Datagram;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The EK60 bottom file.
 */
final class BotFile {
   private final Path file;
   private final List<Bot0Datagram> bot0Datagrams;

   BotFile(Path file, IdxFile idxFile, DatagramTypeManager datagramTypeManager, NoticeHandler noticeHandler) throws IOException {
      this.file = file;
      bot0Datagrams = new ArrayList<>(idxFile.getIdx0Datagrams().size());

      try {
         readBotFile(idxFile, datagramTypeManager, noticeHandler);
      } catch (IOException e) {
         if (FileUtils.notExists(e, file)) {
            noticeHandler.addNotice("Missing bot file " + file);
            addMissingBot0Datagrams(idxFile);
         } else {
            throw e;
         }
      }
   }

   private void readBotFile(IdxFile idxFile, DatagramTypeManager datagramTypeManager, NoticeHandler noticeHandler) throws IOException {
      try (RandomAccessDatagramReader datagramReader = new ByteBufferDatagramReader(FileUtils.toByteBuffer(file, ByteOrder.LITTLE_ENDIAN), datagramTypeManager)) {
         int unexpectedDatagramCount = 0;
         while (bot0Datagrams.size() < idxFile.getIdx0Datagrams().size()) {
            BaseDatagram datagram = datagramReader.nextDatagram();
            if (datagram == null) {
               break;
            } else if (datagram instanceof Bot0Datagram bot0Datagram) {
               bot0Datagrams.add(bot0Datagram);
            } else {
               if (!bot0Datagrams.isEmpty()) {
                  unexpectedDatagramCount++;
               }
            }
         }
         if (unexpectedDatagramCount != 0) {
            noticeHandler.addNotice("Read " + unexpectedDatagramCount + " unexpected datagrams");
         }

         int missingCount = addMissingBot0Datagrams(idxFile);
         if (missingCount != 0) {
            noticeHandler.addNotice("Added " + missingCount + " missing BOT0 datagrams");
         }
      }
   }

   private int addMissingBot0Datagrams(IdxFile idxFile) {
      int missingCount = 0;
      for (int i = bot0Datagrams.size(); i < idxFile.getIdx0Datagrams().size(); i++) {
         bot0Datagrams.add(new MissingBot0Datagram(idxFile.getRawFileConfiguration(), idxFile.getIdx0Datagrams().get(i)));
         missingCount++;
      }
      return missingCount;
   }

   Path getFile() {
      return file;
   }

   List<Bot0Datagram> getBot0Datagrams() {
      return bot0Datagrams;
   }

   @Override
   public String toString() {
      return file.toString();
   }
}
