package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;

public final class Pin1Datagram extends Pin0Datagram {
   public static final DatagramType TYPE_PIN1 = new DatagramType.Simple("PIN1", Pin1Datagram::new);

   public Pin1Datagram(long ntDate, ByteBuffer byteBuffer) {
      super(ntDate, byteBuffer);
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE_PIN1;
   }
}
