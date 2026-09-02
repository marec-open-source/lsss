package no.imr.korona.data.util;

import no.imr.korona.data.datagrams.Mru0Datagram;
import no.imr.korona.data.datagrams.MruDatagram;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class DataUtilsTest {
   @Test
   void interpolateMru() {
      Instant instant = Instant.ofEpochSecond(1234567890);
      MruDatagram mru = DataUtils.interpolateMru(
            new Mru0Datagram(instant, 1, 1, 1, 359),
            new Mru0Datagram(instant.plusSeconds(10), 11, 21, 31, 9),
            instant.plusSeconds(1)
      );
      assertEquals(2, mru.getHeave());
      assertEquals(3, mru.getRoll());
      assertEquals(4, mru.getPitch());
      assertEquals(0, mru.getHeading());
   }
}
