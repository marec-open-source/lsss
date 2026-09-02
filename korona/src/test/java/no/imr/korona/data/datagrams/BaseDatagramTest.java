package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.PingItem;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class BaseDatagramTest {
   @Test
   void ticket31() {
      BaseDatagram datagram = new DummyDatagram(Instant.EPOCH);
      assertEquals(datagram.toByteBufferIncludingHeader(), datagram.makeCopy().toByteBufferIncludingHeader());
   }

   @Test
   void copy() {
      UnknownDatagram datagram = new UnknownDatagram(Instant.EPOCH, UnknownDatagram.type(1), ByteBuffer.wrap(new byte[]{3, 4, 5}));
      UnknownDatagram copy = PingItem.copy(datagram);
      assertEquals(datagram.getInstant(), copy.getInstant());
      assertEquals(datagram.getDatagramType(), copy.getDatagramType());
   }

   private static final class DummyDatagram extends DatagramPingItem {
      private static final DatagramType TYPE = DatagramType.simple("TEST", DummyDatagram::new);

      private DummyDatagram(Instant instant) {
         super(instant);
      }

      private DummyDatagram(Instant instant, ByteBuffer byteBuffer) {
         super(instant);
      }

      @Override
      public void write(ByteBuffer byteBuffer) {
         byteBuffer.position(ByteBufferUtils.INITIAL_CAPACITY);
         byteBuffer.putInt(0);
      }

      @Override
      public DatagramType getDatagramType() {
         return TYPE;
      }
   }
}
