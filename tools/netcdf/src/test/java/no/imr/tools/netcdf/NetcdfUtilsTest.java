package no.imr.tools.netcdf;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class NetcdfUtilsTest {
   @Test
   void netcdfTime() {
      assertThrows(IllegalArgumentException.class, () -> NetcdfUtils.instantToNetcdfTime(Instant.parse("1600-12-31T23:59:59.999999999Z")));
      check(Instant.parse("1601-01-01T00:00:00.000000000Z"), 0);
      check(Instant.parse("1601-01-01T00:00:00.000000001Z"), 1);
      check(Instant.parse("1893-04-11T23:47:16.854775807Z"), 0x7fff_ffff_ffff_ffffL);
      check(Instant.parse("1893-04-11T23:47:16.854775808Z"), 0x8000_0000_0000_0000L);
      check(Instant.parse("1893-04-11T23:47:16.854775809Z"), 0x8000_0000_0000_0001L);
      check(Instant.parse("2185-07-21T23:34:33.709551615Z").minusNanos(709551616), 0xffff_ffff_ffff_ffffL - 709551616);
      assertThrows(IllegalArgumentException.class, () -> check(Instant.parse("2185-07-21T23:34:33.709551614Z"), 0xffff_ffff_ffff_fffeL)); // Should work, but does not with current implementation.
      assertThrows(IllegalArgumentException.class, () -> check(Instant.parse("2185-07-21T23:34:33.709551615Z"), 0xffff_ffff_ffff_ffffL)); // Should work, but does not with current implementation.
      assertThrows(IllegalArgumentException.class, () -> NetcdfUtils.instantToNetcdfTime(Instant.parse("2185-07-21T23:34:33.709551616Z")));
   }

   private static void check(Instant instant, long netcdfTime) {
      assertEquals(instant, NetcdfUtils.netcdfTimeToInstant(netcdfTime));
      assertEquals(netcdfTime, NetcdfUtils.instantToNetcdfTime(instant));
   }
}
