package no.imr.lsss.database.reports;

import no.imr.lsss.database.util.DatabaseTime;
import no.imr.tools.Utils;
import no.imr.tools.io.Print;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

final class PrintUser16 extends BaseMultipleSpeciesPerFileReport {
   private static final DateTimeFormatter DATE_TIME_FORMATTER = Utils.createUTCDateTimeFormatter("yyyy MM dd  HH:mm:ss");

   PrintUser16(ReportEngine reportEngine) {
      super(16, reportEngine);
   }

   @Override
   void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int printType, ReportMode aMode) {
      aPrintWriter.print("YEAR");
      aPrintWriter.print(" MO");
      aPrintWriter.print(" DA");
      aPrintWriter.print("       UTC");    //Melle, 2013.11.29
      aPrintWriter.print("      LOG1");    //Melle, 2013.11.29
      aPrintWriter.print("      LOG2");    //Melle, 2013.11.29
      aPrintWriter.print("   LATITUDE ");
      aPrintWriter.print("  LONGITUD");
      aPrintWriter.print("  BDMIN");
      aPrintWriter.print("   BDMAX");
      aPrintWriter.print(" OBJECT");
      aPrintWriter.print("    CH");
      aPrintWriter.print(" PDMIN");
      aPrintWriter.print(" PDMAX");
      aPrintWriter.print("  PDMEAN");
      aPrintWriter.print("  UPINLM");
      aPrintWriter.print(' ');

      // Species already ordered. Initials for "TOTAL" printed below
      for (int i = 0; i < aPrintData.getPrintCount(); i++) {
         Print.spaceAndLeftPaddedValue(aPrintWriter, 12, getLanguageUtils().getAcCatInitials(aPrintData.getAcousticCategory(i)));
      }
      aPrintWriter.println("        TOTAL");
   }

   /**
    * HISTORY:    Same as PrintAll() (of BEI), but with 4 decimals printed for each species. Each single
    * variable is stored in separate columns.
    * UPDATES:    Made 20.11.93 and modified 12.6.97. Implement changes to print
    * all information in columns rather than using header
    * information for each dataset. This printout does not
    * contain bottom integrator values. Bottom integrator data are stored
    * in a separate file. New changes 4.3.99 to comply with
    * the new version of BEI installed. Implementation of Scatter.PelagicUpper
    * still remains to be done !!!
    *
    * @param aPrintWriter      print file
    * @param aPrintData        pelagic data
    * @param aPrintFrequency   print frequency
    * @param aPrintTransceiver print transceiver
    * @param aMode             print mode
    */
   @Override
   void print(
         PrintWriter aPrintWriter,
         PrintData.Pelagic aPrintData,
         int aPrintFrequency, short aPrintTransceiver, ReportMode aMode) {

      Instant time = Instant.ofEpochMilli(DatabaseTime.toMillis(aPrintData.getScatter(aMode)));

      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df0 = new DecimalFormat("#0", dfs);
      DecimalFormat df1 = new DecimalFormat("#0.0", dfs);
      DecimalFormat df3 = new DecimalFormat("#0.000", dfs);   //Melle, 2013.11.29
      DecimalFormat df4 = new DecimalFormat("#0.0000", dfs);
      DecimalFormat df5 = new DecimalFormat("#0.00000", dfs);

      String firstColumns = DATE_TIME_FORMATTER.format(time)
            + String.format(" %9s", df3.format(aPrintData.getObservation(aMode).getDistance()))
            + String.format(" %9s", df3.format(aPrintData.getObservation(aMode).getDistance() + aPrintData.getScatter(aMode).getDistanceInterval()))
            + String.format(" %10s", df5.format(aPrintData.getObservation(aMode).getLatitude()))
            + String.format(" %10s", df5.format(aPrintData.getObservation(aMode).getLongitude()))
            + String.format(" %6s", df1.format(aPrintData.getScatter(aMode).getMinBottomDepth()))
            + String.format(" %7s", df1.format(aPrintData.getScatter(aMode).getMaxBottomDepth()))
            + String.format(" %6d", aPrintData.getScatter(aMode).getCompId().getObject());

      /* Print pelagic integrator values: */
      for (int i = 1; i <= aPrintData.getMaxChannel(aMode); i++) {
         float pelUpper = aPrintData.getScatter(aMode).getUpperDepth();
         float scatterPCT = aPrintData.getScatter(aMode).getChannelThickness();
         float upper = scatterPCT * (i - 1);
         if (aPrintData.getScatter(aMode).getUpperDepth() < pelUpper) {
            pelUpper = aPrintData.getScatter(aMode).getUpperInterpretationDepth();
         }

         if (upper + scatterPCT <= pelUpper) {
            // do not print nonexistent depth-channel
            continue;
         }

         aPrintWriter.print(firstColumns);

         float delta = aPrintData.getScatter(aMode).getChannelThickness() / 2;

         if (upper >= pelUpper) {
            Print.spaceAndLeftPaddedValue(aPrintWriter, 5, Integer.toString(i));                    // Channel full (normal situation)
            Print.spaceAndLeftPaddedValue(aPrintWriter, 5, df0.format(upper));                      // Upper edge
            Print.spaceAndLeftPaddedValue(aPrintWriter, 5, df0.format(upper + scatterPCT)); // Lower edge
            Print.spaceAndLeftPaddedValue(aPrintWriter, 7, df1.format(upper + delta));      // Mean
            Print.spaceAndLeftPaddedValue(aPrintWriter, 7, df1.format(pelUpper));                   // Upper integr. limit
         } else {  // upper < PelUpper: there is no data there
            Print.spaceAndLeftPaddedValue(aPrintWriter, 5, Integer.toString(i));
            if (upper + scatterPCT > pelUpper) {    // Channel partially filled
               Print.spaceAndLeftPaddedValue(aPrintWriter, 5, df0.format(pelUpper));                                      // Upper edge
               Print.spaceAndLeftPaddedValue(aPrintWriter, 5, df0.format(upper + scatterPCT));                    // Lower edge
               Print.spaceAndLeftPaddedValue(aPrintWriter, 7, df1.format((pelUpper + (upper + scatterPCT)) / 2)); // Mean
               Print.spaceAndLeftPaddedValue(aPrintWriter, 7, df1.format(pelUpper));                                      // Upper integr. limit
            } else {                                   // Channel empty
               // upper + ScatterPCT <= PelUpper
               if (upper + 2 * scatterPCT > pelUpper) {
                  // This channel empty, and next is partially filled: make depth channel cover
                  Print.spaceAndLeftPaddedValue(aPrintWriter, 5, df0.format(upper));                               // Upper edge
                  Print.spaceAndLeftPaddedValue(aPrintWriter, 5, df0.format(pelUpper));                            // Lower edge
                  Print.spaceAndLeftPaddedValue(aPrintWriter, 7, df1.format((upper + pelUpper) / 2));      // Mean
                  Print.spaceAndLeftPaddedValue(aPrintWriter, 7, df1.format(pelUpper));                            // Upper integr. limit
               } else {
                  // Next channel is also empty: make this channel cover only normal depth-channel
                  Print.spaceAndLeftPaddedValue(aPrintWriter, 5, df0.format(upper));                               // Upper edge
                  Print.spaceAndLeftPaddedValue(aPrintWriter, 5, df0.format(upper + scatterPCT));          // Lower edge
                  Print.spaceAndLeftPaddedValue(aPrintWriter, 7, df1.format(upper + delta));               // Mean
                  Print.spaceAndLeftPaddedValue(aPrintWriter, 7, df1.format(pelUpper));                            // Upper integr. limit
               }
            }
         }
         aPrintWriter.print(' ');

         for (int j = 0; j <= aPrintData.getPrintCount(); j++) {  //j is the species count (last is sum), i is the channel.
            Print.spaceAndLeftPaddedValue(aPrintWriter, 12, df4.format(aPrintData.getSaIJ(aMode, j, i)));
         } /*for*/
         aPrintWriter.println();
      } /*for*/
   } //print()
}
