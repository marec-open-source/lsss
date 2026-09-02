package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;
import java.time.Instant;

public final class Cat0Datagram extends Cas0Datagram {
   public Cat0Datagram(Instant instant, int categoryCount, int regionId) {
      super(instant, categoryCount, regionId);
   }

   public Cat0Datagram(Instant instant, ByteBuffer byteBuffer) {
      super(instant, byteBuffer);
   }

   @Override
   public DatagramType getDatagramType() {
      return KoronaDatagramPlugin.CAT0;
   }
}
