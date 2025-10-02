package no.imr.korona.computation.tracking.data;

import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

public final class TargetCandidate {
   private final int sampleIndex;
   private final int sampleCount;
   private final FloatRange rangeRange;
   private final Measurement measurement;
   private @Nullable Track track;
   private float gateDistanceSq = Float.POSITIVE_INFINITY;

   public TargetCandidate(int sampleIndex, int sampleCount, FloatRange rangeRange, Measurement measurement) {
      this.sampleIndex = sampleIndex;
      this.sampleCount = sampleCount;
      this.rangeRange = rangeRange;
      this.measurement = measurement;
   }

   @Override
   public String toString() {
      return "{" + sampleIndex + ", " + rangeRange + ", " + measurement + ", " + track + ", " + gateDistanceSq + "}";
   }

   public int getSampleIndex() {
      return sampleIndex;
   }

   public int getSampleCount() {
      return sampleCount;
   }

   public FloatRange getRangeRange() {
      return rangeRange;
   }

   public Measurement getMeasurement() {
      return measurement;
   }

   public @Nullable Track getTrack() {
      return track;
   }

   public void setTrack(Track track) {
      this.track = track;
   }

   public float getGateDistanceSq() {
      return gateDistanceSq;
   }

   public void setGateDistanceSq(float gateDistanceSq) {
      this.gateDistanceSq = gateDistanceSq;
   }
}
