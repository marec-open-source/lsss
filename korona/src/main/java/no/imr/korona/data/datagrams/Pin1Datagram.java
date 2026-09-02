package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;
import java.time.Instant;

public final class Pin1Datagram extends Pin0Datagram {
   public static final DatagramType TYPE_PIN1 = DatagramType.simple("PIN1", Pin1Datagram::new);

   public Pin1Datagram(Instant instant, ByteBuffer byteBuffer) {
      super(instant, byteBuffer);
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE_PIN1;
   }
}
