package no.imr.tools.parameter;

import no.imr.tools.time.DateTimeMillis;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A date in format YYYY-MM-DD.
 */
public class DateParameter extends OptionalParameter<LocalDate> {
   private static final Unit UNIT = new Unit("YYYY-MM-DD");
   private static final ValueConverter<Optional<LocalDate>> CONVERTER = ValueConverters.of(DateTimeMillis::toLocalDate, DateTimeMillis::localDateToString);

   public DateParameter(Name name) {
      super(name, Optional.empty(), UNIT, "", CONVERTER);
   }

   @Override
   public String getAllowedValuesDescription() {
      return "YYYY-MM-DD";
   }

   public void setValue(LocalDate localDate) {
      setValue(Optional.of(localDate));
   }

   public void setIntValue(int date) {
      setValue(DateTimeMillis.toLocalDate(date));
   }

   public int getIntValue() {
      return DateTimeMillis.localDateToInt(getValue());
   }

   public static void setFromMillis(long timeInMillis, DateParameter dateParameter, TimeParameter timeParameter) {
      ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeInMillis), ZoneOffset.UTC);
      dateParameter.setValue(dateTime.toLocalDate());
      timeParameter.setValue(dateTime.toLocalTime());
   }
}
