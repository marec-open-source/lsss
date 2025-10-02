package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;

public final class Cat0Datagram extends Cas0Datagram {
   public Cat0Datagram(long ntDate, int categoryCount, int regionId) {
      super(ntDate, categoryCount, regionId);
   }

   public Cat0Datagram(long ntDate, ByteBuffer byteBuffer) {
      super(ntDate, byteBuffer);
   }

   @Override
   public DatagramType getDatagramType() {
      return KoronaDatagramPlugin.CAT0;
   }
}
