package no.imr.lsss.modules.ctd;

import no.imr.tools.ResourceUtils;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class CTDDataModuleTest {
   private static CTDData load() throws IOException {
      try (BufferedReader reader = FileUtils.newBufferedReader(ResourceUtils.getUrl("no/imr/lsss/modules/ctd/STA1013.CNV"), Utils.ISO_8859_1)) {
         return CTDDataModule.loadCnvFile(null, reader);
      }
   }

   @Test
   void cnv() throws IOException {
      CTDData ctdData = load();
      assertEquals(LocalDate.of(2002, 11, 24).atTime(5, 48, 25).toInstant(ZoneOffset.UTC), ctdData.time());
      assertEquals(16.09750000000000, ctdData.geographicalPosition().getLongitude());
      assertEquals(68.37383333333334, ctdData.geographicalPosition().getLatitude());
      assertEquals(List.of("scan number", "pressure [db]", "temperature, IPTS-68 [deg C]", "conductivity [S/m]", "oxygen, current [æA]", "oxygen, temperature [deg C]",
            "density, sigma-theta [kg/m^3]", "salinity, PSS-78 [PSU]", "sound velocity, chen millero [m/s]", "0.000e+00"), ctdData.columnNames());
      assertEquals(23, ctdData.rows().size());
      assertArrayEquals(new float[]{624, 3, 5.7199f, 3.14246f, 0, 0, 25.1832f, 31.9582f, 1469.82f, 0}, ctdData.rows().get(1));
      assertArrayEquals(new float[]{1173, 24, 5.7457f, 3.145557f, 0, 0, 25.1805f, 31.9584f, 1470.27f, 0}, ctdData.rows().getLast());
   }
}
