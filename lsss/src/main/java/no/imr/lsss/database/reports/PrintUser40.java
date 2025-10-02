package no.imr.lsss.database.reports;

import no.imr.lsss.database.reports.hibernate.DistCount;
import no.imr.tools.Utils;
import no.imr.tools.io.Print;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Print vertical distribution of average or sum values very compact.
 */
final class PrintUser40 extends BaseTotalReport {
   PrintUser40(ReportEngine reportEngine) {
      super(40, reportEngine);
   }

   @Override
   void printHeader(PrintWriter aPrintWriter, PrintTotalData aPrintTotalData) {
      //ReportUtils.writeStandardHeader(aPrintWriter, "2.7.0");
      ReportUtils.writeStandardHeader(aPrintWriter, "2.11.0");

      aPrintWriter.println("% Vertical distribution of average sA");

      aPrintWriter.println("% SHIP: " + aPrintTotalData.getSurvey().getPlatform().findPlatformName(aPrintTotalData.getSurvey())
            + ", NATION: " + aPrintTotalData.getSurvey().getPlatform().getNation().getNationName()
            + ", SURVEY: " + aPrintTotalData.getSurvey().getCompId().getSurvey()
            + ", FREQUENCY: " + String.format("%d", aPrintTotalData.getFrequency()) + " [Hz]"
            + ", TRANSCEIVER: " + String.format("%1d", aPrintTotalData.getTransceiver()));

      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df1 = new DecimalFormat("#0.0", dfs);
      int count = aPrintTotalData.getDistCount_Count(aPrintTotalData.getFrequency(), aPrintTotalData.getTransceiver());
      for (int i = 1; i <= count; i++) {
         DistCount distCount = aPrintTotalData.getDistCount(aPrintTotalData.getFrequency(), aPrintTotalData.getTransceiver(), i);
         aPrintWriter.print("% DistanceInterval: " + df1.format(distCount.distanceInterval()) + " [nmi]"
               + ", Count: " + String.format("%1d", distCount.distCount()));
      }
      aPrintWriter.println();
      aPrintWriter.println("%");
   }

   @Override
   void print(PrintWriter aPrintWriter, PrintTotalData aPrintTotalData) {
      aPrintWriter.print("AcCat,sA_Tot");
      for (int jCh = 1; jCh <= aPrintTotalData.getMaxCh(); jCh++) {
         aPrintWriter.print(",sA_Ch" + jCh);
      }
      aPrintWriter.println();

      for (int iCat = 0; iCat < aPrintTotalData.getAcCatCount(); iCat++) {
         aPrintWriter.print(aPrintTotalData.getAcCat(iCat).initials());
         Print.commaAndValue(aPrintWriter, Utils.format("%f", aPrintTotalData.getData(iCat, 0)));
         for (int jCh = 1; jCh <= aPrintTotalData.getMaxCh(); jCh++) {
            Print.commaAndValue(aPrintWriter, Utils.format("%f", aPrintTotalData.getData(iCat, jCh)));
         }
         aPrintWriter.println();
      }
      aPrintWriter.println();
   }
}
