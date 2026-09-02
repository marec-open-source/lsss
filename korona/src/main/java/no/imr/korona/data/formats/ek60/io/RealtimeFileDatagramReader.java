package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.tools.io.FileInfoComparator;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public final class RealtimeFileDatagramReader extends FileDatagramReader {
   private static final Duration WAIT_DURATION = Duration.ofMillis(Integer.parseInt(System.getProperty("RealtimeFileDatagramReader.WAIT_MILLIS", "1000")));

   private long allowedSize;
   private long observedSize;
   private Instant timeOfObservedSize;
   private Instant timeOfObservedSizeChange;

   public RealtimeFileDatagramReader(Path file, DatagramTypeManager datagramTypeManager) throws IOException {
      super(file, datagramTypeManager);

      observedSize = getSize();
      timeOfObservedSize = Instant.now();
      timeOfObservedSizeChange = timeOfObservedSize;
   }

   @Override
   void readBytesToBuffer() throws IOException {
      try {
         waitUntilReady();
      } catch (InterruptedException _) {
         Thread.currentThread().interrupt();
      }

      super.readBytesToBuffer();
   }

   private void waitUntilReady() throws IOException, InterruptedException {
      while (true) {
         if (getFileChannel().position() + getReadBuffer().remaining() <= allowedSize) {
            return;
         }

         Instant timeOfNextObservation = timeOfObservedSize.plus(WAIT_DURATION);
         Thread.sleep(Instant.now().until(timeOfNextObservation));

         allowedSize = observedSize;
         observedSize = getSize();
         timeOfObservedSize = Instant.now();
         if (observedSize > allowedSize) {
            timeOfObservedSizeChange = timeOfObservedSize;
            continue;
         }

         Duration durationWaiting = timeOfObservedSizeChange.until(Instant.now());
         if (getEndOfInputHandler().isEndOfInput(durationWaiting, FileInfoComparator.path())) {
            return;
         }
      }
   }
}
