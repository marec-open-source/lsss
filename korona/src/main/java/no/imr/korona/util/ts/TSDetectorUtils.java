package no.imr.korona.util.ts;

import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import org.jspecify.annotations.Nullable;

final class TSDetectorUtils {
   private TSDetectorUtils() {
   }

   static boolean checkEchoLimits(TSDetection tsDetection, ChannelData channelData, float minEchoLength, float maxEchoLength) {
      float length = tsDetection.getSampleCount() * channelData.getSampleInterval();
      float pulseDuration = channelData.getPulseDuration();
      return length >= (pulseDuration * minEchoLength) &&
            length <= (pulseDuration * maxEchoLength);
   }

   static boolean checkPhaseSamplesPhaseSteps(TSDetection tsDetection, @Nullable AngleData angleData,
                                              boolean doPhaseDeviationCheck, float maxPhaseSteps) {
      if (angleData == null) {
         return true;
      }
      if (!doPhaseDeviationCheck) {
         return true;
      }
      if (tsDetection.getSampleCount() == 1) {
         return true;
      }

      double averageAlongshipAngle = 0;
      double averageAthwartshipAngle = 0;
      for (int i = tsDetection.beginIndex(); i < tsDetection.beginIndex() + tsDetection.getSampleCount(); i++) {
         averageAlongshipAngle += angleData.getElectricalAlongAngle(i);
         averageAthwartshipAngle += angleData.getElectricalAthwartAngle(i);
      }
      averageAlongshipAngle /= tsDetection.getSampleCount();
      averageAthwartshipAngle /= tsDetection.getSampleCount();

      double stdDevAlongship = 0;
      double stdDevAthwartship = 0;
      for (int i = tsDetection.beginIndex(); i < tsDetection.beginIndex() + tsDetection.getSampleCount(); i++) {
         double along = angleData.getElectricalAlongAngle(i) - averageAlongshipAngle;
         stdDevAlongship += along * along;
         double athwart = angleData.getElectricalAthwartAngle(i) - averageAthwartshipAngle;
         stdDevAthwartship += athwart * athwart;
      }
      stdDevAlongship /= tsDetection.getSampleCount() - 1;
      stdDevAthwartship /= tsDetection.getSampleCount() - 1;
      stdDevAlongship = Math.sqrt(stdDevAlongship);
      stdDevAthwartship = Math.sqrt(stdDevAthwartship);

      return stdDevAlongship < maxPhaseSteps * AngleData.BYTE_TO_ELECTRICAL_ANGLE
            && stdDevAthwartship < maxPhaseSteps * AngleData.BYTE_TO_ELECTRICAL_ANGLE;
   }

   static float computeOneWayGainCompensation(TSDetection tsDetection, @Nullable AngleData angleData, RawFileTransducer transducer) {
      if (angleData == null || angleData.getElectricalAngles().length <= 2 * tsDetection.peakIndex() + 1) {
         return 0;
      }
      return 0.5f * angleData.getDirectivityCorrection(tsDetection.peakIndex(), transducer);
   }
}
