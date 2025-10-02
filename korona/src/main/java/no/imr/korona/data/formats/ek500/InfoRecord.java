package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.DataException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * EK500 info record.
 */
public final class InfoRecord {
   private static final int MAX_VERSION = 10;

   private static final int EK500_SIZE_ON_FILE = 16 * 4;
   private static final int BEI_SIZE_ON_FILE = 18 * 4;

   int release;             /* File set format release number */
   public int nation;       /* Nation code */
   public int ship;         /* Ship code */
   int survey;              /* Survey code */
   int frequency;           /* Sounder frequency [Hertz] */
   int transceiver;         /* Transceiver number: 1, 2, 3 */
   int startDate;           /* Start date */
   int startTime;           /* Start time */
   float startLatitude;     /* Start position latitude [degree] */
   float startLongitude;    /* Start position longitude [degree] */
   float startDistance;     /* Start log distance [nautical mile] */
   int stopDate;            /* Stop date */
   int stopTime;            /* Stop time */
   float stopLatitude;      /* Stop position latitude [degree] */
   float stopLongitude;     /* Stop position longitude [degree] */
   float stopDistance;      /* Stop log distance [nautical mile] */
   float saPelagic;         /* 20/9-91: R Korneliussen. Used by BEI */
   float pstPelagic;        /* 20/9-91: R Korneliussen. Used by BEI */

   private final boolean bei;
   private final ByteOrder byteOrder;

   InfoRecord(Path file) throws IOException {
      byte[] bytes = Files.readAllBytes(file);
      bei = switch (bytes.length) {
         case EK500_SIZE_ON_FILE -> false;
         case BEI_SIZE_ON_FILE -> true;
         default -> throw new DataException("Wrong file size for " + file);
      };
      ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
      byteOrder = detectByteOrder(byteBuffer, file);

      release = byteBuffer.getInt();
      nation = byteBuffer.getInt();
      ship = byteBuffer.getInt();
      survey = byteBuffer.getInt();
      frequency = byteBuffer.getInt();
      transceiver = byteBuffer.getInt();
      startDate = byteBuffer.getInt();
      startTime = byteBuffer.getInt();
      startLatitude = byteBuffer.getFloat();
      startLongitude = byteBuffer.getFloat();
      startDistance = byteBuffer.getFloat();
      stopDate = byteBuffer.getInt();
      stopTime = byteBuffer.getInt();
      stopLatitude = byteBuffer.getFloat();
      stopLongitude = byteBuffer.getFloat();
      stopDistance = byteBuffer.getFloat();
      if (bei) {
         saPelagic = byteBuffer.getFloat();
         pstPelagic = byteBuffer.getFloat();
      } else {
         saPelagic = Float.NaN;
         pstPelagic = Float.NaN;
      }

      assert !byteBuffer.hasRemaining() : byteBuffer;
   }

   private static ByteOrder detectByteOrder(ByteBuffer byteBuffer, Path file) throws DataException {
      byteBuffer.order(ByteOrder.BIG_ENDIAN);
      if (isInvalidReleaseNumber(byteBuffer)) {
         byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
         if (isInvalidReleaseNumber(byteBuffer)) {
            throw new DataException("Cannot detect byte order for " + file);
         }
      }
      return byteBuffer.order();
   }

   private static boolean isInvalidReleaseNumber(ByteBuffer byteBuffer) {
      int release = byteBuffer.getInt(0);
      return release < 0 || release > MAX_VERSION;
   }

   public boolean isBEI() {
      return bei;
   }

   public int getFrequency() {
      return frequency;
   }

   public ByteOrder getByteOrder() {
      return byteOrder;
   }

   public void setStartNTDate(long ntDate) {
      startDate = EK500Utils.ntDateToDate(ntDate);
      startTime = EK500Utils.ntDateToTime(ntDate);
   }

   public void setStopNTDate(long ntDate) {
      stopDate = EK500Utils.ntDateToDate(ntDate);
      stopTime = EK500Utils.ntDateToTime(ntDate);
   }

   public long getStartNTDate() {
      return EK500Utils.dateTimeToNTDate(startDate, startTime);
   }

   public long getStopNTDate() {
      return EK500Utils.dateTimeToNTDate(stopDate, stopTime);
   }
}
