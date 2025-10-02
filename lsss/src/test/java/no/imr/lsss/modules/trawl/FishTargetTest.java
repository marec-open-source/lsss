package no.imr.lsss.modules.trawl;

import no.imr.lsss.modules.trawl.biotic.BioticTestUtils;
import no.imr.lsss.modules.trawl.spd.SpdTestUtils;
import no.imr.tools.math.WelfordsMethod;
import org.jfree.data.xy.IntervalXYDataset;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class FishTargetTest {
   @Test
   void spd() throws IOException {
      test(true, FishStation.fromSpd(SpdTestUtils.loadStations()));
   }

   @Test
   void biotic() throws IOException {
      test(false, FishStation.fromBiotic(BioticTestUtils.loadBioticFile()));
   }

   private static void test(boolean spd, List<FishStation> stations) {
      FishStation station = stations.get(6);
      FishTarget target = station.getTargets().getFirst();
      assertEquals("MAKRELL", target.speciesName);
      WelfordsMethod welfordsMethod = target.getFishLengths();
      assertEquals(spd ? 25.330000057220452 : 25.080000114440914, welfordsMethod.getMean());
      assertEquals(spd ? 3.8420323093506172 : 3.8420323532783090, welfordsMethod.getStdDev());

      assertEquals(spd ? 0.39733609034896360 : 0.389705405949123500, target.getSa(-100));
      assertEquals(spd ? 0.12693792705868304 : 0.124806602633031190, target.getSa(-56));
      assertEquals(spd ? 0.02303094321137573 : 0.022708898276807585, target.getSa(-55));
      assertEquals(0, target.getSa(-50));

      IntervalXYDataset dataset = target.getFishLengthDataset(Language.NORWEGIAN);
      int lastSeries = spd ? 0 : 2;
      assertEquals(lastSeries + 1, dataset.getSeriesCount());
      assertEquals(36, dataset.getItemCount(0));
      test(dataset, lastSeries, 0, 19.0, 19.5, 2);
      test(dataset, lastSeries, 16, 27.0, 27.5, 14);
      test(dataset, lastSeries, 35, 36.5, 37.0, 1);
   }

   private static void test(IntervalXYDataset dataset, int series, int item, double x1, double x2, int y) {
      assertEquals(x1, dataset.getStartXValue(series, item));
      assertEquals(x2, dataset.getEndXValue(series, item));
      assertEquals(y, dataset.getEndYValue(series, item));
   }
}
