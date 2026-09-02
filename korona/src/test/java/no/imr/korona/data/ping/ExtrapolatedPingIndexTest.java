package no.imr.korona.data.ping;

import no.marec.lsss.api.util.GeoPoint;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class ExtrapolatedPingIndexTest {
   @Test
   void one() {
      DefaultPingIndex a = new DefaultPingIndex(Instant.ofEpochMilli(1000), 1, 2, new GeoPoint(5, 60));
      ExtrapolatedPingIndex e = new ExtrapolatedPingIndex(a);
      assertEquals(Instant.ofEpochMilli(2000), e.getInstant());
      assertEquals(2, e.getPingNumber());
      assertEquals(2 + 11.0 / 3600, e.getVesselDistance());
      assertEquals(a.getGeographicalPosition(), e.getGeographicalPosition());
   }

   @Test
   void two() {
      DefaultPingIndex a = new DefaultPingIndex(Instant.ofEpochMilli(1000), 1, 2, new GeoPoint(5, 60.01));
      DefaultPingIndex b = new DefaultPingIndex(Instant.ofEpochMilli(2001), 2, 2.1, new GeoPoint(5.01, 60.03));
      ExtrapolatedPingIndex e = new ExtrapolatedPingIndex(a, b);
      assertEquals(Instant.ofEpochMilli(3002), e.getInstant());
      assertEquals(3, e.getPingNumber());
      assertEquals(2.2, e.getVesselDistance());
      assertNotNull(e.getGeographicalPosition());
      assertEquals(5.02, e.getGeographicalPosition().getLongitude());
      assertEquals(60.05, e.getGeographicalPosition().getLatitude(), 1e-14);
   }
}
