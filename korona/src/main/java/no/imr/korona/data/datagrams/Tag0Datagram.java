package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.time.Instant;

public final class Tag0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("TAG0", Tag0Datagram::new);

   private final String annotation;

   public Tag0Datagram(Instant instant, String annotation) {
      super(instant);

      this.annotation = annotation;
   }

   public Tag0Datagram(Instant instant, ByteBuffer byteBuffer) {
      super(instant);

      annotation = ByteBufferUtils.readCString(byteBuffer, byteBuffer.remaining());
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeCString(byteBuffer, annotation);
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public String toStringExtra() {
      return annotation;
   }

   public String getAnnotation() {
      return annotation;
   }
}
