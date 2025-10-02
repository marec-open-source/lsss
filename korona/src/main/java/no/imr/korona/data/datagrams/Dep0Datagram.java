package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;

/**
 * Datagram for depth data.
 */
public final class Dep0Datagram extends BaseDepDatagram {
   public static final DatagramType TYPE = new DatagramType.Simple("DEP0", Dep0Datagram::new);

   public Dep0Datagram(long ntDate) {
      super(ntDate);
   }

   public Dep0Datagram(long ntDate, ByteBuffer byteBuffer) {
      super(ntDate, byteBuffer);
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }
}
