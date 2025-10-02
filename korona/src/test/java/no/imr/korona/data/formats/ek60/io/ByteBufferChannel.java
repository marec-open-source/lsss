package no.imr.korona.data.formats.ek60.io;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Channel reading from a ByteBuffer.
 */
final class ByteBufferChannel implements ReadableByteChannel {
   private final ByteBuffer byteBuffer;

   ByteBufferChannel(ByteBuffer byteBuffer) {
      this.byteBuffer = byteBuffer;
   }

   @Override
   public int read(ByteBuffer dst) {
      int position = byteBuffer.position();
      byteBuffer.limit(Math.min(byteBuffer.capacity(), position + dst.remaining()));
      dst.put(byteBuffer);
      int bytesRead = byteBuffer.position() - position;
      return bytesRead == 0 ? -1 : bytesRead;
   }

   @Override
   public boolean isOpen() {
      return true;
   }

   @Override
   public void close() {
   }
}
