package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;

/**
 * End of ping.
 */
public final class Eop0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("EOP0", Eop0Datagram::new);

   public Eop0Datagram(long ntDate, ByteBuffer byteBuffer) {
      super(ntDate);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }
}
