package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.tools.Utils;
import no.imr.tools.io.Print;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;

final class PrintUser2 extends BaseMultipleSpeciesPerFileReport {
   PrintUser2(ReportEngine reportEngine) {
      super(2, reportEngine);
   }

   @Override
   void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int printType, ReportMode aMode) {
      ReportUtils.writeStandardHeader(aPrintWriter, "2.17.0");
      aPrintWriter.println("SHIP: " + aPrintData.getSurvey().getPlatform().findPlatformName(aPrintData.getSurvey())
            + "   NATION: " + aPrintData.getSurvey().getPlatform().getNation().getNationName()
            + "   SURVEY: " + aPrintData.getSurvey().getCompId().getSurvey());
      aPrintWriter.println("FREQUENCY: " + String.format("%8d", aPrintData.getScatter(aMode).getCompId().getFrequency()) + " [Hz]"
            + "   TRANSCEIVER: " + String.format("%1d", aPrintData.getScatter(aMode).getCompId().getTransceiver()));
      aPrintWriter.println();
      aPrintWriter.print("   DATE     TIME        LOG             POSITION      DEPTH");

      // Species already ordered. Initials for "TOTAL" printed below
      for (int i = 0; i < aPrintData.getPrintCount(); i++) {
         Print.spaceAndLeftPaddedValue(aPrintWriter, 10, getLanguageUtils().getAcCatInitials(aPrintData.getAcousticCategory(i)));
      }

      aPrintWriter.println("      TOTAL SC");
   }

   @Override
   void print(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int aPrintFrequency, short aPrintTransceiver, ReportMode aMode) {
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df0 = new DecimalFormat("#0", dfs);
      DecimalFormat df1 = new DecimalFormat("#0.0", dfs);
      DecimalFormat df3 = new DecimalFormat("#0.000", dfs);

      Scatter scatter = aPrintData.getScatter(aMode);
      float[][] dataPrint = aPrintData.getSa(aMode);

      // Date
      Instant time = DatabaseTime.toInstant(scatter.getCompId());
      Print.leftPaddedValue(aPrintWriter, 8, ReportUtils.DATE.format(time));
      Print.spaceAndLeftPaddedValue(aPrintWriter, 5, ReportUtils.TIME.format(time));

      // Vessel distance
      Print.spaceAndLeftPaddedValue(aPrintWriter, 7, df1.format(aPrintData.getObservation(aMode).getDistance()));
      aPrintWriter.print(" -");
      Print.spaceAndLeftPaddedValue(aPrintWriter, 6, df1.format(aPrintData.getObservation(aMode).getDistance() + scatter.getDistanceInterval()));
      aPrintWriter.print(" ");

      // Position
      Print.spaceAndLeftPaddedValue(aPrintWriter, 8, ReportUtils.latitudeString(aPrintData.getObservation(aMode).getLatitude()));
      Print.spaceAndLeftPaddedValue(aPrintWriter, 9, ReportUtils.longitudeString(aPrintData.getObservation(aMode).getLongitude()));

      // Average depth
      float averageDepth = (scatter.getMinBottomDepth() + scatter.getMaxBottomDepth()) / 2;
      Print.spaceAndLeftPaddedValue(aPrintWriter, 5, df0.format(averageDepth));

      // Species scatter and total_sum(i=aPrintData.getPrintCount)
      for (int i = 0; i <= aPrintData.getPrintCount(); i++) { // "i" is species number in array
         Print.spaceAndLeftPaddedValue(aPrintWriter, 10, df3.format(dataPrint[i][0]));
      }

      // School count
      Print.spaceAndLeftPaddedValue(aPrintWriter, 2, aPrintData.getSchoolCount(aMode));

      aPrintWriter.println();
   }
}
