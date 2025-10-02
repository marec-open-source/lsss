package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.formats.ek60.io.ByteBufferDatagramReader;
import no.imr.korona.data.formats.ek60.io.ByteBufferDatagramWriter;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.tools.misc.test.UniqueTmpDir;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class EK60SegmentTest {
   @Test
   void segmentInfo() throws IOException {
      ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[10000])
            .order(ByteOrder.LITTLE_ENDIAN);
      try (ByteBufferDatagramWriter datagramWriter = new ByteBufferDatagramWriter(byteBuffer)) {
         SyntheticData syntheticData = new ConstantSyntheticData();
         for (BaseDatagram datagram : syntheticData.getRawFileConfiguration().toDatagrams()) {
            datagramWriter.writeDatagram(datagram);
         }
         long ntDate = syntheticData.getRawFileConfiguration().getNTDate();
         datagramWriter.writeDatagram(new Idx0Datagram(ntDate + 1, 1, 0, null, 1));
         datagramWriter.writeDatagram(new Idx0Datagram(ntDate + 2, 2, 0, null, 2));
         datagramWriter.writeDatagram(new Idx0Datagram(ntDate + 3, 3, 0, null, 3));
      }

      Path idxFile = UniqueTmpDir.newSubDir("EK60SegmentTest").resolve("test.idx");
      int size = byteBuffer.position();
      byteBuffer.flip();

      DatagramTypeManager datagramTypeManager = new DatagramTypeManager();
      try (ByteBufferDatagramReader datagramReader = new ByteBufferDatagramReader(byteBuffer, datagramTypeManager)) {
         SegmentInfo segmentInfo = EK60Utils.createSegmentInfo(datagramReader, idxFile, datagramTypeManager);
         assertEquals(4, segmentInfo.getPingRange().end().getPingNumber());
      }

      for (int i = 1; i < Idx0Datagram.getSize(); i++) {
         byteBuffer.position(0);
         byteBuffer.limit(size - i);
         try (ByteBufferDatagramReader datagramReader = new ByteBufferDatagramReader(byteBuffer, datagramTypeManager)) {
            SegmentInfo segmentInfo = EK60Utils.createSegmentInfo(datagramReader, idxFile, datagramTypeManager);
            assertEquals(3, segmentInfo.getPingRange().end().getPingNumber());
         }
      }
   }
}
