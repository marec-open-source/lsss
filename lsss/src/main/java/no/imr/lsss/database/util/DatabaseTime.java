package no.imr.lsss.database.util;

import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.database.tables.hibernate.BaseObservationTimeContainer;
import no.imr.tools.database.hibernate.BaseCompDatabaseObject;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;
import no.imr.tools.time.DateTimeMillis;

/**
 * Representation of date and time in LSSS database format.
 * Note: time and date in UTC.
 */
public final class DatabaseTime {
   private final long millis;
   private final int date;
   private final int time;

   /**
    * Creates a new DatabaseTime.
    *
    * @param millis the time in milliseconds
    */
   public DatabaseTime(long millis) {
      long roundedMillis = roundMillis(millis);
      this.millis = roundedMillis;
      DateTimeMillis dateTimeMillis = new DateTimeMillis(roundedMillis);
      date = dateTimeMillis.getDate();
      time = dateTimeMillis.getTime() / 10;
   }

   /**
    * Rounds a time to a time representable by the database resolution, 100th of a second.
    *
    * @param millis the time in milliseconds
    * @return millis - millis % 10
    */
   public static long roundMillis(long millis) {
      return millis - millis % 10;
   }

   /**
    * Convert a ping range to the corresponding time range.
    *
    * @param pingRange a ping range
    * @return a time range in milliseconds
    */
   public static Range<Long> toMillisRange(PingRange pingRange) {
      return new DefaultRange<>(roundMillis(pingRange.begin().getTimeInMillis()), roundMillis(pingRange.end().getTimeInMillis()));
   }

   /**
    * Converts date and time to milliseconds.
    *
    * @param date date in format "YYYYMMDD"
    * @param time time in format HHMMSSXX, where XX is hundreds of a second
    * @return milliseconds
    */
   public static long toMillis(int date, int time) {
      return DateTimeMillis.toMillis(date, time * 10);
   }

   public static long toMillis(BaseCompDatabaseObject<? extends BaseObservationTimeContainer> databaseObject) {
      return toMillis(databaseObject.getCompId());
   }

   public static long toMillis(BaseObservationTimeContainer observationTimeContainer) {
      return toMillis(observationTimeContainer.getObservationDate(), observationTimeContainer.getObservationTime());
   }

   /**
    * Returns the date/time in milliseconds.
    *
    * @return the date/time
    * @see System#currentTimeMillis
    */
   public long getMillis() {
      return millis;
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
