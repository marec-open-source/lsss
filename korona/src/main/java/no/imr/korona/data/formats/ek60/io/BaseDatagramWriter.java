package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.BaseDatagram;

import java.io.Closeable;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * Base abstract class for writing datagrams.
 */
public abstract class BaseDatagramWriter implements Closeable {
   private ByteBuffer byteBuffer;
   private long bytesWritten;

   /**
    * Base Constructor.
    */
   protected BaseDatagramWriter() {
      byteBuffer = ByteBufferUtils.allocate(ByteBufferUtils.INITIAL_CAPACITY);
   }

   /**
    * Returns the number of bytes written.
    *
    * @return the number of bytes written
    */
   public long getBytesWritten() {
      return bytesWritten;
   }

   public void writeDatagrams(Iterable<? extends BaseDatagram> datagrams) throws IOException {
      for (BaseDatagram datagram : datagrams) {
         writeDatagram(datagram);
      }
   }

   public void writeDatagram(BaseDatagram datagram) throws IOException {
      while (true) {
         try {
            datagramToBuffer(datagram);
            break;
         } catch (BufferOverflowException e) {
            byteBuffer = ByteBufferUtils.allocate(2 * byteBuffer.capacity());
         }
      }

      writeBuffer(byteBuffer);

      bytesWritten += byteBuffer.position();

      if (byteBuffer.hasRemaining()) {
         throw new IOException("Remaining: " + byteBuffer.remaining());
      }
   }

   private void datagramToBuffer(BaseDatagram datagram) {
      byteBuffer.limit(byteBuffer.capacity());

      // Write datagram to buffer
      byteBuffer.position(4);
      datagram.writeIncludingHeader(byteBuffer);

      // Find number of bytes written by datagram
      int datagramSize = byteBuffer.position() - 4;

      // Set datagram size after datagram
      byteBuffer.putInt(datagramSize);

      // Set datagram size before datagram
      byteBuffer.putInt(0, datagramSize);

      // Make buffer ready for output
      byteBuffer.flip();
   }

   /**
    * Write content of buffer.
    *
    * @param byteBuffer buffer to write from
    * @throws IOException if some IO error occurs
    */
   protected abstract void writeBuffer(ByteBuffer byteBuffer) throws IOException;
}
