package no.imr.korona.data.formats.ek60.io;

import java.nio.ByteBuffer;

final class NullDatagramWriter extends BaseDatagramWriter {
   NullDatagramWriter() {
   }

   @Override
   protected void writeBuffer(ByteBuffer byteBuffer) {
      byteBuffer.position(byteBuffer.limit());
   }

   @Override
   public void close() {
   }
}
