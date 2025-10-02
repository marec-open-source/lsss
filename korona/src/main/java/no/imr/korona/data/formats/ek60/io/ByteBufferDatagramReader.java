package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.DatagramTypeManager;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public final class ByteBufferDatagramReader extends BaseDatagramReader implements RandomAccessDatagramReader {
   private final ByteBuffer byteBuffer;
   private final int size;

   public ByteBufferDatagramReader(ByteBuffer byteBuffer, DatagramTypeManager datagramTypeManager) {
      super(datagramTypeManager);

      this.byteBuffer = byteBuffer;
      size = byteBuffer.limit();
   }

   @Override
   protected ByteBuffer getReadBuffer() {
      return byteBuffer;
   }

   @Override
   protected void skip(int bytesToSkip) {
      long p = getPosition() + bytesToSkip;
      if (p > getSize()) {
         throw new BufferUnderflowException();
      }
      setPosition(p);
   }

   @Override
   protected void readToEnsureRemaining(int bytesToRemain) {
   }

   @Override
   public long getSize() {
      return size;
   }

   @Override
   public long getPosition() {
      return byteBuffer.position();
   }

   @Override
   public void setPosition(long position) {
      byteBuffer.position((int) position);
   }

   @Override
   public void close() {
   }
}
