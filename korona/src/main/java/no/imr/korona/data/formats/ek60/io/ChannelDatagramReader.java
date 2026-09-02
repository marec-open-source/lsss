package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.tools.io.FileInfoComparator;
import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.time.Duration;
import java.time.Instant;

/**
 * Reads datagrams from a {@link ReadableByteChannel}.
 */
public abstract class ChannelDatagramReader extends BaseDatagramReader {
   private final ReadableByteChannel readChannel;
   private ByteBuffer readBuffer;
   private boolean endOfData;
   private EndOfInputHandler endOfInputHandler = EndOfInputHandler.noWait();

   protected ChannelDatagramReader(ReadableByteChannel readChannel, DatagramTypeManager datagramTypeManager) {
      super(datagramTypeManager);

      this.readChannel = readChannel;
      readBuffer = ByteBufferUtils.allocate(ByteBufferUtils.INITIAL_CAPACITY);
      readBuffer.limit(0);
   }

   @Override
   public void close() throws IOException {
      readChannel.close();
   }

   @Override
   protected ByteBuffer getReadBuffer() {
      return readBuffer;
   }

   protected void discardBufferContents() {
      readBuffer.limit(0); // also sets position to 0
   }

   @Override
   protected final void skip(int bytesToSkip) throws IOException {
      int remaining = readBuffer.remaining();
      if (bytesToSkip > remaining) {
         skipBytesFromChannel(bytesToSkip - remaining);
         readBuffer.position(readBuffer.position() + remaining);
      } else {
         readBuffer.position(readBuffer.position() + bytesToSkip);
      }
   }

   /**
    * Skips bytes from the underlying channel.
    *
    * @param bytesToSkip number of bytes to skip
    * @throws BufferUnderflowException if not enough bytes remain to skip
    * @throws IOException              if some IO error occurs
    */
   protected abstract void skipBytesFromChannel(int bytesToSkip) throws IOException;

   EndOfInputHandler getEndOfInputHandler() {
      return endOfInputHandler;
   }

   public void setEndOfInputHandler(EndOfInputHandler endOfInputHandler) {
      this.endOfInputHandler = endOfInputHandler;
   }

   @Override
   protected void readToEnsureRemaining(int bytesToRemain) throws IOException {
      if (bytesToRemain > readBuffer.capacity()) {
         // Buffer to not large enough, must reallocate
         ByteBuffer newReadBuffer = ByteBufferUtils.allocate(bytesToRemain);
         newReadBuffer.limit(readBuffer.remaining());
         newReadBuffer.put(readBuffer);
         newReadBuffer.position(0);
         readBuffer = newReadBuffer;
      } else {
         // Buffer is large enough
         if (readBuffer.hasRemaining()) {
            // Must preserve remaining data
            if (readBuffer.capacity() - readBuffer.position() < bytesToRemain) {
               // Not enough space after current position
               readBuffer.compact();
               readBuffer.limit(readBuffer.position());
               readBuffer.position(0);
            } else {
               // Enough space after current position
            }
         } else {
            // No data to preserve so jump to beginning
            readBuffer.position(0);
            readBuffer.limit(0);
         }
      }

      int position = readBuffer.position();
      int limit = readBuffer.limit();

      readBuffer.limit(position + bytesToRemain);
      readBuffer.position(limit);

      readBytesToBuffer();

      readBuffer.limit(readBuffer.position());
      readBuffer.position(position);
   }

   void readBytesToBuffer() throws IOException {
      FileUtils.read(readChannel, readBuffer);

      if (readBuffer.hasRemaining()) {
         if (endOfData) {
            return;
         }

         Instant lastReadTime = Instant.now();
         while (readBuffer.hasRemaining()) {
            Duration durationWaiting = lastReadTime.until(Instant.now());
            if (endOfInputHandler.isEndOfInput(durationWaiting, FileInfoComparator.path())) {
               endOfData = true;
               return;
            }
            if (FileUtils.read(readChannel, readBuffer) > 0) {
               lastReadTime = Instant.now();
            }
         }
      }

      endOfData = false;
   }
}
