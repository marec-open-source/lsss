package no.imr.lsss.modules.trawl.biotic;

import no.imr.lsss.modules.trawl.biotic.pojo.BioticFile;
import no.imr.tools.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public final class BioticTestUtils {
   private BioticTestUtils() {
   }

   public static BioticFile loadBioticFile() throws IOException {
      URL url = ResourceUtils.getUrl("no/imr/lsss/modules/trawl/4-2018-1019-7.xml");
      try (InputStream in = url.openStream()) {
         return BioticUtils.loadBioticFile(in);
      }
   }
}
