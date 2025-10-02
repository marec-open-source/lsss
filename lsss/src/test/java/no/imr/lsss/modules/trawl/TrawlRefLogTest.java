package no.imr.lsss.modules.trawl;

import no.imr.lsss.modules.reflog.LogLine;
import no.imr.lsss.modules.trawl.spd.SpdTestUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class TrawlRefLogTest {
   @Test
   void spd() throws IOException {
      List<FishStation> stations = FishStation.fromSpd(SpdTestUtils.loadStations());
      List<List<String>> expectedFieldValues = List.of(
            List.of("331", "8825.000", "5825.700000", "350.700000", "24101"),
            List.of("331", "8825.000", "5825.700000", "350.700000", "24101"),
            List.of("332", "8889.000", "5826.500000", "350.100000", "24102"),
            List.of("332", "8889.000", "5826.500000", "350.100000", "24102"),
            List.of("333", "8929.000", "5825.200000", "352.700000", "24103"),
            List.of("333", "8929.000", "5825.200000", "352.700000", "24103"),
            List.of("334", "8960.000", "5825.300000", "351.000000", "24104"),
            List.of("334", "8960.000", "5825.300000", "351.000000", "24104"),
            List.of("335", "9249.000", "5805.900000", "427.800000", "24105"),
            List.of("335", "9249.000", "5805.900000", "427.800000", "24105"),
            List.of("336", "545.000", "5639.400000", "509.400000", "24106"),
            List.of("336", "545.000", "5639.400000", "509.400000", "24106"),
            List.of("337", "930.000", "5639.300000", "401.100000", "24107"),
            List.of("337", "930.000", "5639.300000", "401.100000", "24107"));

      check(expectedFieldValues,
            TrawlRefLog.createLogLines(stations, Set.of()));

      check(expectedFieldValues.subList(2, 6),
            TrawlRefLog.createLogLines(stations, Set.of("331", "334", "335", "336", "337")));
   }

   private static void check(List<List<String>> expectedFieldValues, List<LogLine> logLines) {
      assertEquals(expectedFieldValues.size(), logLines.size());
      for (int i = 0; i < logLines.size(); i++) {
         assertEquals(i % 2 == 0 ? "Trawl start (from trawl file)" : "Trawl stop (from trawl file)", logLines.get(i).stationType());
         assertEquals(expectedFieldValues.get(i), logLines.get(i).fieldValues());
      }
   }
}
