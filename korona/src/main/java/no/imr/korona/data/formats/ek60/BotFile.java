package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.formats.ek60.io.ByteBufferDatagramReader;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.formats.ek60.io.RandomAccessDatagramReader;
import no.imr.korona.data.formats.missing.MissingBot0Datagram;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The EK60 bottom file.
 */
record BotFile(
      Path file,
      List<Bot0Datagram> bot0Datagrams
) {
   @Override
   public String toString() {
      return file.toString();
   }

   static BotFile load(Path file, IdxFile idxFile, DatagramTypeManager datagramTypeManager, NoticeHandler noticeHandler) throws IOException {
      List<Bot0Datagram> bot0Datagrams = new ArrayList<>(idxFile.idx0Datagrams().size());

      try (RandomAccessDatagramReader datagramReader = new ByteBufferDatagramReader(ByteBufferUtils.toByteBuffer(file), datagramTypeManager)) {
         int unexpectedDatagramCount = 0;
         while (bot0Datagrams.size() < idxFile.idx0Datagrams().size()) {
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

         int missingCount = addMissingBot0Datagrams(bot0Datagrams, idxFile);
         if (missingCount != 0) {
            noticeHandler.addNotice("Added " + missingCount + " missing BOT0 datagrams");
         }
      } catch (IOException e) {
         if (FileUtils.notExists(e, file)) {
            noticeHandler.addNotice("Missing bot file " + file);
            addMissingBot0Datagrams(bot0Datagrams, idxFile);
         } else {
            throw e;
         }
      }
      return new BotFile(file, List.copyOf(bot0Datagrams));
   }

   private static int addMissingBot0Datagrams(List<Bot0Datagram> bot0Datagrams, IdxFile idxFile) {
      int missingCount = 0;
      for (int i = bot0Datagrams.size(); i < idxFile.idx0Datagrams().size(); i++) {
         bot0Datagrams.add(new MissingBot0Datagram(idxFile.rawFileConfiguration(), idxFile.idx0Datagrams().get(i)));
         missingCount++;
      }
      return missingCount;
   }
}
