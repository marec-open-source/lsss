package no.imr.korona.data.formats.ek500;

import no.imr.tools.time.DateTimeMillis;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * EK500 time record.
 */
final class TimeRecord {
   static final int SIZE_ON_FILE = 4;

   private final int time; // HHMMSSXX

   TimeRecord(ByteBuffer byteBuffer) {
      time = byteBuffer.getInt();
   }

   @Override
   public String toString() {
      return Integer.toString(time);
   }

   int getTime() {
      return time;
   }

   Instant getInstant(IndexRecord indexRecord) {
      Instant instant = DateTimeMillis.toInstant(indexRecord.date, time * 10);
      if (time < 1_00_00_00 && indexRecord.time > 23_00_00) {
         return instant.plus(1, ChronoUnit.DAYS);
      }
      if (time > 23_00_00_00 && indexRecord.time < 1_00_00) {
         return instant.minus(1, ChronoUnit.DAYS);
      }
      return instant;
   }
}
