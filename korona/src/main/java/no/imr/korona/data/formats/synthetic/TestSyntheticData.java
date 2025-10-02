package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;

public class TestSyntheticData extends SyntheticData {
   public TestSyntheticData() {
   }

   @Override
   protected float getBottomDepth(PingIndex pingIndex, int channel) {
      float factor = (float) (1 - 0.25 * Math.sin(2 * Math.PI * pingIndex.getVesselDistance()));
      return 100 * factor;
   }

   private static int getCount(PingIndex pingIndex) {
      if (pingIndex.getPingNumber() / 10000 % 2 == 0) {
         return 1000;
      } else {
         return 2000;
      }
   }

   @Override
   protected void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
      float[] logSv = new float[getCount(pingIndex)];

      float fx = (float) (1 - 0.1 * Math.sin(2 * Math.PI * pingIndex.getVesselDistance() / 5));

      for (int i = 0; i < logSv.length; i++) {
         float depth = powerData.getSampleDepth(i);
         float fy = (float) (1 - 0.2 * Math.sin(2 * Math.PI * depth / 100));

         float a = depth / 500;
         float value = -20 * (1 - a) + -120 * a;
         logSv[i] = value * fx * fy;
      }

      powerData.setLogSv(logSv);
   }
}
