package no.imr.korona.data.formats.ek60.io;

import java.nio.ByteBuffer;

public final class ByteBufferDatagramWriter extends BaseDatagramWriter {
   private final ByteBuffer byteBuffer;

   public ByteBufferDatagramWriter(ByteBuffer byteBuffer) {
      this.byteBuffer = byteBuffer;
   }

   @Override
   protected void writeBuffer(ByteBuffer byteBuffer) {
      this.byteBuffer.put(byteBuffer);
   }

   @Override
   public void close() {
   }
}
