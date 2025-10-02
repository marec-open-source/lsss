package no.imr.korona.data.formats.ek500;

import no.imr.tools.time.DateTimeMillis;
import no.imr.tools.time.NTDate;

import java.nio.ByteBuffer;

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

   long getNTDate(IndexRecord indexRecord) {
      return NTDate.timeInMillisToNTDate(DateTimeMillis.toMillis(indexRecord.date, time * 10));
   }
}
