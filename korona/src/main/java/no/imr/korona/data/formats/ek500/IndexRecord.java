package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.DataException;
import no.imr.tools.Utils;

import java.nio.ByteBuffer;
import java.time.Instant;

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

   private Instant instant;

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

      try {
         instant = EK500Utils.dateTimeToInstant(date, time);
      } catch (Exception e) {
         throw new DataException("Cannot parse date and time " + date + " " + time, e);
      }
   }

   @Override
   public String toString() {
      return "Date: " + date
            + ", Time: " + Utils.format("%06d", time)
            + ", Distance: " + Utils.format("%8.3f", distance)
            + ", BottomDepth: " + Utils.format("%8.3f", bottomDepth)
            + ", PelagicOffset: " + pelagicOffset;
   }

   void setInstant(Instant instant) {
      this.instant = instant;
      date = EK500Utils.instantToDate(instant);
      time = EK500Utils.instantToTime(instant);
   }

   public Instant getInstant() {
      return instant;
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
}
