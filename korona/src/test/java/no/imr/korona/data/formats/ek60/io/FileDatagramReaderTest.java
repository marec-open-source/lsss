package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.datagrams.Raw0Datagram;
import no.imr.korona.data.datagrams.UnknownDatagram;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class FileDatagramReaderTest {
   /**
    * This test failed before
    * {@link ChannelDatagramReader#discardBufferContents()}
    * was called from
    * {@link FileDatagramReader#setPosition(long)}.
    *
    * @throws IOException should not happen
    */
   @Test
   void jumpAfterSearch() throws IOException {
      ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[10000])
            .order(ByteOrder.LITTLE_ENDIAN);

      int posIdxWithPingNumber1;
      int size;

      try (BaseDatagramWriter datagramWriter = new ByteBufferDatagramWriter(byteBuffer)) {

         UnknownDatagram invalidRaw = new UnknownDatagram(Instant.ofEpochSecond(0), UnknownDatagram.type(Raw0Datagram.TYPE.getIntCode()), ByteBuffer.allocate(1000));
         datagramWriter.writeDatagram(invalidRaw);
         posIdxWithPingNumber1 = byteBuffer.position();
         datagramWriter.writeDatagram(new Idx0Datagram(Instant.ofEpochSecond(1), 1, 1, null, 1));

         size = byteBuffer.position();

         // Write two small valid datagrams within the invalid raw
         byteBuffer.position(500);
         datagramWriter.writeDatagram(new Idx0Datagram(Instant.ofEpochSecond(2), 2, 2, null, 2));
         datagramWriter.writeDatagram(new Idx0Datagram(Instant.ofEpochSecond(3), 3, 3, null, 3));
      }

      byteBuffer.position(0);
      byteBuffer.limit(size);

      try (RandomAccessDatagramReader datagramReader = new ByteBufferDatagramReader(byteBuffer, new DatagramTypeManager())) {
         Idx0Datagram idx = (Idx0Datagram) datagramReader.nextDatagram();
         assertNotNull(idx);
         assertEquals(2, idx.getPingNumber());

         datagramReader.setPosition(posIdxWithPingNumber1);
         idx = (Idx0Datagram) datagramReader.nextDatagram();
         assertNotNull(idx);
         assertEquals(1, idx.getPingNumber());

         idx = (Idx0Datagram) datagramReader.nextDatagram();
         assertNull(idx);
      }
   }
}
