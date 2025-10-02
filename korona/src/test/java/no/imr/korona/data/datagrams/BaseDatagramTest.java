package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.PingItem;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

final class BaseDatagramTest {
   @Test
   void ticket31() {
      BaseDatagram datagram = new DummyDatagram(0);
      assertEquals(datagram.toByteBufferIncludingHeader(), datagram.makeCopy().toByteBufferIncludingHeader());
   }

   @Test
   void copy() {
      UnknownDatagram datagram = new UnknownDatagram(0, new UnknownDatagramType(1), ByteBuffer.wrap(new byte[]{3, 4, 5}));
      UnknownDatagram copy = PingItem.copy(datagram);
      assertEquals(datagram.getNTDate(), copy.getNTDate());
      assertEquals(datagram.getDatagramType(), copy.getDatagramType());
   }

   private static final class DummyDatagram extends DatagramPingItem {
      private static final DatagramType TYPE = new DatagramType.Simple("TEST", DummyDatagram::new);

      private DummyDatagram(long ntDate) {
         super(ntDate);
      }

      private DummyDatagram(long ntDate, ByteBuffer byteBuffer) {
         super(ntDate);
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
