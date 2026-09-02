package no.imr.korona.computation.broadband.notchfilter;

import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.InstantParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueParameter;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.time.Instant;
import java.util.List;

public final class BroadbandTemporalNotchFilterConfig implements ParameterContainer, Comparable<BroadbandTemporalNotchFilterConfig> {
   public final FloatParameter rejectionFrequency = new FloatParameter(
         new Name(BroadbandNotchFiltersFileService.REJECTION_FREQUENCY_KHZ, "Rejection frequency"),
         0, Unit.KHZ, ValueConstraints.gte(0f),
         "Rejection frequency");

   public final FloatParameter bandwidth = new FloatParameter(
         new Name(BroadbandNotchFiltersFileService.BANDWIDTH_KHZ, "Bandwidth"),
         1, Unit.KHZ, ValueConstraints.gt(0f),
         "Bandwidth of frequencies around rejection frequency attenuated 3 dB or more");

   public final InstantParameter startTime = new InstantParameter(new Name("StartTime", "Start"));
   public final InstantParameter stopTime = new InstantParameter(new Name("StopTime", "Stop"));

   public BroadbandTemporalNotchFilterConfig() {
   }

   @Override
   public List<ValueParameter<?>> getParameters() {
      return List.of(rejectionFrequency, bandwidth, startTime, stopTime);
   }

   @Override
   public int compareTo(BroadbandTemporalNotchFilterConfig other) {
      int c = Float.compare(rejectionFrequency.getFloatValue(), other.rejectionFrequency.getFloatValue());
      if (c != 0) {
         return c;
      }
      c = Float.compare(bandwidth.getFloatValue(), other.bandwidth.getFloatValue());
      if (c != 0) {
         return c;
      }
      c = getEffectiveStartTime().compareTo(other.getEffectiveStartTime());
      if (c != 0) {
         return c;
      }
      return getEffectiveStopTime().compareTo(other.getEffectiveStopTime());
   }

   public BroadbandNotchFilterConfig getBroadbandNotchFilterConfig() {
      return new BroadbandNotchFilterConfig(1000 * rejectionFrequency.getFloatValue(), 1000 * bandwidth.getFloatValue());
   }

   public boolean isValid(Instant instant) {
      return !instant.isBefore(getEffectiveStartTime()) &&
            !instant.isAfter(getEffectiveStopTime());
   }

   private Instant getEffectiveStopTime() {
      return stopTime.getValue().orElse(Instant.MAX);
   }

   private Instant getEffectiveStartTime() {
      return startTime.getValue().orElse(Instant.MIN);
   }
}
