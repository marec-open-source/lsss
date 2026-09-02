package no.imr.korona.computation.broadband.splitting;

import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueParameter;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;
import java.util.Optional;

public final class BroadbandSplitterBand implements ParameterContainer, Comparable<BroadbandSplitterBand> {
   public final FloatParameter nominal = new FloatParameter(
         new Name(BroadbandSplitterBandsFileService.FREQUENCY_NOMINAL, "Nominal frequency"),
         0, Unit.KHZ, ValueConstraints.gte(0f),
         "Nominal frequency of split band");

   public final OptionalFloatParameter start = new OptionalFloatParameter(
         new Name(BroadbandSplitterBandsFileService.FREQUENCY_START, "Start frequency"),
         Optional.empty(), Unit.KHZ, ValueConstraints.gte(0f),
         "Start frequency of split band");

   public final OptionalFloatParameter stop = new OptionalFloatParameter(
         new Name(BroadbandSplitterBandsFileService.FREQUENCY_STOP, "Stop frequency"),
         Optional.empty(), Unit.KHZ, ValueConstraints.gte(0f),
         "Stop frequency of split band");

   public BroadbandSplitterBand() {
   }

   @Override
   public List<ValueParameter<?>> getParameters() {
      return List.of(nominal, start, stop);
   }

   @Override
   public int compareTo(BroadbandSplitterBand other) {
      int c = Float.compare(nominal.getFloatValue(), other.nominal.getFloatValue());
      if (c != 0) {
         return c;
      }
      c = Float.compare(start.getValue().orElse(Float.NEGATIVE_INFINITY), other.start.getValue().orElse(Float.NEGATIVE_INFINITY));
      if (c != 0) {
         return c;
      }
      return Float.compare(stop.getValue().orElse(Float.POSITIVE_INFINITY), other.stop.getValue().orElse(Float.POSITIVE_INFINITY));
   }
}
