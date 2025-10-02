package no.imr.tools.parameter;

import java.time.Instant;
import java.util.Optional;

/**
 * A parameter representing an {@link Instant}.
 */
public class InstantParameter extends OptionalParameter<Instant> {
   private static final Unit UNIT = new Unit("YYYY-MM-DDTHH:MM:SSZ");
   private static final ValueConverter<Optional<Instant>> CONVERTER = ValueConverters.optional(ValueConverters.of(Instant::parse, Instant::toString));

   public InstantParameter(Name name) {
      super(name, Optional.empty(), UNIT, "", CONVERTER);
   }

   public InstantParameter(Name name, Instant initialValue) {
      super(name, Optional.of(initialValue), UNIT, "", CONVERTER);
   }

   @Override
   public String getAllowedValuesDescription() {
      return "YYYY-MM-DDTHH:MM:SSZ";
   }

   public long getTimeInMillis(long defaultValue) {
      return getValue()
            .map(Instant::toEpochMilli)
            .orElse(defaultValue);
   }

   public void setInstant(Instant instant) {
      setValue(Optional.of(instant));
   }
}
