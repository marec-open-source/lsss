package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.channel.PowerData;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class Raw3DatagramTest {
   @Test
   void paddingToMultipleOfFour() throws DatagramFormatException {
      // Issue #1500.
      Raw3Datagram raw3Datagram = new Raw3Datagram(Instant.EPOCH);
      raw3Datagram.dataType = PowerData.DATA_TYPE_POWER; // Power only => 2 bytes per sample.
      raw3Datagram.power = new short[]{1, 2, 3};         // Odd number of samples => datagram size not multiple of 4.
      raw3Datagram.count = raw3Datagram.power.length;
      ByteBuffer byteBuffer = ByteBufferUtils.allocate(1000);
      raw3Datagram.write(byteBuffer);

      int datagramSize = 128 + 2 + 2 + 4 + 4 + 3 * 2;
      assertEquals(2, datagramSize % 4);
      assertEquals(datagramSize, byteBuffer.position());

      // Add 2 extra bytes to make multiple of 4:
      byteBuffer.putShort((short) 0);
      assertEquals(0, byteBuffer.position() % 4);

      byteBuffer.flip();
      Raw3Datagram readRaw3Datagram = new Raw3Datagram(Instant.EPOCH, byteBuffer);
      assertArrayEquals(raw3Datagram.power, readRaw3Datagram.power);
      assertEquals(0, byteBuffer.remaining());
   }
}
