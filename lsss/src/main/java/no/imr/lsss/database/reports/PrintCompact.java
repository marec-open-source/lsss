package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.tools.Utils;
import no.imr.tools.io.Print;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;

final class PrintCompact extends BaseMultipleSpeciesPerFileReport {
   private int compactRow = 0;                  // print heading in compact file
   private int lastFrequency = -1;
   private int lastTransceiver = -1;

   PrintCompact(ReportEngine reportEngine) {
      super(0, reportEngine);
   }

   void resetRow() {
      compactRow = 0;
   }

   @Override
   String getFilePrefix() {
      return "ListComScatter";
   }

   @Override
   void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int printType, ReportMode aMode) {
   }

   @Override
   void print(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int aPrintFrequency, short aPrintTransceiver, ReportMode aMode) {
      if (lastFrequency != aPrintFrequency || lastTransceiver != aPrintTransceiver) {
         compactRow = 0;  // Force new heading at start of file
         lastFrequency = aPrintFrequency;
         lastTransceiver = aPrintTransceiver;
      }

      int numCompactBlocks = 4;
      if (compactRow % (10 * numCompactBlocks) == 0) {
         aPrintWriter.printf("\f");  //  Form feed - new page
         aPrintWriter.println("SHIP: " + aPrintData.getSurvey().getPlatform().findPlatformName(aPrintData.getSurvey())
               + "   NATION: " + aPrintData.getSurvey().getPlatform().getNation().getNationName()
               + "   SURVEY: " + aPrintData.getSurvey().getCompId().getSurvey());
         aPrintWriter.println("FREQUENCY: " + String.format("%8d", aPrintData.getScatter(aMode).getCompId().getFrequency()) + " [Hz]"
               + "   TRANSCEIVER: " + String.format("%1d", aPrintData.getScatter(aMode).getCompId().getTransceiver()));
         aPrintWriter.println();
         aPrintWriter.print("   DATE     TIME        LOG             POSITION      DEPTH");

         // Species already ordered. Initials for "TOTAL" printed below
         for (int i = 0; i < aPrintData.getPrintCount(); i++) {
            Print.spaceAndLeftPaddedValue(aPrintWriter, 6, getLanguageUtils().getAcCatInitials(aPrintData.getAcousticCategory(i)));
         }

         aPrintWriter.println("  TOTAL SC");
      } else if (compactRow % 10 == 0) {
         aPrintWriter.println();  // New line
      }

      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df0 = new DecimalFormat("#0", dfs);
      DecimalFormat df1 = new DecimalFormat("#0.0", dfs);

      Scatter scatter = aPrintData.getScatter(aMode);
      float[][] dataPrint = aPrintData.getSa(aMode);

      // Date
      Instant time = Instant.ofEpochMilli(DatabaseTime.toMillis(scatter.getCompId()));
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
         Print.spaceAndLeftPaddedValue(aPrintWriter, 6, df0.format(dataPrint[i][0]));
      }

      // School count
      Print.spaceAndLeftPaddedValue(aPrintWriter, 2, aPrintData.getSchoolCount(aMode));

      aPrintWriter.println();

      // Comment if available
      for (int i = 0; i < aPrintData.getCommentCount(aMode); i++) {
         Instant commentTime = Instant.ofEpochMilli(DatabaseTime.toMillis(aPrintData.getComment(aMode, i)));
         Print.leftPaddedValue(aPrintWriter, 8, ReportUtils.DATE.format(commentTime));
         Print.spaceAndLeftPaddedValue(aPrintWriter, 5, ReportUtils.TIME.format(commentTime));
         aPrintWriter.print("  ");
         aPrintWriter.print(aPrintData.getComment(aMode, i).getText());
         aPrintWriter.print(" ");
         if (aPrintData.getComment(aMode, i).getMantissa() != 0) {
            aPrintWriter.print(BigDecimal.valueOf(aPrintData.getComment(aMode, i).getMantissa(), aPrintData.getComment(aMode, i).getExp()));
         }
         aPrintWriter.println();
      }

      compactRow++;
   }
}
