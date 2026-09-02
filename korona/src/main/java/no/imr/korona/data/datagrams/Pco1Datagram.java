package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;
import java.time.Instant;

public final class Pco1Datagram extends Pco0Datagram {
   public static final DatagramType TYPE_PCO1 = DatagramType.simple("PCO1", Pco1Datagram::new);

   public Pco1Datagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant, byteBuffer);
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE_PCO1;
   }
}
