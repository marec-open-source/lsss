package no.imr.lsss.database.util;

import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.database.tables.hibernate.BaseObservationTimeContainer;
import no.imr.tools.database.hibernate.BaseCompDatabaseObject;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;
import no.imr.tools.time.DateTimeMillis;

import java.time.Instant;

/**
 * Representation of date and time in LSSS database format.
 * Note: time and date in UTC.
 */
public final class DatabaseTime {
   private final Instant instant;
   private final int date;
   private final int time;

   public DatabaseTime(Instant instant) {
      Instant truncatedInstant = truncatedInstant(instant);
      this.instant = truncatedInstant;
      DateTimeMillis dateTimeMillis = new DateTimeMillis(truncatedInstant);
      date = dateTimeMillis.getDate();
      time = dateTimeMillis.getTime() / 10;
   }

   /**
    * Truncates a time to a time representable by the database resolution, 100th of a second.
    *
    * @param instant the time
    * @return the truncated time
    */
   public static Instant truncatedInstant(Instant instant) {
      int excessNanos = instant.getNano() % 10_000_000;
      return excessNanos == 0 ? instant : instant.minusNanos(excessNanos);
   }

   public static Range<Instant> toTimeRange(PingRange pingRange) {
      return new DefaultRange<>(truncatedInstant(pingRange.begin().getInstant()), truncatedInstant(pingRange.end().getInstant()));
   }

   /**
    * Converts date and time to an instant.
    *
    * @param date date in format "YYYYMMDD"
    * @param time time in format HHMMSSXX, where XX is hundreds of a second
    * @return an instant
    */
   public static Instant toInstant(int date, int time) {
      return DateTimeMillis.toInstant(date, time * 10);
   }

   public static Instant toInstant(BaseCompDatabaseObject<? extends BaseObservationTimeContainer> databaseObject) {
      return toInstant(databaseObject.getCompId());
   }

   public static Instant toInstant(BaseObservationTimeContainer observationTimeContainer) {
      return toInstant(observationTimeContainer.getObservationDate(), observationTimeContainer.getObservationTime());
   }

   public Instant getInstant() {
      return instant;
   }

   /**
    * Returns the date in format "YYYYMMDD".
    *
    * @return the date
    */
   public int getDate() {
      return date;
   }

   /**
    * Returns the time in format HHMMSSXX, where XX is hundreds of a second.
    *
    * @return the time
    */
   public int getTime() {
      return time;
   }
}
