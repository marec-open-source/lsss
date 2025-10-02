package no.imr.tools.netcdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class NetcdfUtilsTest {
   @Test
   void netcdfTime() {
      assertEquals(0, NetcdfUtils.netcdfTimeToNTDate(0));
      assertEquals(92233720368547758L, NetcdfUtils.netcdfTimeToNTDate(0x8000_0000_0000_0000L));
      assertEquals(92233720368547758L, NetcdfUtils.netcdfTimeToNTDate(0x7fff_ffff_ffff_fff8L));
      assertEquals(184467440737095516L, NetcdfUtils.netcdfTimeToNTDate(0xffff_ffff_ffff_ffffL));
      assertEquals(184467440737095516L, NetcdfUtils.netcdfTimeToNTDate(0xffff_ffff_ffff_fff0L));

      assertEquals(0, NetcdfUtils.ntDateToNetcdfTime(0));
      assertEquals(0x7fff_ffff_ffff_fff8L, NetcdfUtils.ntDateToNetcdfTime(92233720368547758L));
      assertEquals(0xffff_ffff_ffff_fff0L, NetcdfUtils.ntDateToNetcdfTime(184467440737095516L));
      assertThrows(IllegalArgumentException.class, () -> NetcdfUtils.ntDateToNetcdfTime(-1));
      assertThrows(IllegalArgumentException.class, () -> NetcdfUtils.ntDateToNetcdfTime(184467440737095516L + 1));
   }
}
