package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.DatagramTypeManager;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

final class ByteBufferChannelDatagramReader extends ChannelDatagramReader {
   private final ByteBuffer byteBuffer;

   ByteBufferChannelDatagramReader(ByteBuffer byteBuffer, DatagramTypeManager datagramTypeManager) {
      super(new ByteBufferChannel(byteBuffer), datagramTypeManager);

      this.byteBuffer = byteBuffer;
   }

   @Override
   protected void skipBytesFromChannel(int bytesToSkip) {
      int p = byteBuffer.position() + bytesToSkip;
      if (p > byteBuffer.capacity()) {
         throw new BufferUnderflowException();
      }
      if (p > byteBuffer.limit()) {
         byteBuffer.limit(p);
      }
      byteBuffer.position(p);
   }
}
