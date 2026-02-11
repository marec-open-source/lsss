package no.imr.korona.data.util;

import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.tools.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ResampledFloatArrayTest {
   @Test
   void testResampleFloatArray() {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData().withFirstAndLastPingNumber(1, 1);

      PingIndex pingIndex = syntheticDataFile.createPingIndex(1);
      PowerData powerData = syntheticDataFile.createPowerData(pingIndex, 1);
      assertNotNull(powerData);

      powerData.setTransducerDepth(0);
      float sd = powerData.getSampleDistance();

      powerData.setOffset(0);
      powerData.setCount(10);

      PowerData rawToResample = syntheticDataFile.createPowerData(pingIndex, 2);
      assertNotNull(rawToResample);
      rawToResample.setTransducerDepth(0);
      rawToResample.setSampleDistance(sd * 0.5f);
      rawToResample.setOffset(0);
      rawToResample.setCount(10 * 2);

      for (int i = 0; i < rawToResample.getCount(); i++) {
         rawToResample.getSv()[i] = (int) (i * 0.5);
      }

      ResampledFloatArray resampledArray = ResampledFloatArray.create(rawToResample.getSv(), rawToResample, powerData);

      assertEquals(resampledArray.values().length, powerData.getCount());
      assertEquals(0, resampledArray.getBeginReferenceIndex());
      assertEquals(powerData.getCount(), resampledArray.getEndReferenceIndex());
      assertEquals(0, resampledArray.values()[0], 0.001);
      assertEquals(9, resampledArray.values()[resampledArray.values().length - 1], 0.001);

      ResampledFloatArray resampledArrayWithOffset = ResampledFloatArray.create(rawToResample.getSv(), rawToResample, powerData, 2);

      assertEquals(resampledArrayWithOffset.values().length, powerData.getCount() - 1);
      assertEquals(1, resampledArrayWithOffset.getBeginReferenceIndex());
      assertEquals(powerData.getCount(), resampledArrayWithOffset.getEndReferenceIndex());
      assertEquals(1, resampledArrayWithOffset.values()[0], 0.001);
      assertEquals(9, resampledArrayWithOffset.values()[resampledArrayWithOffset.values().length - 1], 0.001);
   }

   @Test
   void fullResampleWithEmptyArray() {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData(0, Float.NaN).withFirstAndLastPingNumber(1, 1);
      PingIndex pingIndex = syntheticDataFile.createPingIndex(1);
      PowerData powerData = syntheticDataFile.createPowerData(pingIndex, 1);
      assertNotNull(powerData);
      PowerData powerDataToResample = syntheticDataFile.createPowerData(pingIndex, 2);
      assertNotNull(powerDataToResample);
      powerDataToResample.setTransducerDepth(powerData.getSampleDistance() / 2);
      ResampledFloatArray resampledArray = ResampledFloatArray.create(powerDataToResample.getSv(), powerDataToResample, powerData);
      assertArrayEquals(Utils.EMPTY_FLOAT_ARRAY, resampledArray.values());
   }
}
