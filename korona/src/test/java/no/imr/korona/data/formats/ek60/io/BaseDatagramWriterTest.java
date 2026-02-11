package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.UnknownDatagram;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

final class BaseDatagramWriterTest {
   @Test
   void ticket31() throws IOException {
      try (BaseDatagramWriter datagramWriter = new NullDatagramWriter()) {
         long expectedBytesWritten = 0;
         int n = ByteBufferUtils.INITIAL_CAPACITY + 1;

         datagramWriter.writeDatagram(createUnknownDatagram(n));
         expectedBytesWritten += n + BaseDatagram.ENVELOPE_AND_HEADER_SIZE;
         assertEquals(expectedBytesWritten, datagramWriter.getBytesWritten());

         datagramWriter.writeDatagram(createUnknownDatagram(3 * n));
         expectedBytesWritten += 3 * n + BaseDatagram.ENVELOPE_AND_HEADER_SIZE;
         assertEquals(expectedBytesWritten, datagramWriter.getBytesWritten());
      }
   }

   private static UnknownDatagram createUnknownDatagram(int byteCount) {
      return new UnknownDatagram(0, UnknownDatagram.type(0), ByteBuffer.wrap(new byte[byteCount]));
   }
}
