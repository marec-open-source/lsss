package no.imr.tools.time;

import no.imr.tools.Utils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Locale;

public final class TimeUtils {
   public static final DateTimeFormatter JAVA_UTIL_DATE_FORMATTER = createLocalDateTimeFormatter("EEE MMM dd HH:mm:ss zzz yyyy");

   private TimeUtils() {
   }

   public static DateTimeFormatter createUTCDateTimeFormatter(String pattern) {
      return DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).withZone(ZoneOffset.UTC);
   }

   public static DateTimeFormatter createLocalDateTimeFormatter(String pattern) {
      return DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).withZone(ZoneId.systemDefault());
   }

   public static String getDurationString(Duration duration) {
      return Utils.format("%d:%02d:%02d",
            duration.toHours(),
            duration.toMinutesPart(),
            duration.toSecondsPart()
      );
   }

   public static double toSeconds(Instant from, Instant to) {
      long secsDiff = to.getEpochSecond() - from.getEpochSecond();
      int nanosDiff = to.getNano() - from.getNano();
      return secsDiff + nanosDiff / 1e9;
   }

   public static double toSeconds(Duration duration) {
      return duration.getSeconds() + duration.getNano() / 1e9;
   }

   public static Duration secondsToDuration(double seconds) {
      long secs = (long) seconds;
      long nanos = Math.round((seconds - secs) * 1e9);
      return Duration.ofSeconds(secs, nanos);
   }

   public static Instant roundedTo(Instant instant, TemporalUnit unit) {
      Instant truncated = instant.truncatedTo(unit);
      if (truncated.until(instant, ChronoUnit.NANOS) >= unit.getDuration().toNanos() / 2) {
         return truncated.plus(1, unit);
      } else {
         return truncated;
      }
   }

   public static Instant ceiledTo(Instant instant, TemporalUnit unit) {
      Instant truncated = instant.truncatedTo(unit);
      if (truncated.isBefore(instant)) {
         return truncated.plus(1, unit);
      } else {
         return truncated;
      }
   }

   public static Instant interpolateInstant(Instant a, Instant b, double fractionFromAToB) {
      return a.plusNanos(Math.round(fractionFromAToB * a.until(b, ChronoUnit.NANOS)));
   }
}
