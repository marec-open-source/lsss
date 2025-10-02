package no.imr.lsss.database.reports;

import no.imr.tools.Utils;
import no.imr.tools.io.Print;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Same format as {@link PrintUser22}, but only for depth channels &gt; 0 and sA_ch0 &gt; 1 and sA_ch &gt; 0.1.
 */
final class PrintUser23 extends BaseMultiFrequencyTextReport {
   private static final int MAX_CH = 2000;
   private static final int MAX_FREQ = 25;
   private int lastDate = -1;
   private int lastTime = -1;
   private int lastFrequency = -1;
   private int lastFrequencyIndex = -1;
   private int lastTransceiver = -1;
   private int lastScatterType = -1;
   private int lastAcousticCategory = -1;
   private int maxCh = -1;
   private double[][] sa_rf = new double[MAX_CH][MAX_FREQ];

   PrintUser23(ReportEngine reportEngine) {
      super(23, reportEngine);
   }

   @Override
   void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData) {
      ReportUtils.writeStandardHeader(aPrintWriter, "1.8.0");
      aPrintWriter.printf("%% Relative frequency response for species first on list%n");
      aPrintWriter.printf("%% Nation: %s", aPrintData.getNation().getNationName());
      aPrintWriter.printf("(%d);  ", aPrintData.getNation().getNation());
      aPrintWriter.printf("Platform: %s", aPrintData.getPlatform().findPlatformName(aPrintData.getSurvey()));
      aPrintWriter.printf("(%d);  %n", aPrintData.getPlatform().getCompId().getPlatform());
      aPrintWriter.printf("%% Survey: %s (%d);  %n", aPrintData.getSurvey().getSurveyTitle(),
            aPrintData.getSurvey().getCompId().getSurvey());
      aPrintWriter.printf("%%%n");

      int maxFrequencyIndex = aPrintData.getFrequencyList().size() - 1;
      aPrintWriter.printf("%% Species[-],Date[yyyymmdd],Time[hhmmssxx],Ch[#],dCh[#],sA38[m2/nmi2]");
      for (int i = 0; i <= maxFrequencyIndex; i++) {
         int fr = aPrintData.getFrequency(i) / 1000;
         aPrintWriter.printf(",r%03d[-]", fr);
      }
      aPrintWriter.println();
      aPrintWriter.printf("Species,Date,Time,Ch,dCh,sA38");
      for (int i = 0; i <= maxFrequencyIndex; i++) {
         int fr = aPrintData.getFrequency(i) / 1000;
         aPrintWriter.printf(",r%03d", fr);
      }
      aPrintWriter.println();
   } //printHeader()

   //todo Should find species first on list even if the "select all" box is selected
   private static int getSpeciesIndex(int aAcousticCategory) {
      int iSpeciesIndex = 0;

      return 0;
   }

   // Set elements of data array to -1.0: not very elegant, but it works
   private static void resetSa_rf(PrintData.Pelagic aPrintData, double[][] aSa_rf) {
      int mrf = MAX_FREQ - 1;
      if (aPrintData.getFrequencyList().size() > 1) {
         mrf = aPrintData.getFrequencyList().size() - 1;
      }
      for (int i = 0; i < MAX_CH; i++) {
         for (int j = 0; j <= mrf; j++) {
            aSa_rf[i][j] = -1.0;
         }
      }
   }

   // Set elements of data array to zero: not very elegant, but it works
   private static void calculate_rf(PrintData.Pelagic aPrintData, double[][] aSa_rf, int aMaxCh) {
      int j38 = aPrintData.getFrequencyIndex(38000);
      int maxFrequencyIndex = aPrintData.getFrequencyList().size() - 1;

      for (int i = 0; i < aMaxCh; i++) {
         if (j38 == -1 || aSa_rf[i][j38] == 0) {
            for (int j = 0; j <= maxFrequencyIndex; j++) {
               aSa_rf[i][j] = -1.0;   //Marks illegal value
            }
         } else {
            for (int j = 0; j <= maxFrequencyIndex; j++) {
               if (j != j38 && aSa_rf[i][j] > 0.0 && aSa_rf[i][j38] > 0.0) {
                  aSa_rf[i][j] /= aSa_rf[i][j38];  //relative to 38 kHz
               }
            }
         }
      }
   } //calculate_rf()

   private void print_rf(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, ReportMode aMode, double[][] sa_rf, int aMaxCh) {
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df = new DecimalFormat("#0.000", dfs);
      DecimalFormat df2 = new DecimalFormat("#0.0", dfs);
      DecimalFormat df38 = new DecimalFormat("#0.000000", dfs);

      int j38 = aPrintData.getFrequencyIndex(38000);

      for (int ch = 1; ch < aMaxCh; ch++) {
         boolean dataFound = false;
         for (int jFrequency = 0; jFrequency < aPrintData.getFrequencyList().size(); jFrequency++) {
            if (sa_rf[ch][jFrequency] > 1) { // For at least one of the frequencies
               dataFound = true;
               break;
            }
         }

         if (!dataFound) {
            continue;
         }

         if (sa_rf[ch][j38] > 0.1) { // Minimum value to print.
            aPrintWriter.print(getLanguageUtils().getAcCatInitials(aPrintData.getAcousticCategory(0)));
            Print.commaAndValue(aPrintWriter, lastDate);
            Print.commaAndValue(aPrintWriter, Utils.format("%08d", lastTime));
            Print.commaAndValue(aPrintWriter, ch);
            float dCh = aPrintData.getScatter(aMode).getChannelThickness();
            if (ch == 0) {  //todo Always false??
               float upper = Math.max(aPrintData.getScatter(aMode).getUpperInterpretationDepth(),
                     aPrintData.getScatter(aMode).getUpperDepth());
               float lower = Math.min(aPrintData.getScatter(aMode).getLowerInterpretationDepth(),
                     aPrintData.getScatter(aMode).getLowerDepth());
               dCh = lower - upper;
            }
            Print.commaAndValue(aPrintWriter, df2.format(dCh));
            Print.commaAndValue(aPrintWriter, df38.format(sa_rf[ch][j38]));     //Integral at 38 kHz

            int maxFrequencyIndex = aPrintData.getFrequencyList().size() - 1;
            for (int jFrequency = 0; jFrequency <= maxFrequencyIndex; jFrequency++) {
               if (jFrequency == j38) {
                  Print.commaAndValue(aPrintWriter, df.format(1.0));     //Integral
               } else {
                  Print.commaAndValue(aPrintWriter, df.format(sa_rf[ch][jFrequency]));     //r(f)
               }
            }
            aPrintWriter.println();
         }
      }
   } //print_rf()

   @Override
   void print(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, ReportMode aMode, PrintData.Bottom aPrintDataBottom) {
      float[][] dataPrint = aPrintData.getSa(aMode);   //First index: species; last index: channel

      if (lastDate < 0 || lastTime < 0) {
         //Initialize first time
         lastDate = aPrintData.getScatter(aMode).getCompId().getObservationDate();
         lastTime = aPrintData.getScatter(aMode).getCompId().getObservationTime();
         resetSa_rf(aPrintData, sa_rf);
      }

      if (aPrintData.getScatter(aMode).getCompId().getObservationDate() != lastDate ||
            aPrintData.getScatter(aMode).getCompId().getObservationTime() != lastTime) {
         // Calculate and print
         calculate_rf(aPrintData, sa_rf, maxCh);
         print_rf(aPrintWriter, aPrintData, aMode, sa_rf, maxCh);

         //Initialize and reset data array
         lastDate = aPrintData.getScatter(aMode).getCompId().getObservationDate();
         lastTime = aPrintData.getScatter(aMode).getCompId().getObservationTime();
         maxCh = -1;
         resetSa_rf(aPrintData, sa_rf);
      }

      // Same time/distance as last entry: FILL DATA
      if (aPrintData.getScatter(aMode).getCompId().getObservationDate() == lastDate &&
            aPrintData.getScatter(aMode).getCompId().getObservationTime() == lastTime) {
         if (aPrintData.getPrintCount() == 0) {
            return;
         }
         int jFrequency = aPrintData.getFrequencyIndex(aPrintData.getScatter(aMode).getCompId().getFrequency());
         int aAcousticCategory = aPrintData.getAcousticCategory(0).getCompId().getAcousticCategory();

         int iSpecies = getSpeciesIndex(aAcousticCategory);
         for (int iCh = 0; iCh < MAX_CH; iCh++) {
            sa_rf[iCh][jFrequency] = dataPrint[iSpecies][iCh];
            if (iCh > maxCh && sa_rf[iCh][jFrequency] > 0.0) {
               maxCh = iCh;
            }
         }
      }

      int i;
      for (i = 0; i < aPrintData.getPrintCount(); i++) {
         short nation = aPrintData.getSurvey().getCompId().getNation();
         short platform = aPrintData.getSurvey().getCompId().getPlatform();
         int acousticCategory = aPrintData.getAcousticCategory(i).getCompId().getAcousticCategory();
         if (aPrintData.getPrintOrder(nation, platform, acousticCategory) == 0) {
            break;  //Print species = i
         }
      }
   }  // print()
}  //PrintUser23
