package no.imr.lsss.modules.reflog;

import no.imr.tools.ResourceUtils;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class LoaderRefCsvTest {
   @Test
   void test() throws IOException {
      List<LogLine> logLines;
      try (BufferedReader in = FileUtils.newBufferedReader(ResourceUtils.getUrl("no/imr/lsss/modules/reflog/ref14-12-2000.csv"), Utils.ISO_8859_1)) {
         logLines = LoaderRefCsv.load(in);
      }
      assertEquals(15, logLines.size());
      assertEquals(new LogLine(
            Instant.parse("2000-12-14T16:18:55Z").toEpochMilli(),
            ActivityType.BOTTOM_TRAWL,
            true,
            "Bottom trawl start",
            "367",
            List.of(
                  new LogLineField("Ref. no", ""),
                  new LogLineField("Loc.St.no", ""),
                  new LogLineField("Logg", ""),
                  new LogLineField("Lattitude", ""),
                  new LogLineField("Longitude", ""),
                  new LogLineField("Depth", ""),
                  new LogLineField("Heading", ""),
                  new LogLineField("Velocity", ""),
                  new LogLineField("Water temp", ""),
                  new LogLineField("Wind ", ""),
                  new LogLineField("Wind dir", ""),
                  new LogLineField("Air temp", ""),
                  new LogLineField("Air pressure", ""),
                  new LogLineField("Humidity", ""),
                  new LogLineField("Weather", ""),
                  new LogLineField("Seastate", ""),
                  new LogLineField("Clouds", ""),
                  new LogLineField("Ice", ""),
                  new LogLineField("Quantity", ""),
                  new LogLineField("Code", ""),
                  new LogLineField("Number", ""),
                  new LogLineField("Serial no.", ""),
                  new LogLineField("Wirelenght", ""),
                  new LogLineField("Min depth", ""),
                  new LogLineField("Max depth", ""),
                  new LogLineField("Opening", ""),
                  new LogLineField("Spread", ""),
                  new LogLineField("Comment", "")),
            List.of(
                  "040", "367", " 9594.72", "4254.91 N", "03020.37 W", " 2960.11", " 191", "    1.40", "   19.00", "    7.60", "  204.00",
                  "   18.80", " 1026.20", "  88", "4", "3", "8", "0", "1", "3533", "7", " 1149", "4500", "-9", "-9", "", "", "")
      ), logLines.get(5));
   }
}
