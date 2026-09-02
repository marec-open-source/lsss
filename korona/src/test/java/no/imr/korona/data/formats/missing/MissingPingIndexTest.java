package no.imr.korona.data.formats.missing;

import no.imr.korona.data.ping.DefaultPingIndex;
import no.marec.lsss.api.util.GeoPoint;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class MissingPingIndexTest {
   @Test
   void test() {
      MissingPingIndex m = MissingPingIndex.create(
            new DefaultPingIndex(Instant.ofEpochSecond(10), 10, 1, new GeoPoint(5, 60)),
            new DefaultPingIndex(Instant.ofEpochSecond(20), 20, 2, new GeoPoint(6, 70)),
            13
      );
      assertEquals(Instant.ofEpochSecond(13), m.getInstant());
      assertEquals(13, m.getPingNumber());
      assertEquals(1.3, m.getVesselDistance());
      assertNotNull(m.getGeographicalPosition());
      assertEquals(5.3, m.getGeographicalPosition().getLongitude());
      assertEquals(63, m.getGeographicalPosition().getLatitude());
   }
}
