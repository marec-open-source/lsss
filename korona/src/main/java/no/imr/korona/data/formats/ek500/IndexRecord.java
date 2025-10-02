package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.DataException;
import no.imr.tools.Utils;
import no.imr.tools.time.DateTimeMillis;
import no.imr.tools.time.NTDate;

import java.nio.ByteBuffer;

/**
 * EK500 index record.
 * <p>
 * Typical values:
 * <pre>
 * Date: int = 20070427
 * Time: int = 81322
 * Distance: float = 9881.075
 * Latitude: float = 69.75386
 * Longitude: float = 17.288225
 * BottomDepth: float = 180.02003
 * EchogramType: int = 0
 * PelagicUpper: float = 0.0
 * PelagicLower: float = 500.0
 * PelagicCount: int = 500
 * PelagicOffset: int = 552700
 * BottomUpper: float = 10.0
 * BottomLower: float = -5.0
 * BottomCount: int = 150
 * BottomOffset: int = 553700
 * EchotraceCount: int = 0
 * EchotraceOffset: int = 554000
 * </pre>
 */
public final class IndexRecord {
   static final int SIZE_ON_FILE = 17 * 4;

   public int date;         /* Current date */
   public int time;         /* Current time */
   public float distance;   /* Current log distance [nautical mile] */
   float latitude;          /* Current position latitude [degree] */
   float longitude;         /* Current position longitude [degree] */
   float bottomDepth;       /* Current bottom depth [meter] */
   int echogramType;        /* Data type: 0 = Sv, 1 = TS, 2 = Ss */
   float pelagicUpper;      /* Upper boundary of main echogram [meter] */
   float pelagicLower;      /* Lower boundary of main echogram [meter] */
   int pelagicCount;        /* Number of main echogram data points */
   int pelagicOffset;       /* Offset of pelagic data [bytes] */
   float bottomUpper;       /* Upper boundary of bottom echogram [meter] */
   float bottomLower;       /* Lower boundary of bottom echogram [meter] */
   int bottomCount;         /* Number of bottom echogram data points */
   int bottomOffset;        /* Offset of bottom data [byte] */
   int echotraceCount;      /* Number of echo traces in current ping */
   int echotraceOffset;     /* Offset of trace data [byte] */

   private long ntDate;

   IndexRecord(ByteBuffer byteBuffer) throws DataException {
      date = byteBuffer.getInt();
      time = byteBuffer.getInt();
      distance = byteBuffer.getFloat();
      latitude = byteBuffer.getFloat();
      longitude = byteBuffer.getFloat();
      bottomDepth = byteBuffer.getFloat();
      echogramType = byteBuffer.getInt();
      pelagicUpper = byteBuffer.getFloat();
      pelagicLower = byteBuffer.getFloat();
      pelagicCount = byteBuffer.getInt();
      pelagicOffset = byteBuffer.getInt();
      bottomUpper = byteBuffer.getFloat();
      bottomLower = byteBuffer.getFloat();
      bottomCount = byteBuffer.getInt();
      bottomOffset = byteBuffer.getInt();
      echotraceCount = byteBuffer.getInt();
      echotraceOffset = byteBuffer.getInt();

      if (pelagicCount < 0) {
         throw new DataException("Pelagic count: " + pelagicCount);
      }
      if (bottomCount < 0) {
         throw new DataException("Bottom count: " + bottomCount);
      }
      if (pelagicLower < pelagicUpper) {
         throw new DataException("Pelagic range: " + pelagicLower + ", " + pelagicUpper);
      }
      if (bottomLower > bottomUpper) {
         throw new DataException("Bottom range: " + bottomLower + ", " + bottomUpper);
      }

      ntDate = EK500Utils.dateTimeToNTDate(date, time);
   }

   @Override
   public String toString() {
      return "Date: " + date
            + ", Time: " + Utils.format("%06d", time)
            + ", Distance: " + Utils.format("%8.3f", distance)
            + ", BottomDepth: " + Utils.format("%8.3f", bottomDepth)
            + ", PelagicOffset: " + pelagicOffset;
   }

   public int getTime() {
      return time;
   }

   void setNTDate(long ntDate) {
      this.ntDate = ntDate;
      DateTimeMillis dateTimeMillis = new DateTimeMillis(NTDate.ntDateToTimeInMillis(ntDate));
      date = dateTimeMillis.getDate();
      time = dateTimeMillis.getTime();
   }

   public long getNTDate() {
      return ntDate;
   }

   public float getPelagicEchogramMinDepth() {
      return pelagicUpper;
   }

   public float getPelagicEchogramMaxDepth() {
      return pelagicLower;
   }

   public float getBottomEchogramMinDepth() {
      return bottomDepth - bottomUpper;
   }

   public float getBottomEchogramMaxDepth() {
      return bottomDepth - bottomLower;
   }

   public float getPelagicEchogramSampleDistance() {
      return (pelagicLower - pelagicUpper) / pelagicCount;
   }

   public float getBottomEchogramSampleDistance() {
      return (bottomUpper - bottomLower) / bottomCount;
   }

   public boolean pelagicAndBottomOverlap() {
      return getBottomEchogramMinDepth() <= getPelagicEchogramMaxDepth();
   }

   public float calculateSpeedInKnots(IndexRecord reference) {
      float nauticalMiles = distance - reference.distance;
      float seconds = (float) (ntDate - reference.ntDate) / (float) NTDate.UNITS_PER_SECOND;
      float hours = seconds / 3600;
      return nauticalMiles / hours;
   }
}
