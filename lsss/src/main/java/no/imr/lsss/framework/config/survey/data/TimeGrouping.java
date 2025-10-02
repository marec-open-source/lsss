package no.imr.lsss.framework.config.survey.data;

import no.imr.tools.Utils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

enum TimeGrouping {
   DAYS("Days", Utils.createUTCDateTimeFormatter("yyyy-MM-dd EEEE")),
   HOURS("Hours", Utils.createUTCDateTimeFormatter("yyyy-MM-dd EEEE HH"));

   final String label;
   final DateTimeFormatter dateTimeFormatter;

   TimeGrouping(String label, DateTimeFormatter dateTimeFormatter) {
      this.label = label;
      this.dateTimeFormatter = dateTimeFormatter;
   }

   TimeGroup toTimeGroup(Instant instant) {
      if (instant.equals(Instant.MAX)) {
         return TimeGroup.MAX;
      }
      return new TimeGroup(truncate(instant));
   }

   private Instant truncate(Instant instant) {
      return switch (this) {
         case DAYS -> instant.truncatedTo(ChronoUnit.DAYS);
         case HOURS -> instant.truncatedTo(ChronoUnit.HOURS);
      };
   }
}
