package no.imr.tools.parameter;

import no.imr.tools.time.DateTimeMillis;

import java.time.LocalTime;
import java.util.Optional;

/**
 * A time in format HH:MM[:SS[:XX]].
 */
public class TimeParameter extends OptionalParameter<LocalTime> {
   private static final Unit UNIT = new Unit("HH:MM:SS:XX");
   private static final ValueConverter<Optional<LocalTime>> CONVERTER = ValueConverters.optional(ValueConverters.of(
         DateTimeMillis::centisTimeToLocalTime,
         DateTimeMillis::localTimeToCentisString
   ));

   public TimeParameter(Name name) {
      super(name, Optional.empty(), UNIT, "", CONVERTER);
   }

   public void setValue(LocalTime localTime) {
      setValue(Optional.of(localTime));
   }

   public void setIntValue(int time) {
      setValue(DateTimeMillis.toLocalTime(time * 10));
   }

   public int getIntValue() {
      return getValue().isPresent() ? DateTimeMillis.localTimeToInt(getValue().get()) / 10 : 0;
   }

   @Override
   public String getAllowedValuesDescription() {
      return "HH:MM[:SS[:XX]]";
   }
}
