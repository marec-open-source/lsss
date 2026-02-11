package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.DataException;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.DatagramType;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.formats.ek60.io.BaseDatagramReader;
import no.imr.korona.data.formats.ek60.io.ByteBufferDatagramReader;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.formats.ek60.io.EndOfInputHandler;
import no.imr.korona.data.formats.ek60.io.FileDatagramReader;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The EK60 raw file.
 */
final class RawFile {
   private final Path file;
   private final DatagramTypeManager datagramTypeManager;
   private final FileChannel fileChannel;
   private final PingConfiguration pingConfiguration;

   RawFile(Path file, EndOfInputHandler endOfInputHandler, DatagramTypeManager datagramTypeManager) throws IOException {
      this.file = file;
      this.datagramTypeManager = datagramTypeManager;
      FileDatagramReader fileDatagramReader = new FileDatagramReader(file, datagramTypeManager);
      fileDatagramReader.setEndOfInputHandler(endOfInputHandler);
      fileChannel = fileDatagramReader.getFileChannel();
      pingConfiguration = PingConfigurationReader.read(fileDatagramReader);
   }

   Path getFile() {
      return file;
   }

   PingConfiguration getPingConfiguration() {
      return pingConfiguration;
   }

   void close() throws IOException {
      fileChannel.close();
   }

   long size() throws IOException {
      return fileChannel.size();
   }

   List<BaseDatagram> loadDatagrams(long startOffset, long endOffset, Predicate<DatagramType> readPredicate, AsyncHandle asyncHandle) throws IOException {
      if (startOffset < 0) {
         throw new DataException("Start offset: " + startOffset + " in " + file);
      }
      int size = (int) (endOffset - startOffset);
      if (size < 0) {
         throw new DataException("Ping size: " + size + " at offset " + startOffset + " in " + file);
      }
      ByteBuffer byteBuffer = ByteBufferUtils.getThreadLocalByteBuffer(size);
      try {
         FileUtils.read(fileChannel, byteBuffer, startOffset);
      } catch (ClosedChannelException _) {
         byteBuffer.position(0);
         // Reading cancelled
         return List.of();
      }
      byteBuffer.flip();

      try (BaseDatagramReader datagramReader = new ByteBufferDatagramReader(byteBuffer, datagramTypeManager)) {
         datagramReader.setReadPredicate(readPredicate);
         datagramReader.setAcceptPredicate(new PingConfigurationAcceptPredicate(pingConfiguration));

         List<BaseDatagram> datagrams = new ArrayList<>();
         while (byteBuffer.hasRemaining()) {
            if (asyncHandle.isCancelled()) {
               return List.of();
            }
            BaseDatagram datagram = datagramReader.nextDatagram(asyncHandle);
            if (datagram == null) {
               // End of file (should normally not happen) or cancelled.
               break;
            }
            datagrams.add(datagram);
         }
         return datagrams;
      }
   }

   @Override
   public String toString() {
      return file.toString();
   }
}
