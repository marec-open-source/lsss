package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

final class ChannelDatagramReaderTest {
   @Test
   void ticket31() throws IOException {
      int contentSize = 2 * ByteBufferUtils.INITIAL_CAPACITY;
      int totalSize = 4 + 8 + contentSize;
      ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[4 + totalSize + 4])
            .order(ByteOrder.LITTLE_ENDIAN);
      byteBuffer.putInt(totalSize); // byte count
      byteBuffer.putInt(Integer.MIN_VALUE); // type
      long ntDate = 0;
      byteBuffer.putLong(ntDate); // NT date
      byteBuffer.position(byteBuffer.position() + contentSize); // contents
      byteBuffer.putInt(totalSize); // byte count
      assertEquals(byteBuffer.capacity(), byteBuffer.position());
      byteBuffer.rewind();

      try (BaseDatagramReader datagramReader = new ByteBufferChannelDatagramReader(byteBuffer, new DatagramTypeManager())) {
         BaseDatagram datagram = datagramReader.nextDatagram();
         assertNotNull(datagram);
         assertEquals(Integer.MIN_VALUE, datagram.getDatagramType().getIntCode());
         assertEquals(ntDate, datagram.getNTDate());
         assertEquals(0, byteBuffer.remaining());
      }
   }

   @Test
   void skip() throws IOException {
      int totalSize = 4 + 8;
      ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[3 + 4 + totalSize + 4])
            .order(ByteOrder.LITTLE_ENDIAN);
      byteBuffer.put((byte) 0); // to be skipped
      byteBuffer.put((byte) 0); // to be skipped
      byteBuffer.put((byte) 0); // to be skipped
      byteBuffer.putInt(totalSize); // byte count
      byteBuffer.putInt(Integer.MIN_VALUE); // type
      long ntDate = 0;
      byteBuffer.putLong(ntDate); // NT date
      byteBuffer.putInt(totalSize); // byte count
      assertEquals(byteBuffer.capacity(), byteBuffer.position());
      byteBuffer.rewind();

      try (BaseDatagramReader datagramReader = new ByteBufferChannelDatagramReader(byteBuffer, new DatagramTypeManager())) {
         datagramReader.skip(2);
         datagramReader.readToEnsureRemaining(2);
         datagramReader.skip(1);
         assertThrows(BufferUnderflowException.class, () -> {
            datagramReader.skip(byteBuffer.capacity());
         });
         BaseDatagram datagram = datagramReader.nextDatagram();
         assertNotNull(datagram);
         assertEquals(Integer.MIN_VALUE, datagram.getDatagramType().getIntCode());
         assertEquals(ntDate, datagram.getNTDate());
         assertEquals(0, byteBuffer.remaining());
      }
   }
}
