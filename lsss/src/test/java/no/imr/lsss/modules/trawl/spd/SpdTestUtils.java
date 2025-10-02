package no.imr.lsss.modules.trawl.spd;

import no.imr.tools.ResourceUtils;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public final class SpdTestUtils {
   private SpdTestUtils() {
   }

   public static List<SpdStation> loadStations() throws IOException {
      try (BufferedReader reader = FileUtils.newBufferedReader(ResourceUtils.getUrl("no/imr/lsss/modules/trawl/4-2018-1019-7.spd"), Utils.ISO_8859_1)) {
         return SpdParser.parse(reader);
      }
   }
}
