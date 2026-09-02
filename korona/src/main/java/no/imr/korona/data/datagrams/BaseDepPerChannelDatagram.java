package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;
import java.time.Instant;

public abstract class BaseDepPerChannelDatagram extends BaseDepDatagram implements PerChannelDatagram {
   protected BaseDepPerChannelDatagram(Instant instant) {
      super(instant);
   }

   protected BaseDepPerChannelDatagram(Instant instant, ByteBuffer byteBuffer) {
      super(instant, byteBuffer);
   }
}
