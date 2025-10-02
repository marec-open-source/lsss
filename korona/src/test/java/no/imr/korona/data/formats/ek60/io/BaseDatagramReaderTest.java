package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.datagrams.UnknownDatagram;
import no.imr.korona.data.datagrams.UnknownDatagramType;
import no.imr.tools.time.NTDate;
import no.marec.lsss.api.util.GeoPoint;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

final class BaseDatagramReaderTest {
   private static final long NT_DATE = 0;

   @Test
   void maxNTDate() {
      long millis = LocalDate.of(9999, Month.DECEMBER, 31).atStartOfDay(ZoneOffset.UTC).toEpochSecond() * 1000;
      long maxNTDate = NTDate.timeInMillisToNTDate(millis);
      assertEquals(BaseDatagramReader.MAX_NT_DATE, maxNTDate);
   }

   @Test
   void junk() throws IOException {
      ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[4 * 4 + 2 * Idx0Datagram.getSize()])
            .order(ByteOrder.LITTLE_ENDIAN);

      Idx0Datagram idx0Datagram = new Idx0Datagram(NT_DATE, 56, 98, new GeoPoint(34, 45), 999);

      byteBuffer.putInt(-1); // Junk

      byteBuffer.putInt(Idx0Datagram.getSize() - 8);
      idx0Datagram.writeIncludingHeader(byteBuffer);
      byteBuffer.putInt(Idx0Datagram.getSize() - 8);

      byteBuffer.putInt(-1); // Junk

      idx0Datagram.setNTDate(NT_DATE + 1);
      byteBuffer.putInt(Idx0Datagram.getSize() - 8);
      idx0Datagram.writeIncludingHeader(byteBuffer);
      byteBuffer.putInt(Idx0Datagram.getSize() - 8);

      byteBuffer.putInt(1000); // Junk
      byteBuffer.putInt(-1); // Junk

      assertEquals(byteBuffer.capacity(), byteBuffer.position());

      byteBuffer.rewind();

      try (BaseDatagramReader datagramReader = new ByteBufferDatagramReader(byteBuffer, new DatagramTypeManager())) {
         assertEquals(0L, datagramReader.getTotalRead());
         assertEquals(0L, datagramReader.getBytesSkipped());

         BaseDatagram datagram = datagramReader.nextDatagram();
         assertNotNull(datagram);
         assertEquals(Idx0Datagram.TYPE, datagram.getDatagramType());
         assertEquals(NT_DATE, datagram.getNTDate());
         assertEquals(Idx0Datagram.getSize() + 4L, datagramReader.getTotalRead());
         assertEquals(4L, datagramReader.getBytesSkipped());

         datagram = datagramReader.nextDatagram();
         assertNotNull(datagram);
         assertEquals(Idx0Datagram.TYPE, datagram.getDatagramType());
         assertEquals(NT_DATE + 1, datagram.getNTDate());
         assertEquals(2L * Idx0Datagram.getSize() + 8L, datagramReader.getTotalRead());
         assertEquals(8L, datagramReader.getBytesSkipped());

         datagram = datagramReader.nextDatagram();
         assertNull(datagram);
         assertEquals(2L * Idx0Datagram.getSize() + 16L, datagramReader.getTotalRead());
         assertEquals(16L, datagramReader.getBytesSkipped());
      }
   }

   @Test
   void truncated() throws IOException {
      ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[Idx0Datagram.getSize() - 4])
            .order(ByteOrder.LITTLE_ENDIAN);

      Idx0Datagram idx0Datagram = new Idx0Datagram(NT_DATE, 56, 98, new GeoPoint(34, 45), 999);

      byteBuffer.putInt(Idx0Datagram.getSize() - 8);
      idx0Datagram.writeIncludingHeader(byteBuffer);
      // skip this: byteBuffer.putInt(Idx0Datagram.getSize() - 8);

      assertEquals(byteBuffer.capacity(), byteBuffer.position());

      byteBuffer.rewind();

      try (BaseDatagramReader datagramReader = new ByteBufferDatagramReader(byteBuffer, new DatagramTypeManager())) {
         BaseDatagram datagram = datagramReader.nextDatagram();
         assertNull(datagram);
         assertEquals(Idx0Datagram.getSize() - 4L, datagramReader.getTotalRead());
         assertEquals(Idx0Datagram.getSize() - 4L, datagramReader.getBytesSkipped());
      }
   }

   @Test
   void skipSomeDatagrams() throws IOException {
      ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[10000])
            .order(ByteOrder.LITTLE_ENDIAN);
      try (BaseDatagramWriter datagramWriter = new ByteBufferDatagramWriter(byteBuffer)) {
         datagramWriter.writeDatagram(new Idx0Datagram(NT_DATE + 1, 1, 1, null, 1));
         datagramWriter.writeDatagram(new UnknownDatagram(NT_DATE + 1, new UnknownDatagramType(-1), ByteBuffer.wrap(new byte[2])));
         datagramWriter.writeDatagram(new UnknownDatagram(NT_DATE + 2, new UnknownDatagramType(-1), ByteBuffer.wrap(new byte[3])));
         datagramWriter.writeDatagram(new Idx0Datagram(NT_DATE + 2, 2, 2, null, 2));
         datagramWriter.writeDatagram(new UnknownDatagram(NT_DATE + 3, new UnknownDatagramType(-1), ByteBuffer.wrap(new byte[5])));
         datagramWriter.writeDatagram(new Idx0Datagram(NT_DATE + 3, 3, 3, null, 3));
      }

      byteBuffer.flip();

      try (BaseDatagramReader datagramReader = new ByteBufferDatagramReader(byteBuffer, new DatagramTypeManager())) {
         datagramReader.setReadPredicate(datagramType -> datagramType.equals(Idx0Datagram.TYPE));

         Idx0Datagram idx;
         idx = (Idx0Datagram) datagramReader.nextDatagram();
         assertNotNull(idx);
         assertEquals(1, idx.getPingNumber());

         idx = (Idx0Datagram) datagramReader.nextDatagram();
         assertNotNull(idx);
         assertEquals(2, idx.getPingNumber());

         idx = (Idx0Datagram) datagramReader.nextDatagram();
         assertNotNull(idx);
         assertEquals(3, idx.getPingNumber());

         idx = (Idx0Datagram) datagramReader.nextDatagram();
         assertNull(idx);
      }
   }
}
