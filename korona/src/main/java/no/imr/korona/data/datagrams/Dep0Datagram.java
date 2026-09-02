package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * Datagram for depth data.
 */
public final class Dep0Datagram extends BaseDepDatagram {
   public static final DatagramType TYPE = DatagramType.simple("DEP0", Dep0Datagram::new);

   public Dep0Datagram(Instant instant) {
      super(instant);
   }

   public Dep0Datagram(Instant instant, ByteBuffer byteBuffer) {
      super(instant, byteBuffer);
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }
}
