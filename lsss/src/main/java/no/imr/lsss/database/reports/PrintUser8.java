package no.imr.lsss.database.reports;

import no.imr.lsss.database.util.DatabaseTime;
import no.imr.tools.Utils;
import no.imr.tools.io.Print;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Print PGNAPES format - file-type 2.
 */
final class PrintUser8 extends BaseOneSpeciesPerFileReport {
   PrintUser8(ReportEngine reportEngine) {
      super(8, reportEngine);
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
            + "\tSpecies"
            + "\tChUppDepth"
            + "\tChLowDepth"
            + "\tSA");
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
      String species = GetPgnapes.species(aPrintData.getAcousticCategory(aAcousticCategoryIndex));
      LocalDateTime time = LocalDateTime.ofInstant(DatabaseTime.toInstant(aPrintData.getScatter(aMode)), ZoneOffset.UTC);
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df1 = new DecimalFormat("#0.0", dfs);
      DecimalFormat df2 = new DecimalFormat("#0.00", dfs);

      float[][] sa = aPrintData.getSa(aMode);
      float channelThickness = aPrintData.getScatter(aMode).getChannelThickness();

      // This report was originally required to have 50 m vertical resolution (if possible), but that is no longer  required 2015.12.03
      //double y = 50.0 / aPrintData.getScatter(aMode).getChannelThickness();
      //long x = Math.round(y);
      //if (y <= 1 || x != y) x = 1;
      //double sa50 = 0;

      String firstColumns = country
            + '\t' + callsign
            + '\t' + aPrintData.getScatter(aMode).getCompId().getSurvey()
            + '\t' + df1.format(aPrintData.getObservation(aMode).getDistance())
            + '\t' + time.getYear()
            + '\t' + String.format("%02d", time.getMonthValue())
            + '\t' + String.format("%02d", time.getDayOfMonth())
            + '\t' + species;

      for (int ch = 1; ch <= aPrintData.getMaxChannel(aMode); ch++) {
      //for (int ch = aPrintData.getMinChannel(aMode); ch <= aPrintData.getMaxChannel(aMode); ch++) {
         //sa50 += sa[aAcousticCategoryIndex][ch];
         //if (ch % x == 1 || x == 1)
         //{
         aPrintWriter.print(firstColumns);
         Print.tabAndValue(aPrintWriter, df1.format((ch - 1) * channelThickness));
         //Print.tabAndValue(aPrintWriter, df1.format((ch + x - 1) * channelThickness));
         Print.tabAndValue(aPrintWriter, df1.format(ch * channelThickness));
         //}
         //if (ch % x == 0 || x == 1 || ch == aPrintData.getMaxChannel(aMode))
         //{
         //aPrintWriter.print(df2.format(sa50), "\t");
         Print.tabAndValue(aPrintWriter, df2.format(sa[aAcousticCategoryIndex][ch]));
         aPrintWriter.println('\t');
         //sa50 = 0;
         //}
      }
   } // print()
}
