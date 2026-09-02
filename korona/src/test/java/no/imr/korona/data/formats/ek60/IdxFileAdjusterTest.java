package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.Utils;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class IdxFileAdjusterTest {
   @Test
   void test() {
      IdxFileAdjuster idxFileAdjuster = new IdxFileAdjuster();
      idxFileAdjuster.setLast(List.of(
            new Idx0Datagram(Instant.ofEpochSecond(1000), 1000, 1000, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(1100), 1001, 1010, null, 0)
      ));

      IdxFile a = new IdxFile(Utils.getTmpDir().resolve("a.idx"), List.of(
            new Idx0Datagram(Instant.ofEpochSecond(10), 11, 101, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(20), 12, 102, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(30), 13, 103, null, 0)
      ), new RawFileConfiguration(Instant.ofEpochSecond(0)), List.of(), null);
      idxFileAdjuster.adjustNext(a);
      check(a.idx0Datagrams().get(0), Instant.ofEpochSecond(1200), 1002, 1020);
      check(a.idx0Datagrams().get(1), Instant.ofEpochSecond(1210), 1003, 1021);
      check(a.idx0Datagrams().get(2), Instant.ofEpochSecond(1220), 1004, 1022);

      IdxFile b = new IdxFile(Utils.getTmpDir().resolve("b.idx"), List.of(
            new Idx0Datagram(Instant.ofEpochSecond(3), 12, 101, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(4), 13, 105, null, 0)
      ), new RawFileConfiguration(Instant.ofEpochSecond(0)), List.of(), null);
      idxFileAdjuster.adjustNext(b);
      check(b.idx0Datagrams().get(0), Instant.ofEpochSecond(1230), 1005, 1023);
      check(b.idx0Datagrams().get(1), Instant.ofEpochSecond(1231), 1006, 1027);

      IdxFile c = new IdxFile(Utils.getTmpDir().resolve("c.idx"), List.of(
            new Idx0Datagram(Instant.ofEpochSecond(1300), 1007, 1030, null, 0),
            new Idx0Datagram(Instant.ofEpochSecond(1400), 1008, 1040, null, 0)
      ), new RawFileConfiguration(Instant.ofEpochSecond(0)), List.of(), null);
      idxFileAdjuster.adjustNext(c);
      check(c.idx0Datagrams().get(0), Instant.ofEpochSecond(1300), 1007, 1030);
      check(c.idx0Datagrams().get(1), Instant.ofEpochSecond(1400), 1008, 1040);
   }

   private static void check(Idx0Datagram idx, Instant instant, long pingNumber, double vesselDistance) {
      assertEquals(instant, idx.getInstant());
      assertEquals(pingNumber, idx.getPingNumber());
      assertEquals(vesselDistance, idx.getVesselDistance());
   }
}
