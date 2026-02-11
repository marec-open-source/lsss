package no.imr.korona.data.ping.items.channel;

import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.tools.range.FloatRange;
import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

final class PowerDataTest {
   @Test
   void linearPowerToShortPower() {
      assertEquals(PowerData.EK60_SHORT_POWER_NULL, PowerData.linearPowerToShortPower(-1));
      assertEquals(PowerData.EK60_SHORT_POWER_NULL, PowerData.linearPowerToShortPower(0));
      assertEquals(PowerData.EK60_SHORT_POWER_NULL, PowerData.linearPowerToShortPower(PowerData.shortPowerToLinearPower(Short.MIN_VALUE)));
      assertEquals(PowerData.EK60_SHORT_POWER_NULL, PowerData.linearPowerToShortPower(PowerData.shortPowerToLinearPower(PowerData.EK60_SHORT_POWER_NULL)));
      assertEquals(PowerData.EK60_SHORT_POWER_NULL, PowerData.linearPowerToShortPower(Math.nextUp(PowerData.shortPowerToLinearPower(PowerData.EK60_SHORT_POWER_NULL))));
      assertEquals(Short.MAX_VALUE, PowerData.linearPowerToShortPower(Math.nextDown(PowerData.shortPowerToLinearPower(Short.MAX_VALUE))));
      assertEquals(Short.MAX_VALUE, PowerData.linearPowerToShortPower(PowerData.shortPowerToLinearPower(Short.MAX_VALUE)));
      assertEquals(Short.MAX_VALUE, PowerData.linearPowerToShortPower(Float.MAX_VALUE));
   }

   @Test
   void testSetRange() {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData(10, 0).withFirstAndLastPingNumber(1, 1);
      PingIndex pingIndex = syntheticDataFile.createPingIndex(1);
      PowerData powerData = syntheticDataFile.createPowerData(pingIndex, 1);
      assertNotNull(powerData);

      powerData.setTransducerDepth(0);
      float sd = powerData.getSampleDistance();

      powerData.setOffset(0);
      powerData.setCount(10);
      powerData.setRange(powerData.getMinDepth(), powerData.getMaxDepth());
      assertEquals(0, powerData.getOffset());
      assertEquals(10, powerData.getCount());

      powerData.setRange(3 * sd, 17 * sd);
      assertEquals(3, powerData.getOffset());
      assertEquals(14, powerData.getCount());

      powerData.setRange(0.1f * sd, 2.9f * sd);
      assertEquals(0, powerData.getOffset());
      assertEquals(3, powerData.getCount());
   }

   @Test
   void testConversion() {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData(10, 0).withFirstAndLastPingNumber(1, 1);
      PingIndex pingIndex = syntheticDataFile.createPingIndex(1);
      PowerData powerData = syntheticDataFile.createPowerData(pingIndex, 1);
      assertNotNull(powerData);

      for (short p = -1230; p < 1230; p += 17) {
         short[] power = new short[powerData.getCount()];
         Arrays.fill(power, p);
         powerData.setSvFromShortPower(power);

         powerData.setSv(powerData.getSv());
         powerData.setLogSv(powerData.getLogSv());
         JUnitUtils.assertAllEquals(p, powerData.computeShortPower());

         powerData.setLogSv(powerData.getLogSv());
         powerData.setSv(powerData.getSv());
         JUnitUtils.assertAllEquals(p, powerData.computeShortPower());
      }
   }

   @Test
   void testSvLogSvConversion() {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData(10, 0).withFirstAndLastPingNumber(1, 1);
      PingIndex pingIndex = syntheticDataFile.createPingIndex(1);
      PowerData powerData = syntheticDataFile.createPowerData(pingIndex, 1);
      assertNotNull(powerData);

      Arrays.fill(powerData.getLogSv(), -56);
      powerData.setLogSv(powerData.getLogSv());
      assertEquals(-56, powerData.getLogSv()[5]);
      powerData.setSv(powerData.getSv());
      JUnitUtils.assertAllEquals(-56, powerData.getLogSv());

      Arrays.fill(powerData.getSv(), 56);
      powerData.setSv(powerData.getSv());
      assertEquals(56, powerData.getSv()[5]);
      powerData.setLogSv(powerData.getLogSv());
      JUnitUtils.assertAllEquals(56, powerData.getSv(), 1e-4f);

      powerData.setCount(11);
      JUnitUtils.assertAllEquals(0, powerData.getSv());
      JUnitUtils.assertAllEquals(-Float.MAX_VALUE, powerData.getLogSv());

      powerData.setCount(10);
      JUnitUtils.assertAllEquals(-Float.MAX_VALUE, powerData.getLogSv());
      JUnitUtils.assertAllEquals(0, powerData.getSv());
   }

   @Test
   void testPowerConversion() {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData(10, 0).withFirstAndLastPingNumber(1, 1);
      PingIndex pingIndex = syntheticDataFile.createPingIndex(1);
      PowerData powerData = syntheticDataFile.createPowerData(pingIndex, 1);
      assertNotNull(powerData);

      short[] power = {-1000, -513, -100, -1, 0, 1, 123, 800, 900, 10000};

      powerData.setSvFromShortPower(power);
      powerData.setSv(powerData.getSv());
      assertArrayEquals(power, powerData.computeShortPower());

      powerData.setSvFromShortPower(power);
      powerData.setLogSv(powerData.getLogSv());
      assertArrayEquals(power, powerData.computeShortPower());
   }

   @Test
   void extremeLogSvToPower() {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData(10, 0).withFirstAndLastPingNumber(1, 1);
      PingIndex pingIndex = syntheticDataFile.createPingIndex(1);
      PowerData powerData = syntheticDataFile.createPowerData(pingIndex, 1);
      assertNotNull(powerData);

      for (int i = 0; i < powerData.getCount(); i++) {
         powerData.getLogSv()[i] = -1000 - i * 100;
      }
      powerData.setLogSv(powerData.getLogSv());
      powerData.setSvFromShortPower(powerData.computeShortPower());
      JUnitUtils.assertTrue(powerData.getLogSv(), value -> value < -200);

      for (int i = 0; i < powerData.getCount(); i++) {
         powerData.getLogSv()[i] = 1000 + i * 100;
      }
      powerData.setLogSv(powerData.getLogSv());
      powerData.setSvFromShortPower(powerData.computeShortPower());
      JUnitUtils.assertTrue(powerData.getLogSv(), value -> value > 200);
   }

   @Test
   void angles() {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData(10, 0).withFirstAndLastPingNumber(1, 1);
      PingIndex pingIndex = syntheticDataFile.createPingIndex(1);
      PowerData powerData = syntheticDataFile.createPowerData(pingIndex, 1);
      assertNotNull(powerData);

      //---

      powerData.setElectricAngles(new float[2 * powerData.getCount()]);

      powerData.setSv(new float[powerData.getCount()]);
      assertNotNull(powerData.getAngleData());

      powerData.setSv(new float[powerData.getCount() - 1]);
      assertNull(powerData.getAngleData());

      //---

      powerData.setElectricAngles(new float[2 * powerData.getCount()]);

      powerData.setLogSv(new float[powerData.getCount()]);
      assertNotNull(powerData.getAngleData());

      powerData.setLogSv(new float[powerData.getCount() - 1]);
      assertNull(powerData.getAngleData());
   }

   @Test
   void getVerticalIntegralSv() {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData(20, 1).withFirstAndLastPingNumber(1, 1);
      PingIndex pingIndex = syntheticDataFile.createPingIndex(1);
      PowerData powerData = syntheticDataFile.createPowerData(pingIndex, 1);
      assertNotNull(powerData);
      float dist = powerData.getSampleDistance();
      float depth = powerData.getSampleDepth(5) + 0.1f * dist;
      assertEquals(0, powerData.getVerticalIntegralSv(FloatRange.ofMinAndSize(depth, 0), FloatRange.ALL));
      assertEquals(0.1 * dist, powerData.getVerticalIntegralSv(FloatRange.ofMinAndSize(depth, 0.1f * dist), FloatRange.ALL), 1e-6);
      assertEquals(dist, powerData.getVerticalIntegralSv(FloatRange.ofMinAndSize(depth, dist), FloatRange.ALL), 1e-6);
      assertEquals(2 * dist, powerData.getVerticalIntegralSv(FloatRange.ofMinAndSize(depth, 2 * dist), FloatRange.ALL), 1e-6);
      assertEquals(10 * dist, powerData.getVerticalIntegralSv(FloatRange.ofMinAndSize(depth, 10 * dist), FloatRange.ALL), 1e-6);
   }
}
