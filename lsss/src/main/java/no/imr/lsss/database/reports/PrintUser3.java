package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.tools.Utils;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;

final class PrintUser3 extends BaseMultipleSpeciesPerFileReport {
   PrintUser3(ReportEngine reportEngine) {
      super(3, reportEngine);
   }

   @Override
   void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int printType, ReportMode aMode) {
      ReportUtils.writeStandardHeader(aPrintWriter, "2.17.0");
      aPrintWriter.print("SHIP");
      aPrintWriter.print(", NATION");
      aPrintWriter.print(", SURVEY");
      aPrintWriter.print(", FREQUENCY");
      aPrintWriter.print(", TR");
      aPrintWriter.print(", DATE");
      aPrintWriter.print(", TIME");
      aPrintWriter.print(", LOGSTART");
      aPrintWriter.print(", LOGSTOP");
      aPrintWriter.print(", LONGITUDE");
      aPrintWriter.print(", LATITUDE");
      aPrintWriter.print(", DEPTH");

      // Species already ordered. Initials for "TOTAL" printed below
      for (int i = 0; i < aPrintData.getPrintCount(); i++) {
         aPrintWriter.print(", " + getLanguageUtils().getAcCatInitials(aPrintData.getAcousticCategory(i)));
      }

      aPrintWriter.println(", TOTAL");
   }

   @Override
   void print(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int aPrintFrequency, short aPrintTransceiver, ReportMode aMode) {
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df1 = new DecimalFormat("#0.0", dfs);
      DecimalFormat df2 = new DecimalFormat("#0.00", dfs);
      DecimalFormat df3 = new DecimalFormat("#0.000", dfs);
      DecimalFormat df5 = new DecimalFormat("#0.00000", dfs);

      Instant time = Instant.ofEpochMilli(DatabaseTime.toMillis(aPrintData.getScatter(aMode)));

      Scatter scat = aPrintData.getScatter(aMode);
      float[][] dataPrint = aPrintData.getSa(aMode);

      // Nation, ship, survey, frequency, transceiver
      aPrintWriter.print(aPrintData.getSurvey().getPlatform().findPlatformName(aPrintData.getSurvey()));
      aPrintWriter.print(", " + aPrintData.getSurvey().getPlatform().getNation().getNationName());
      aPrintWriter.print(", " + aPrintData.getSurvey().getCompId().getSurvey());
      aPrintWriter.print(", " + aPrintData.getScatter(aMode).getCompId().getFrequency());
      aPrintWriter.print(", " + aPrintData.getScatter(aMode).getCompId().getTransceiver());
      // Print data
      aPrintWriter.print(", " + ReportUtils.DATE.format(time));
      aPrintWriter.print(", " + ReportUtils.TIME_LONG.format(time));
      //vessel distance
      aPrintWriter.print(", " + df2.format(aPrintData.getObservation(aMode).getDistance()));
      aPrintWriter.print(", " + df2.format(aPrintData.getObservation(aMode).getDistance() + scat.getDistanceInterval()));
      //position
      aPrintWriter.print(", " + df5.format(aPrintData.getObservation(aMode).getLongitude()));
      aPrintWriter.print(", " + df5.format(aPrintData.getObservation(aMode).getLatitude()));
      //average depth
      float averageDepth = (scat.getMinBottomDepth() + scat.getMaxBottomDepth()) / 2;
      //printStream.print(", " + df1.format(averageDepth));
      aPrintWriter.print(", " + df1.format(averageDepth));

      // Print species scatter and total_sum(i=aPrintData.getPrintCount)
      for (int i = 0; i <= aPrintData.getPrintCount(); i++) { // "i" is species number in array
         //printStream.print(String.format("%s, ", df1.format(dataPrint[i][0])));
         aPrintWriter.print(", " + df3.format(dataPrint[i][0]));
      }

      aPrintWriter.println();
   } //print()  METHOD
}
