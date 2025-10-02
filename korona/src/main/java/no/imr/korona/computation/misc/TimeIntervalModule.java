package no.imr.korona.computation.misc;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.Utils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalIntParameter;
import no.imr.tools.parameter.OptionalParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueConverter;
import no.imr.tools.parameter.ValueConverters;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Module for processing only the datagrams in a specified time interval.
 * The start and end dates are in the same format as is displayed in the upper right
 * in the KORONA main window. The empty string implies no restriction.
 */
public final class TimeIntervalModule extends GeneralPingModule {
   private static final String DATE_FORMAT = "dd.MM.yyyy HH:mm:ss";
   private static final Unit UNIT = new Unit(DATE_FORMAT);
   private static final DateTimeFormatter DATE_TIME_FORMATTER = Utils.createUTCDateTimeFormatter(DATE_FORMAT);

   private final HeaderParameter timeHeader = new HeaderParameter("Time limits");

   public final DateParameter startDate = new DateParameter(
         new Name("StartDateString", "Start date"),
         "The minimum date and time");

   public final DateParameter endDate = new DateParameter(
         new Name("EndDateString", "End date"),
         "The maximum date and time");

   private final HeaderParameter pingIndexHeader = new HeaderParameter("Relative ping number limits");

   public final OptionalIntParameter startRelativePingNumber = new OptionalIntParameter(
         new Name("StartRelativePingNumber", "Start relative ping number"),
         Optional.empty(), Unit.COUNT,
         "The minimum relative ping number within a file, starting at 1");

   public final OptionalIntParameter endRelativePingNumber = new OptionalIntParameter(
         new Name("EndRelativePingNumber", "End relative ping number"),
         Optional.empty(), Unit.COUNT,
         "The maximum relative ping number within a file, starting at 1");

   public TimeIntervalModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            timeHeader,
            startDate,
            endDate,
            //---
            pingIndexHeader,
            startRelativePingNumber,
            endRelativePingNumber
      );
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new TimeIntervalModuleComputation(this, computationContext, pingSource);
   }

   public static final class DateParameter extends OptionalParameter<Instant> {
      private static final ValueConverter<Optional<Instant>> CONVERTER = ValueConverters.optional(ValueConverters.of(
            string -> DATE_TIME_FORMATTER.parse(string, Instant::from),
            DATE_TIME_FORMATTER::format
      ));

      private DateParameter(Name name, String description) {
         super(name, Optional.empty(), UNIT, description, CONVERTER);
      }

      @Override
      public String getAllowedValuesDescription() {
         return DATE_FORMAT;
      }

      long toMillisOrDefault(long defaultValue) {
         if (getValue().isPresent()) {
            return getValue().get().toEpochMilli();
         }
         return defaultValue;
      }
   }
}
