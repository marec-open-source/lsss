package no.imr.lsss.modules.trawl.biotic;

import no.imr.lsss.modules.trawl.biotic.pojo.BioticCatchSample;
import no.imr.lsss.modules.trawl.biotic.pojo.BioticFile;
import no.imr.lsss.modules.trawl.biotic.pojo.BioticFishStation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class BioticFileTest {
   @Test
   void test() throws IOException {
      BioticFile bioticFile = BioticTestUtils.loadBioticFile();
      List<BioticFishStation> stations = bioticFile.missions.getFirst().fishstations;

      assertEquals(7, stations.size());

      BioticFishStation station = stations.get(5);
      assertNull(BioticUtils.parseDateAndTime(null, null));
      assertNull(BioticUtils.parseDateAndTime(null, station.stationstarttime));
      assertEquals(LocalDate.of(2018, 7, 3).atStartOfDay().toInstant(ZoneOffset.UTC),
            BioticUtils.parseDateAndTime(station.stationstartdate, null));
      assertEquals(LocalDate.of(2018, 7, 3).atTime(21, 22, 6).toInstant(ZoneOffset.UTC),
            BioticUtils.parseDateAndTime(station.stationstartdate, station.stationstarttime));
      assertEquals(LocalDate.of(2018, 7, 3).atTime(21, 52, 7).toInstant(ZoneOffset.UTC),
            BioticUtils.parseDateAndTime(station.stationstopdate, station.stationstoptime));
      assertEquals(1.8659999999999997, station.distance);
      assertEquals("1", station.gearcondition);

      BioticCatchSample sample = station.catchsamples.get(4);
      assertEquals("makrell", sample.commonname);
      assertEquals(List.of(0.19f, 0.275f), sample.individuals.stream().map(individual -> individual.length).toList());
   }
}
