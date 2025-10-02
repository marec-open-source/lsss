package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.TargetCandidateExtractor;
import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.TargetCandidate;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.util.ts.TSDetection;
import no.imr.korona.util.ts.TSDetector;
import no.imr.tools.range.FloatRange;

import java.util.ArrayList;
import java.util.List;

public final class TsDetectorCandidateExtractor implements TargetCandidateExtractor {
   private final int detectionChannel;
   private final TSDetector tsDetector;

   public TsDetectorCandidateExtractor(int detectionChannel, TSDetector tsDetector) {
      this.detectionChannel = detectionChannel;
      this.tsDetector = tsDetector;
   }

   @Override
   public List<TargetCandidate> extract(Ping ping, FloatRange depthRange) {
      ChannelData channelData = ping.getChannelData(detectionChannel);
      if (channelData == null) {
         return List.of();
      }
      List<TargetCandidate> targetCandidates = new ArrayList<>();
      for (TSDetection tsDetection : tsDetector.getAcceptedTsDetections(ping, channelData, depthRange)) {
         int startSample = tsDetection.beginIndex();
         int peakSample = tsDetection.peakIndex();
         Measurement measurement;
         if (channelData instanceof BroadbandData broadbandData) {
            measurement = new Measurement(broadbandData.getSampleRange(peakSample),
                  (float) Math.toRadians(broadbandData.getMechanicalAlongAngle(peakSample, broadbandData.getCenterFrequency())),
                  (float) Math.toRadians(broadbandData.getMechanicalAthwartAngle(peakSample, broadbandData.getCenterFrequency())),
                  broadbandData.getTSC(peakSample, broadbandData.getCenterFrequency()));
         } else {
            PowerData powerData = channelData.getPowerData();
            measurement = new Measurement(powerData.getSampleRange(peakSample),
                  (float) Math.toRadians(powerData.getMechanicalAlongAngle(peakSample)),
                  (float) Math.toRadians(powerData.getMechanicalAthwartAngle(peakSample)),
                  powerData.getTSC(peakSample));
         }
         int sampleCount = tsDetection.getSampleCount();
         float minRange = channelData.getSampleRange(startSample);
         float maxRange = channelData.getSampleRange(startSample + sampleCount);
         targetCandidates.add(new TargetCandidate(startSample, sampleCount, FloatRange.of(minRange, maxRange), measurement));
      }
      return targetCandidates;
   }
}
