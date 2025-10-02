package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;

public abstract class BaseDepPerChannelDatagram extends BaseDepDatagram implements PerChannelDatagram {
   protected BaseDepPerChannelDatagram(long ntDate) {
      super(ntDate);
   }

   protected BaseDepPerChannelDatagram(long ntDate, ByteBuffer byteBuffer) {
      super(ntDate, byteBuffer);
   }
}
