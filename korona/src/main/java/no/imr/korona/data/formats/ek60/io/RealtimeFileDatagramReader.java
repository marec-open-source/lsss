package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.tools.Utils;
import no.imr.tools.io.FileInfoComparator;

import java.io.IOException;
import java.nio.file.Path;

public final class RealtimeFileDatagramReader extends FileDatagramReader {
   private static final long WAIT_MILLIS = Utils.parseInt(System.getProperty("RealtimeFileDatagramReader.WAIT_MILLIS"), 1000);

   private long allowedSize;
   private long observedSize;
   private long timeOfObservedSize;
   private long timeOfObservedSizeChange;

   public RealtimeFileDatagramReader(Path file, DatagramTypeManager datagramTypeManager) throws IOException {
      super(file, datagramTypeManager);

      observeSize();
   }

   private void observeSize() throws IOException {
      long previousObserverSize = observedSize;
      observedSize = getSize();
      timeOfObservedSize = System.currentTimeMillis();

      if (observedSize > previousObserverSize) {
         timeOfObservedSizeChange = timeOfObservedSize;
      }
   }

   @Override
   void readBytesToBuffer() throws IOException {
      waitUntilReady();

      super.readBytesToBuffer();
   }

   private void waitUntilReady() throws IOException {
      while (true) {
         if (System.currentTimeMillis() - timeOfObservedSize >= WAIT_MILLIS) {
            allowedSize = observedSize;
            observeSize();
         }

         if (getFileChannel().position() + getReadBuffer().remaining() <= allowedSize) {
            return;
         }

         if (allowedSize < observedSize) {
            long timeToWait = WAIT_MILLIS - (System.currentTimeMillis() - timeOfObservedSize);
            if (timeToWait > 0) {
               Utils.sleep(timeToWait);
            }
            continue;
         }

         long millisWaiting = System.currentTimeMillis() - timeOfObservedSizeChange;
         if (getEndOfInputHandler().isEndOfInput(millisWaiting, FileInfoComparator.path())) {
            return;
         }
      }
   }
}
