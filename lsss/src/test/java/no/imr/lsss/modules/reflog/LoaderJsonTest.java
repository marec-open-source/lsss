package no.imr.lsss.modules.reflog;

import no.imr.tools.ResourceUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class LoaderJsonTest {
   @Test
   void test() throws IOException {
      List<LogLine> logLines;
      try (InputStream in = ResourceUtils.getUrl("no/imr/lsss/modules/reflog/9cc68000-9e7c-11eb-a429-b7b91b84686c.json").openStream()) {
         logLines = new LoaderJson().load(in);
      }
      assertEquals(2, logLines.size());
      assertEquals(new LogLine(
            Instant.parse("2021-04-16T06:26:00.828Z"),
            ActivityType.PELAGIC_TRAWL,
            true,
            "Pelagisk trål - start",
            "3",
            List.of(
                  new LogLineField("Max depth", "m"),
                  new LogLineField("Min depth", "m"),
                  new LogLineField("Antall", ""),
                  new LogLineField("Kode (SPD)", ""),
                  new LogLineField("Wirelengde", ""),
                  new LogLineField("Redskapsnummer", ""),
                  new LogLineField("Serienummer", ""),
                  new LogLineField("Åpning", "m"),
                  new LogLineField("Dørspredning", "m")
            ),
            List.of("83.95", "73.35", "1", "3517", "", "", "3", "", "0")
      ), logLines.getFirst());
   }
}
