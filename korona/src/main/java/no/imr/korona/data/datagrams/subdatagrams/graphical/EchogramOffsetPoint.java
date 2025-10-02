package no.imr.korona.data.datagrams.subdatagrams.graphical;

import java.nio.ByteBuffer;

public record EchogramOffsetPoint(int pingNumberOffset, float depth) {

   EchogramOffsetPoint(ByteBuffer byteBuffer) {
      this(byteBuffer.getInt(),
            byteBuffer.getFloat());
   }

   void write(ByteBuffer byteBuffer) {
      byteBuffer.putInt(pingNumberOffset);
      byteBuffer.putFloat(depth);
   }
}
