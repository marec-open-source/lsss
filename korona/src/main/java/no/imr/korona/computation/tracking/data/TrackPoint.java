package no.imr.korona.computation.tracking.data;

import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

public final class TrackPoint {
   private final PingIndex pingIndex;
   private final TargetPoint prediction;
   private @Nullable Measurement measurement;
   private @Nullable TargetPoint estimate;
   private @Nullable FloatRange rangeRange;
   private int sampleCount;

   public TrackPoint(PingIndex pingIndex, TargetPoint prediction) {
      this.pingIndex = pingIndex;
      this.prediction = prediction;
   }

   @Override
   public String toString() {
      return "{" + pingIndex.getPingNumber() + ", " + prediction + ", " + measurement + ", " + estimate + "}";
   }

   public PingIndex getPingIndex() {
      return pingIndex;
   }

   public TargetPoint getPrediction() {
      return prediction;
   }

   public Measurement getMeasurement() {
      if (measurement == null) {
         throw new IllegalStateException();
      }
      return measurement;
   }

   public void setMeasurement(Measurement measurement) {
      this.measurement = measurement;
   }

   public @Nullable TargetPoint getEstimate() {
      return estimate;
   }

   public void setEstimate(TargetPoint estimate) {
      this.estimate = estimate;
   }

   public FloatRange getRangeRange() {
      if (rangeRange == null) {
         throw new IllegalStateException();
      }
      return rangeRange;
   }

   public void setRangeRange(FloatRange rangeRange) {
      this.rangeRange = rangeRange;
   }

   public int getSampleCount() {
      return sampleCount;
   }

   public void setSampleCount(int sampleCount) {
      this.sampleCount = sampleCount;
   }
}
