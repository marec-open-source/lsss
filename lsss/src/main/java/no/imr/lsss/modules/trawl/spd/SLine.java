package no.imr.lsss.modules.trawl.spd;

import no.imr.tools.range.FloatRange;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class SLine extends BaseLine {
   public final int year;
   final int country;
   final char shipCode;
   final String shipName;
   public final int month;
   public final int day;
   public final int stationNo;
   public final int serialNo;
   final char stationType;
   final String latitude;
   final String longitude;
   final char nsew;
   final char system;
   final String territory;
   final String bottomDepth;
   final String noOfTools;
   public final String toolCode;
   final String toolNumber;
   final String location;
   final String direction;
   final String speed;
   public final String startTime;
   public final double startLog;
   public final String stopTime;
   public final double distance;
   public final char state;
   public final char quality;
   final String maxFishDepth;
   final String minFishDepth;
   final String trawlOpening;
   final String stdDevTrawlOpening;
   final String doorSpread;
   final String stdDevDoorSpread;
   final String specialCode;
   final String wireLength;
   final char qualityMark;
   final String qualityProc;

   SLine(String line) {
      super('S', line);

      year = parseYear(substring(line, 1, 4));
      country = parseInt(substring(line, 4, 6));
      shipCode = charAt(line, 6);
      shipName = substring(line, 7, 13);
      month = parseInt(substring(line, 13, 15));
      day = parseInt(substring(line, 15, 17), 0);
      stationNo = parseInt(substring(line, 17, 21), 0);
      serialNo = parseInt(substring(line, 21, 26), 0);
      stationType = charAt(line, 26);
      latitude = substring(line, 27, 32);
      longitude = substring(line, 32, 38);
      nsew = charAt(line, 38);
      system = charAt(line, 39);
      territory = substring(line, 40, 42);
      location = substring(line, 42, 45);
      bottomDepth = substring(line, 45, 49);
      noOfTools = substring(line, 49, 51);
      toolCode = substring(line, 51, 55);
      toolNumber = substring(line, 55, 57);
      direction = substring(line, 57, 59);
      speed = substring(line, 59, 61);
      startTime = substring(line, 61, 65).replace(' ', '0');
      startLog = parseDouble(substring(line, 65, 69), Double.NaN);
      stopTime = substring(line, 69, 73).replace(' ', '0');
      distance = parseDouble(substring(line, 73, 76), Double.NaN);
      state = charAt(line, 76);
      quality = charAt(line, 77);
      maxFishDepth = substring(line, 78, 82).trim();
      minFishDepth = substring(line, 82, 86).trim();
      trawlOpening = substring(line, 86, 89);
      stdDevTrawlOpening = substring(line, 89, 91);
      doorSpread = substring(line, 91, 94);
      stdDevDoorSpread = substring(line, 94, 97);
      specialCode = substring(line, 97, 99);
      wireLength = substring(line, 99, 103);
      qualityMark = charAt(line, 121);
      qualityProc = substring(line, 122, 124);
   }

   private static int parseYear(String string) {
      int year = Integer.parseInt(string.trim());
      if (year < 500) {
         return year + 2000;
      } else {
         return year + 1000;
      }
   }

   public Instant parseTime(String time) {
      int hour = Integer.parseInt(time.substring(0, 2));
      int minute = Integer.parseInt(time.substring(2, 4));
      return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.UTC).toInstant();
   }

   public double getLongitude() {
      int factor = nsew == '1' || nsew == '3' ? -1 : 1;
      return factor * Integer.parseInt(longitude.trim()) / 10.0;
   }

   public double getLatitude() {
      int factor = nsew == '2' || nsew == '3' ? -1 : 1;
      return factor * Integer.parseInt(latitude.trim()) / 10.0;
   }

   public String getToolName() {
      if (toolCode.startsWith("31")) {
         return "Bottom trawl";
      }
      if (toolCode.startsWith("35")) {
         return "Pelagic trawl";
      }
      return "Trawl";
   }

   public FloatRange getFishingDepth() {
      return switch (parseInt(maxFishDepth, -1)) {
         case 9001 -> FloatRange.of(0, 0);
         case 9002 -> FloatRange.of(0, 20);
         case 9003 -> FloatRange.of(0, 40);
         case 9004 -> FloatRange.of(0, 60);
         case 9005 -> FloatRange.of(0, 80);
         case 9006 -> FloatRange.of(20, 20);
         case 9007 -> FloatRange.of(20, 40);
         case 9008 -> FloatRange.of(20, 60);
         case 9009 -> FloatRange.of(40, 40);
         case 9010 -> FloatRange.of(40, 60);
         case 9011 -> FloatRange.of(60, 60);
         case 9012 -> FloatRange.of(60, 80);
         case 9013 -> FloatRange.of(80, 80);
         case 9014 -> FloatRange.of(0, 30);
         case 9015 -> FloatRange.of(0, 50);
         default -> FloatRange.of(parseDouble(minFishDepth, 0), parseDouble(maxFishDepth, 0));
      };
   }

   public static String getQualityString(String state, String quality) {
      int x = parseInt(state, 1);   // x assumed to be 1 unless explicitly parsed otherwise.
      int y = parseInt(quality, 1); // y assumed to be 1 unless explicitly parsed otherwise.
      if (x == 1 && y <= 2) {
         return "OK";
      }
      if ((x >= 2 && x < 4) || y <= 3) {
         return "Problem";
      }
      if (x >= 4 || y >= 5) {
         return "Bad";
      }
      return quality;
   }
}
