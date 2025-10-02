package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.Validator;
import no.imr.korona.computation.tracking.data.Track;
import no.imr.korona.computation.tracking.data.TrackPoint;

public final class SimpleValidator implements Validator {
   private final float maxMissingPingsFraction;
   private final int minTrackLength;
   private final float minSampleToLengthFraction;

   public SimpleValidator(float maxMissingPingsFraction, int minTrackLength, float minSampleToLengthFraction) {
      this.maxMissingPingsFraction = maxMissingPingsFraction;
      this.minTrackLength = minTrackLength;
      this.minSampleToLengthFraction = minSampleToLengthFraction;
   }

   @Override
   public boolean isValid(Track track) {
      int missingPingAtEnd = (int) (track.getLastPoint().getPingIndex().getPingNumber() - track.getLastPointWithEstimate().getPingIndex().getPingNumber());
      float length = track.getPoints().size() - missingPingAtEnd;
      if (length < minTrackLength) {
         return false;
      }

      int missingPings = 0;
      int sampleCount = 0;
      for (TrackPoint point : track.getPoints()) {
         if (point.getEstimate() == null) {
            missingPings++;
         }
         sampleCount += point.getSampleCount();
      }
      missingPings -= missingPingAtEnd;
      float missingFraction = missingPings / length;
      float sampleToLengthFraction = sampleCount / length;
      return missingFraction <= maxMissingPingsFraction && sampleToLengthFraction >= minSampleToLengthFraction;
   }
}
