package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;

public final class Pco1Datagram extends Pco0Datagram {
   public static final DatagramType TYPE_PCO1 = new DatagramType.Simple("PCO1", Pco1Datagram::new);

   public Pco1Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate, byteBuffer);
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE_PCO1;
   }
}
