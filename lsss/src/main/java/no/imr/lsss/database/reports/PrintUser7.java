package no.imr.lsss.database.reports;

import no.imr.lsss.database.util.DatabaseTime;
import no.imr.tools.Utils;
import no.imr.tools.io.Print;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Print PGNAPES format - file-type 1.
 */
final class PrintUser7 extends BaseOneSpeciesPerFileReport {
   PrintUser7(ReportEngine reportEngine) {
      super(7, reportEngine);
   }

   @Override
   void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int printType, ReportMode aMode) {
      aPrintWriter.print("Country"
            + "\tVessel"
            + "\tCruise"
            + "\tLog"
            + "\tYear"
            + "\tMonth"
            + "\tDay"
            + "\tHour"
            + "\tMin"
            + "\tAcLat"
            + "\tAcLon"
            + "\tLogint"
            + "\tFrequency"
            + "\tSv_threshold");
      aPrintWriter.println('\t');
   }

   @Override
   void print(
         PrintWriter aPrintWriter,
         PrintData.Pelagic aPrintData,
         PrintData.Bottom aPrintDataBottom,
         ReportMode aMode,
         int aAcousticCategoryIndex                 // Acoustic category
   ) {
      String country = GetPgnapes.nation(aPrintData.getNation());
      String callsign = GetPgnapes.callsign(aPrintData.getPlatform());
      ZonedDateTime time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(DatabaseTime.toMillis(aPrintData.getScatter(aMode))), ZoneOffset.UTC);

      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df1 = new DecimalFormat("#0.0", dfs);
      DecimalFormat df3 = new DecimalFormat("#0.000", dfs);

      // General information
      aPrintWriter.print(country);
      Print.tabAndValue(aPrintWriter, callsign);
      Print.tabAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getSurvey());
      Print.tabAndValue(aPrintWriter, df1.format(aPrintData.getObservation(aMode).getDistance()));
      Print.tabAndValue(aPrintWriter, time.getYear());
      Print.tabAndValue(aPrintWriter, String.format("%02d", time.getMonthValue()));
      Print.tabAndValue(aPrintWriter, String.format("%02d", time.getDayOfMonth()));
      Print.tabAndValue(aPrintWriter, String.format("%02d", time.getHour()));
      Print.tabAndValue(aPrintWriter, String.format("%02d", time.getMinute()));
      Print.tabAndValue(aPrintWriter, df3.format(aPrintData.getObservation(aMode).getLatitude()));
      Print.tabAndValue(aPrintWriter, df3.format(aPrintData.getObservation(aMode).getLongitude()));
      Print.tabAndValue(aPrintWriter, df1.format(aPrintData.getScatter(aMode).getDistanceInterval()));
      Print.tabAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getFrequency());
      Print.tabAndValue(aPrintWriter, df1.format(aPrintData.getScatter(aMode).getThreshold()));

      aPrintWriter.println('\t');
   } // print()
}
