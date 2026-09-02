package no.imr.korona.data.datagrams;

import no.imr.korona.computation.plankton.SizeHistogram;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class Pid0DatagramTest {
   @Test
   void testLengthDistribution() {
      Pid0Datagram.LengthDistribution lengthDistribution;

      lengthDistribution = new Pid0Datagram.LengthDistribution(
            new double[]{0, 1},
            new double[]{1});
      assertArrayEquals(new float[]{0, 1}, lengthDistribution.getDividers());
      assertArrayEquals(new float[]{1}, lengthDistribution.getAbundances());

      lengthDistribution = new Pid0Datagram.LengthDistribution(
            new double[]{0, 1 /**/, 1, 2},
            new double[]{1 /*     */, 2});
      assertArrayEquals(new float[]{0, 1, 2}, lengthDistribution.getDividers());
      assertArrayEquals(new float[]{1, 2}, lengthDistribution.getAbundances());

      lengthDistribution = new Pid0Datagram.LengthDistribution(
            new double[]{0, 1 /**/, 1, 2 /**/, 2, 3},
            new double[]{0 /*     */, 2 /*  */, 0});
      assertArrayEquals(new float[]{1, 2}, lengthDistribution.getDividers());
      assertArrayEquals(new float[]{2}, lengthDistribution.getAbundances());

      lengthDistribution = new Pid0Datagram.LengthDistribution(
            new double[]{0, 1 /* gap */, 2, 3},
            new double[]{1 /*          */, 2});
      assertArrayEquals(new float[]{0, 1, 2, 3}, lengthDistribution.getDividers());
      assertArrayEquals(new float[]{1, 0, 2}, lengthDistribution.getAbundances());

      lengthDistribution = new Pid0Datagram.LengthDistribution(
            new double[]{0, 1 /**/, 1, 2 /**/, 2, 3 /* gap */, 5, 6 /**/, 6, 7 /**/, 8, 9},
            new double[]{0 /*     */, 3 /*   */, 7 /*        */, 0 /*   */, 1 /**/, 0});
      assertArrayEquals(new float[]{1, 2, 3, 6, 7}, lengthDistribution.getDividers());
      assertArrayEquals(new float[]{3, 7, 0, 1}, lengthDistribution.getAbundances());

      lengthDistribution = new Pid0Datagram.LengthDistribution(
            new double[]{0, 1 /* gap */, 2, 3 /* gap */, 4, 5},
            new double[]{1 /*          */, 0 /*        */, 1});
      assertArrayEquals(new float[]{0, 1, 4, 5}, lengthDistribution.getDividers());
      assertArrayEquals(new float[]{1, 0, 1}, lengthDistribution.getAbundances());
   }

   @Test
   void testPid0Datagram() {
      int count = 10;
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData(count, 100).withFirstAndLastPingNumber(0, 0);

      SizeHistogram sizeHistogram = new SizeHistogram(new double[]{0, 1 /**/, 1, 2 /**/, 2, 3});
      JUnitUtils.set(sizeHistogram.getAbundances(), 0, 1, 0);

      Pic0Datagram.PlanktonCategory testCategory = new Pic0Datagram.PlanktonCategory("TestTest", "Test", 13, Color.RED);

      Pic0Datagram pic0 = new Pic0Datagram(Instant.EPOCH);
      pic0.addCategory(testCategory);

      Pid0Datagram.LengthDistribution lengthDistribution = new Pid0Datagram.LengthDistribution(sizeHistogram.getDividers(), sizeHistogram.getAbundances());
      Pid0Datagram.PlanktonData planktonData = new Pid0Datagram.PlanktonData(testCategory, lengthDistribution, 1.0f, 0);

      PowerData powerData = syntheticDataFile.createPowerData(syntheticDataFile.createPingIndex(0), 1);
      assertNotNull(powerData);
      Pid0Datagram pid0 = new Pid0Datagram(powerData, Instant.EPOCH);
      pid0.setCategorySamples(0, List.of(planktonData));

      List<Pid0Datagram.PlanktonSample> planktonSamples = pid0.getPlanktonSamples(pic0);
      assertEquals(count, planktonSamples.size());

      Pic0Datagram.PlanktonCategory firstCategory = planktonSamples.getFirst().getBestPlanktonData().getPlanktonCategory();
      assertNotNull(firstCategory);
      assertEquals(testCategory.getNumber(), firstCategory.getNumber());

      Pic0Datagram.PlanktonCategory lastCategory = planktonSamples.getLast().getBestPlanktonData().getPlanktonCategory();
      assertNotNull(lastCategory);
      assertEquals(testCategory.getNumber(), lastCategory.getNumber());

      assertArrayEquals(new float[]{1, 2}, planktonSamples.get(1).getBestPlanktonData().getLengthDistribution().getDividers());
      assertArrayEquals(new float[]{1}, planktonSamples.get(1).getBestPlanktonData().getLengthDistribution().getAbundances());
   }
}
