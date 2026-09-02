package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * End of ping.
 */
public final class Eop0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("EOP0", Eop0Datagram::new);

   public Eop0Datagram(Instant instant, ByteBuffer byteBuffer) {
      super(instant);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }
}
