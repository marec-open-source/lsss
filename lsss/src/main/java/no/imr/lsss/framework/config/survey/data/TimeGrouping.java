package no.imr.lsss.framework.config.survey.data;

import no.imr.tools.time.TimeUtils;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

enum TimeGrouping {
   DAYS("Days", TimeUtils.createUTCDateTimeFormatter("yyyy-MM-dd EEEE")),
   HOURS("Hours", TimeUtils.createUTCDateTimeFormatter("yyyy-MM-dd EEEE HH"));

   final String label;
   final DateTimeFormatter dateTimeFormatter;

   TimeGrouping(String label, DateTimeFormatter dateTimeFormatter) {
      this.label = label;
      this.dateTimeFormatter = dateTimeFormatter;
   }

   TimeGroup toTimeGroup(@Nullable Instant instant) {
      if (instant == null) {
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
