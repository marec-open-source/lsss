package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.DatagramSource;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.datagrams.Nme0Datagram;
import no.imr.korona.data.datagrams.UnknownDatagram;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.util.DataUtils;
import no.imr.tools.time.NTDate;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class IdxCorrectionFilterTest {
   @Test
   void testUnexpectedDatagrams() throws IOException {
      List<BaseDatagram> input = List.of(
            new Idx0Datagram(Instant.ofEpochSecond(0), 1, 1.0, null, 0),
            new Nme0Datagram(Instant.ofEpochSecond(0), "dummy"),
            new Idx0Datagram(Instant.ofEpochSecond(1), 2, 1.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(2), 3, 1.0, null, 0),
            new UnknownDatagram(Instant.ofEpochSecond(2), UnknownDatagram.type(1), ByteBuffer.wrap(new byte[0])),
            new Nme0Datagram(Instant.ofEpochSecond(2), "dummy"),
            new Idx0Datagram(Instant.ofEpochSecond(3), 4, 1.0, null, 0)
      );
      List<Idx0Datagram> expected = List.of(
            new Idx0Datagram(Instant.ofEpochSecond(0), 1, 1.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(1), 2, 1.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(2), 3, 1.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(3), 4, 1.0, null, 0)
      );
      test(input, expected, 3, 0, 0, 0, null);
   }

   @Test
   void testPingNumber() throws IOException {
      List<Idx0Datagram> input = List.of(
            new Idx0Datagram(Instant.ofEpochSecond(1), 1, 1.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(2), 2, 1.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(3), 5, 1.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(4), 4, 1.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(5), 9, 1.0, null, 0)
      );
      List<Idx0Datagram> expected = List.of(
            new Idx0Datagram(Instant.ofEpochSecond(1), 1, 1.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(2), 2, 1.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(2), 3, 1.0, null, 0), // added
            new Idx0Datagram(Instant.ofEpochSecond(2), 4, 1.0, null, 0), // added
            new Idx0Datagram(Instant.ofEpochSecond(3), 5, 1.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(3), 6, 1.0, null, 0), // added
            new Idx0Datagram(Instant.ofEpochSecond(3), 7, 1.0, null, 0), // added
            new Idx0Datagram(Instant.ofEpochSecond(3), 8, 1.0, null, 0), // added
            new Idx0Datagram(Instant.ofEpochSecond(5), 9, 1.0, null, 0)
      );
      test(input, expected, 0, 5, 1, 0, null);
   }

   @Test
   void testVesselDistance() throws IOException {
      List<Idx0Datagram> input = List.of(
            new Idx0Datagram(Instant.ofEpochSecond(1), 1, 1.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(2), 2, 2.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(3), 3, 1.5, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(4), 4, 1.9, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(5), 5, 3.0, null, 0)
      );
      List<Idx0Datagram> expected = List.of(
            new Idx0Datagram(Instant.ofEpochSecond(1), 1, 1.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(2), 2, 2.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(3), 3, 2.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(4), 4, 2.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(5), 5, 3.0, null, 0)
      );
      test(input, expected, 0, 0, 0, 0, null);
   }

   @Test
   void testVesselDistanceSpike() throws IOException {
      List<Idx0Datagram> input = List.of(
            new Idx0Datagram(Instant.ofEpochSecond(1), 1, 999.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(2), 2, 999.0, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(3), 3, 1.5, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(4), 4, 1.8, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(5), 5, 1.9, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(6), 6, 989, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(7), 7, 2.1, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(8), 8, 2.4, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(9), 9, 987, null, 0)
      );
      List<Idx0Datagram> expected = List.of(
            new Idx0Datagram(Instant.ofEpochSecond(1), 1, 1.5, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(2), 2, 1.5, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(3), 3, 1.5, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(4), 4, 1.8, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(5), 5, 1.9, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(6), 6, 1.9, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(7), 7, 2.1, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(8), 8, 2.4, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(9), 9, 2.4, null, 0)
      );
      test(input, expected, 0, 0, 0, 4, null);
   }

   @Test
   void testWrapAround() throws IOException {
      List<Idx0Datagram> input = List.of(
            new Idx0Datagram(Instant.ofEpochSecond(1), 1, 9999.90, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(2), 2, 9999.94, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(3), 3, 9999.98, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(4), 4, 0.02, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(5), 5, 0.04, null, 0)
      );
      List<Idx0Datagram> expected = List.of(
            new Idx0Datagram(Instant.ofEpochSecond(1), 1, 9999.90, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(2), 2, 9999.94, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(3), 3, 9999.98, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(4), 4, 10000.02, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(5), 5, 10000.04, null, 0)
      );
      test(input, expected, 0, 0, 0, 0, new WrapAround(input.get(3), 10000.0));
   }

   @Test
   void testWrapAroundNearBeginning() throws IOException {
      List<Idx0Datagram> input = List.of(
            new Idx0Datagram(Instant.ofEpochSecond(2), 2, 9999.96, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(3), 3, 9999.98, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(4), 4, 0.02, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(5), 5, 0.04, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(6), 6, 0.06, null, 0)
      );
      List<Idx0Datagram> expected = List.of(
            new Idx0Datagram(Instant.ofEpochSecond(2), 2, 9999.96, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(3), 3, 9999.98, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(4), 4, 10000.02, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(5), 5, 10000.04, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(6), 6, 10000.06, null, 0)
      );
      test(input, expected, 0, 0, 0, 0, new WrapAround(input.get(2), 10000.0));
   }

   private static void test(List<? extends BaseDatagram> input, List<Idx0Datagram> expected,
                            int otherDatagramCount, int missingCount, int ignoredCount, int spikeCount, @Nullable WrapAround wrapAround) throws IOException {
      IdxCorrectionFilter idxCorrectionFilter = new IdxCorrectionFilter(DatagramSource.ofDatagrams(input));
      List<Idx0Datagram> correctedIdx0Datagrams = DataUtils.getAll(idxCorrectionFilter, Idx0Datagram.class);
      assertEquals(expected, correctedIdx0Datagrams);
      assertEquals(otherDatagramCount, idxCorrectionFilter.getOtherDatagrams().size());
      assertEquals(missingCount, idxCorrectionFilter.getMissingCount());
      assertEquals(ignoredCount, idxCorrectionFilter.getIgnoredCount());
      assertEquals(spikeCount, idxCorrectionFilter.getSpikeCount());
      assertEquals(wrapAround, idxCorrectionFilter.getWrapAround());
   }

   @Test
   void modifyResult() throws IOException {
      List<Idx0Datagram> input = List.of(
            new Idx0Datagram(Instant.ofEpochSecond(1), 1, 1, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(2), 2, 2, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(4), 4, 4, null, 0)
      );
      IdxCorrectionFilter idxCorrectionFilter = new IdxCorrectionFilter(DatagramSource.ofDatagrams(input));

      Idx0Datagram idx0Datagram = idxCorrectionFilter.nextDatagram();
      assertNotNull(idx0Datagram);
      assertEquals(Instant.ofEpochSecond(1), idx0Datagram.getInstant());
      assertEquals(1L, idx0Datagram.getPingNumber());
      assertEquals(1d, idx0Datagram.getVesselDistance());

      idx0Datagram.setInstant(Instant.ofEpochSecond(99));
      idx0Datagram.setPingNumber(99);
      idx0Datagram.setVesselDistance(99);

      idx0Datagram = idxCorrectionFilter.nextDatagram();
      assertNotNull(idx0Datagram);
      assertEquals(Instant.ofEpochSecond(2), idx0Datagram.getInstant());
      assertEquals(2L, idx0Datagram.getPingNumber());
      assertEquals(2d, idx0Datagram.getVesselDistance());

      idx0Datagram.setInstant(Instant.ofEpochSecond(99));
      idx0Datagram.setPingNumber(99);
      idx0Datagram.setVesselDistance(99);

      idx0Datagram = idxCorrectionFilter.nextDatagram();
      assertNotNull(idx0Datagram);
      assertEquals(Instant.ofEpochSecond(2, NTDate.NANOSECONDS_PER_NT_DATE_UNIT), idx0Datagram.getInstant());
      assertEquals(3L, idx0Datagram.getPingNumber());
      assertEquals(2d, idx0Datagram.getVesselDistance());

      idx0Datagram.setInstant(Instant.ofEpochSecond(99));
      idx0Datagram.setPingNumber(99);
      idx0Datagram.setVesselDistance(99);

      idx0Datagram = idxCorrectionFilter.nextDatagram();
      assertNotNull(idx0Datagram);
      assertEquals(Instant.ofEpochSecond(4), idx0Datagram.getInstant());
      assertEquals(4L, idx0Datagram.getPingNumber());
      assertEquals(4d, idx0Datagram.getVesselDistance());

      idx0Datagram = idxCorrectionFilter.nextDatagram();
      assertNull(idx0Datagram);

      assertEquals(0, idxCorrectionFilter.getOtherDatagrams().size());
      assertEquals(1, idxCorrectionFilter.getMissingCount());
      assertEquals(0, idxCorrectionFilter.getIgnoredCount());
   }

   @Test
   void time() throws IOException {
      List<Idx0Datagram> input = List.of(
            new Idx0Datagram(Instant.ofEpochSecond(1), 1, 1, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(2), 2, 2, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(2), 3, 2, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(3), 4, 2, null, 0),

            new Idx0Datagram(Instant.ofEpochSecond(11), 5, 11, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(11), 6, 11, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(11), 7, 11, null, 0),

            new Idx0Datagram(Instant.ofEpochSecond(20), 8, 20, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(20), 9, 23, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(30), 10, 30, null, 0),

            new Idx0Datagram(Instant.ofEpochSecond(50), 11, 50, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(45), 12, 55, null, 0)
      );
      IdxCorrectionFilter idxCorrectionFilter = new IdxCorrectionFilter(DatagramSource.ofDatagrams(input));

      checkNext(Instant.ofEpochSecond(1), idxCorrectionFilter);
      checkNext(Instant.ofEpochSecond(2), idxCorrectionFilter);
      checkNext(Instant.ofEpochSecond(2, NTDate.NANOSECONDS_PER_NT_DATE_UNIT), idxCorrectionFilter);
      checkNext(Instant.ofEpochSecond(3), idxCorrectionFilter);

      checkNext(Instant.ofEpochSecond(11), idxCorrectionFilter);
      checkNext(Instant.ofEpochSecond(11, NTDate.NANOSECONDS_PER_NT_DATE_UNIT), idxCorrectionFilter);
      checkNext(Instant.ofEpochSecond(11, 2 * NTDate.NANOSECONDS_PER_NT_DATE_UNIT), idxCorrectionFilter);

      checkNext(Instant.ofEpochSecond(20), idxCorrectionFilter);
      checkNext(Instant.ofEpochSecond(23), idxCorrectionFilter);
      checkNext(Instant.ofEpochSecond(30), idxCorrectionFilter);

      checkNext(Instant.ofEpochSecond(50), idxCorrectionFilter);
      checkNext(Instant.ofEpochSecond(50, NTDate.NANOSECONDS_PER_NT_DATE_UNIT), idxCorrectionFilter);

      assertNull(idxCorrectionFilter.nextDatagram());
   }

   private static void checkNext(Instant expectedTime, IdxCorrectionFilter idxCorrectionFilter) throws IOException {
      Idx0Datagram idx0Datagram = idxCorrectionFilter.nextDatagram();
      assertNotNull(idx0Datagram);
      assertEquals(expectedTime, idx0Datagram.getInstant());
   }
}
