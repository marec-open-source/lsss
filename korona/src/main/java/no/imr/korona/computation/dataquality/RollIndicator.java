package no.imr.korona.computation.dataquality;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.tools.Utils;
import no.imr.tools.math.ArrayKernel;
import no.imr.tools.parameter.Unit;
import no.imr.tools.time.TimeUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class RollIndicator extends DataQualityIndicator {
   private final int channel;
   private final List<Float> rollList = new ArrayList<>();

   RollIndicator(int channel) {
      super(new NcVariableInfo("roll_qc", Unit.DEGREES.formalName()));

      this.channel = channel;
   }

   @Override
   void processPing(Ping ping) {
      ChannelData channelData = ping.getChannelData(channel);
      float roll = channelData != null ? channelData.getRoll() : Float.NaN;
      rollList.add(roll);
   }

   @Override
   float[] computeResult(Instant[] instants, float[] bottomDepths) {
      float[] roll = Utils.toFloats(rollList);
      return compute(roll, instants, bottomDepths);
   }

   public static float[] compute(float[] y, Instant[] instants, float[] bottom) {
      float[] yCopy = ArrayKernel.createGaussian(1).smooth(y);
      int maxRadius = 150;  //For 1 sec ping-rate, 30 means (2 x 30 seconds =) 1 minute
      float soundSpeed = 1500; //For now, more accurate probably not needed

      for (int i = 0; i < y.length; i++) {
         int i_min = Math.max(i - maxRadius, 0);
         int i_max = Math.min(i + maxRadius + 1, yCopy.length);
         float maxDepth = Math.min(bottom[i], 200); //Minimum of bottom depth and 200 m

         //If bottom detection failed, bottom = 0m
         if (maxDepth < 10) {
            y[i] = Float.NaN;
            continue;
         }

         // y[i] = Max.of(yCopy, Math.max(i - maxRadius, 0), Math.min(i + maxRadius + 1, yCopy.length));
         //(Degrees) roll from first to last ping
         double totalRoll = 0;
         double totalTime = 0;
         for (int j = i_min; j < i_max - 1; j++) {
            float deltaRoll = Math.abs(yCopy[j] - yCopy[j + 1]);
            if (!Float.isNaN(deltaRoll)) {
               totalRoll += deltaRoll;
               totalTime += TimeUtils.toSeconds(instants[j], instants[j + 1]);
             }
            //Alt: totalRoll = Math.max(totalRoll, Math.abs(yCopy[j] - yCopy[j + 1]));
            //System.out.printf("  %5.2f - %5.2f => %5.2f => totalRoll=%5.2f, \n", yCopy[j], yCopy[j + 1], yCopy[j] - yCopy[j + 1], totalRoll);

         }

         // How many degrees roll from transmission util last scatter received (bottom or 200 m)
         // Time from transmission until bottom backscatter is received: time * (2 * maxDepth / soundSpeed),
         // ... so only a fraction of (2 * maxDepth) / soundSpeed of the totalRoll should count:
         y[i] = (float) (totalRoll / totalTime * (2 * maxDepth / soundSpeed));  //From average roll: max is larger
         //y[i] *= Math.sqrt(2);  //Closer to max roll based on RMS
         /*
         System.out.printf("totalRoll[%1d]=%5.2f, timeDiff=%5.2f, maxDepth=%4.1f, (i_min=%1d; i_max=%1d => 2*radius=%1d, tr=%5.2f\n",
               i, totalRoll, timeDiff, maxDepth, i_min, i_max, i_max - i_min, y[i]);
          */

         //Quality factor based on roll, provided 7 degrees transducers
         float maxAcceptableRoll = 3.5f;
         if (y[i] < 0.1) {  //Little roll -good quality
            // qc[i] = 1;
         } else if (y[i] < maxAcceptableRoll) {
            // qc[i] = 1 - y[i] / maxAcceptableRoll;
         } else {  //Much roll - unacceptable quality
            // qc[i] = 0;
         }
      }
      return y;
   }
}
