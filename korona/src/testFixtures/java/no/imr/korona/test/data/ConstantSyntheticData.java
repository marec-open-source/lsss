package no.imr.korona.test.data;

import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;

import java.util.Arrays;

/**
 * Simple implementation for testing.
 */
public class ConstantSyntheticData extends SyntheticData {
   private final float[] svArray;

   public ConstantSyntheticData() {
      this(10, 100);
   }

   public ConstantSyntheticData(int count, float sv) {
      svArray = new float[count];
      Arrays.fill(svArray, sv);
   }

   @Override
   public void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
      powerData.setSv(svArray.clone());
   }
}
