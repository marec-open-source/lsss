package no.imr.lsss.database.reports;

import no.imr.lsss.LSSS;
import no.imr.tools.Utils;

import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class ReportUtils {
   public static final DateTimeFormatter DATE_TIME = Utils.createUTCDateTimeFormatter("yyyy-MM-dd HH:mm:ss");
   public static final DateTimeFormatter DATE = Utils.createUTCDateTimeFormatter("yyyy.MM.dd");
   public static final DateTimeFormatter TIME = Utils.createUTCDateTimeFormatter("H:mm");
   public static final DateTimeFormatter TIME_LONG = Utils.createUTCDateTimeFormatter("HH:mm:ss");

   private ReportUtils() {
   }

   public static void writeStandardHeader(PrintWriter printWriter, String formatSinceLsssVersion) {
      printWriter.println("% Lines starting with % are comments");
      printWriter.println("% The format of and definitions in this file may change in future versions of LSSS,"
            + " last changed in LSSS version " + formatSinceLsssVersion);
      printWriter.println("% Export time: " + new Date() + ", LSSS version " + LSSS.VERSION);
      printWriter.println("%");
   }

   /**
    * Convert decimal latitude.
    *
    * @param latitude input decimal latitude
    * @return string latitude
    */
   static String latitudeString(float latitude) {
      String nOrS;
      if (latitude < 0) {
         nOrS = "S";
         latitude = -latitude;
      } else {
         nOrS = "N";
      }
      // Here: latitude is in decimal degrees, always positive.
      int degrees = (int) latitude;
      float minutes = (latitude - degrees) * 60;
      return Utils.format("%s%02d:%04.1f", nOrS, degrees, minutes);
   }

   /**
    * Convert decimal longitude.
    *
    * @param longitude input decimal longitude
    * @return string longitude
    */
   static String longitudeString(float longitude) {
      String eOrW;
      if (longitude < 0) {
         eOrW = "W";
         longitude = -longitude;
      } else {
         eOrW = "E";
      }
      // Here: longitude is in decimal degrees, always positive.
      int degrees = (int) longitude;
      float minutes = (longitude - degrees) * 60;
      return Utils.format("%s%03d:%04.1f", eOrW, degrees, minutes);
   }

   /**
    * Calculates difference in time.
    *
    * @param startTime input time in format hhmmsshh
    * @param stopTime  input time in format hhmmsshh
    * @return returns time difference in hundredths of a second
    */
   public static int diffTime(int startTime, int stopTime) {
      return timeToHundredths(stopTime) - timeToHundredths(startTime);
   } /*diffTime()*/

   /**
    * Converts time hhmmssxx to hundredths.
    *
    * @param time input time in format hhmmssxx
    * @return returns time difference in hundredths of a second
    */
   public static int timeToHundredths(int time) {
      int hh = time / 1000000;
      int rest = time - hh * 1000000;
      int mm = rest / 10000;
      rest = rest - mm * 10000;
      int ss = rest / 100;
      rest = rest - ss * 100;
      int xx = rest;

      return hh * 60 * 60 * 100 + mm * 60 * 100 + ss * 100 + xx;
   } /*TimeToHundredths()*/
}
