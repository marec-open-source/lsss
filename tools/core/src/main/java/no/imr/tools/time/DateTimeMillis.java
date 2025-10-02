package no.imr.tools.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Date and time in format YYYYMMDD and HHMMSSXXX, where XXX are milliseconds.
 * <p>
 * Note: date and time in UTC.
 */
public final class DateTimeMillis {
   private final int date;
   private final int time;

   /**
    * Creates a new DateTime, using UTC.
    *
    * @param millis the time in milliseconds
    */
   public DateTimeMillis(long millis) {
      ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
      date = localDateToInt(dateTime.toLocalDate());
      time = localTimeToInt(dateTime.toLocalTime());
   }

   public static long toMillis(int date, int time) {
      return ZonedDateTime.of(toLocalDate(date).get(), toLocalTime(time), ZoneOffset.UTC).toInstant().toEpochMilli();
   }

   /**
    * Returns the date in format YYYYMMDD.
    *
    * @return the date
    */
   public int getDate() {
      return date;
   }

   /**
    * Returns the time in format HHMMSSXXX, where XXX are milliseconds.
    *
    * @return the time
    */
   public int getTime() {
      return time;
   }

   // Time conversions

   public static LocalTime toLocalTime(int time) {
      int hour = time / 100_00_000;
      time -= hour * 100_00_000;
      int minute = time / 100_000;
      time -= minute * 100_000;
      int second = time / 1000;
      time -= second * 1000;
      int millisecond = time;
      return LocalTime.of(hour, minute, second, millisecond * 1_000_000);
   }

   public static int localTimeToInt(LocalTime localTime) {
      return localTime.getHour() * 100_00_000 +
            localTime.getMinute() * 100_000 +
            localTime.getSecond() * 1000 +
            localTime.getNano() / 1_000_000;
   }

   public static int localTimeToInt(Optional<LocalTime> localTime) {
      return localTime.isPresent() ? localTimeToInt(localTime.get()) : 0;
   }

   public static Optional<LocalTime> centisTimeToLocalTime(String stringValue) {
      String s = stringValue.replaceAll("\\D+", "");
      if (s.isEmpty()) {
         return Optional.empty();
      }
      int time = Integer.parseInt(s);
      int digits = s.length();
      if (s.length() % 2 != 0) {
         digits++;
      }
      while (digits < 8) {
         time *= 100;
         digits += 2;
      }

      return Optional.of(toLocalTime(time * 10));
   }

   public static int centisTimeToInt(String stringValue) {
      return localTimeToInt(centisTimeToLocalTime(stringValue)) / 10;
   }

   public static String centisTimeToString(int time) {
      return localTimeToCentisString(toLocalTime(time * 10));
   }

   public static String localTimeToCentisString(Optional<LocalTime> localTime) {
      return localTime.isPresent() ? localTimeToCentisString(localTime.get()) : "";
   }

   public static String localTimeToCentisString(LocalTime localTime) {
      int hh = localTime.getHour();
      int mm = localTime.getMinute();
      int ss = localTime.getSecond();
      int xx = localTime.getNano() / 10_000_000;
      StringBuilder s = new StringBuilder(11)
            .append(String.format("%02d:%02d", hh, mm));
      if (ss != 0 || xx != 0) {
         s.append(String.format(":%02d", ss));
      }
      if (xx != 0) {
         s.append(String.format(":%02d", xx));
      }
      return s.toString();
   }

   // Date conversions

   public static Optional<LocalDate> toLocalDate(String dateString) {
      String s = dateString.replaceAll("\\D+", "");
      if (s.isEmpty()) {
         return Optional.empty();
      }
      int date = Integer.parseInt(s);
      return toLocalDate(date);
   }

   public static Optional<LocalDate> toLocalDate(int date) {
      if (date == 0) {
         return Optional.empty();
      }
      int year = date / 100_00;
      date -= year * 100_00;
      int month = date / 100;
      date -= month * 100;
      int day = date;
      return Optional.of(LocalDate.of(year, month, day));
   }

   public static int localDateToInt(LocalDate localDate) {
      return localDate.getYear() * 100_00 +
            localDate.getMonthValue() * 100 +
            localDate.getDayOfMonth();
   }

   public static int localDateToInt(Optional<LocalDate> localDate) {
      return localDate.isPresent() ? localDateToInt(localDate.get()) : 0;
   }

   public static String localDateToString(Optional<LocalDate> localDate) {
      return localDate.isPresent() ? localDateToString(localDate.get()) : "";
   }

   public static String localDateToString(LocalDate localDate) {
      return DateTimeFormatter.ISO_LOCAL_DATE.format(localDate);
   }

   public static int stringDateToInt(String date) {
      return localDateToInt(toLocalDate(date));
   }

   public static String intDateToString(int date) {
      return localDateToString(toLocalDate(date));
   }
}
