package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.tools.Utils;
import no.imr.tools.io.Print;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Same data as PrintUser21, but not normalized against 38 kHz.
 */
final class PrintUser24 extends BaseMultiFrequencyTextReport {
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
   private double[][] sa = new double[MAX_CH][MAX_FREQ];

   PrintUser24(ReportEngine reportEngine) {
      super(24, reportEngine);
   }

   @Override
   void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData) {
      ReportUtils.writeStandardHeader(aPrintWriter, "1.8.1");
      aPrintWriter.printf("%% Nautical Area Scattering Coefficient (Area backscattering coefficient - sA) for species first on list%n");
      aPrintWriter.printf("%% Nation: %s", aPrintData.getNation().getNationName());
      aPrintWriter.printf("(%d);  ", aPrintData.getNation().getNation());
      aPrintWriter.printf("Platform: %s", aPrintData.getPlatform().findPlatformName(aPrintData.getSurvey()));
      aPrintWriter.printf("(%d);  %n", aPrintData.getPlatform().getCompId().getPlatform());
      aPrintWriter.printf("%% Survey: %s (%d);  %n", aPrintData.getSurvey().getSurveyTitle(),
            aPrintData.getSurvey().getCompId().getSurvey());
      if (aPrintData.getPrintDataScatterType() == ScatterTypeEnum.PELAGIC_SCHOOL.getValue()) {
         aPrintWriter.printf("%% Scatter type: SCHOOL(%d);  %n", aPrintData.getPrintDataScatterType());
      } else if (aPrintData.getPrintDataScatterType() == ScatterTypeEnum.PELAGIC.getValue()) {
         aPrintWriter.printf("%% Scatter type: SCATTER(%d);  %n", aPrintData.getPrintDataScatterType());
      }
      aPrintWriter.printf("%%%n");

      int maxFrequencyIndex = aPrintData.getFrequencyList().size() - 1;
      aPrintWriter.printf("%% Species[-],Object[-],Date[yyyymmdd],Time[hhmmssxx],Threshold[dB],Ch[#],dCh[#]");
      for (int i = 0; i <= maxFrequencyIndex; i++) {
         int fr = aPrintData.getFrequency(i) / 1000;
         aPrintWriter.printf(",sa%03d[m2/nmi2]", fr);
      }
      aPrintWriter.println();
      aPrintWriter.printf("Species,Object,Date,Time,Threshold,Ch,dCh");
      for (int i = 0; i <= maxFrequencyIndex; i++) {
         int fr = aPrintData.getFrequency(i) / 1000;
         aPrintWriter.printf(",sa%03d", fr);
      }
      aPrintWriter.println();
   } //printHeader()

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

   private void print_rf(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, ReportMode aMode, double[][] sa, int aMaxCh) {
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df = new DecimalFormat("#0.000", dfs);
      DecimalFormat df2 = new DecimalFormat("#0.0", dfs);

      for (int ch = 1; ch < aMaxCh; ch++) {
         boolean dataFound = false;
         for (int jFrequency = 0; jFrequency < aPrintData.getFrequencyList().size(); jFrequency++) {
            if (sa[ch][jFrequency] > 0.01) { // For at least one of the frequencies
               dataFound = true;
               break;
            }
         }

         if (!dataFound) {
            continue;
         }

         aPrintWriter.print(getLanguageUtils().getAcCatInitials(aPrintData.getAcousticCategory(0)));
         Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getObject());
         Print.commaAndValue(aPrintWriter, lastDate);
         Print.commaAndValue(aPrintWriter, Utils.format("%08d", lastTime));
         Print.commaAndValue(aPrintWriter, df.format(aPrintData.getScatter(aMode).getThreshold()));
         Print.commaAndValue(aPrintWriter, ch);
         float dCh = aPrintData.getScatter(aMode).getChannelThickness();
         if (ch == 0) { //todo Always false??
            float upper = Math.max(aPrintData.getScatter(aMode).getUpperInterpretationDepth(),
                  aPrintData.getScatter(aMode).getUpperDepth());
            float lower = Math.min(aPrintData.getScatter(aMode).getLowerInterpretationDepth(),
                  aPrintData.getScatter(aMode).getLowerDepth());
            dCh = lower - upper;
         }
         Print.commaAndValue(aPrintWriter, df2.format(dCh));

         int maxFrequencyIndex = aPrintData.getFrequencyList().size() - 1;
         for (int jFrequency = 0; jFrequency <= maxFrequencyIndex; jFrequency++) {
            Print.commaAndValue(aPrintWriter, df.format(sa[ch][jFrequency]));
         }
         aPrintWriter.println();
      }
   } //print_rf()

   @Override
   void print(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, ReportMode aMode, PrintData.Bottom aPrintDataBottom) {
      float[][] dataPrint = aPrintData.getSa(aMode);   //First index: species; last index: channel

      if (lastDate < 0 || lastTime < 0) {
         //Initialize first time
         lastDate = aPrintData.getScatter(aMode).getCompId().getObservationDate();
         lastTime = aPrintData.getScatter(aMode).getCompId().getObservationTime();
         resetSa_rf(aPrintData, sa);
      }

      if (aPrintData.getScatter(aMode).getCompId().getObservationDate() != lastDate ||
            aPrintData.getScatter(aMode).getCompId().getObservationTime() != lastTime) {
         // Calculate and print
         print_rf(aPrintWriter, aPrintData, aMode, sa, maxCh);

         //Initialize and reset data array
         lastDate = aPrintData.getScatter(aMode).getCompId().getObservationDate();
         lastTime = aPrintData.getScatter(aMode).getCompId().getObservationTime();
         maxCh = -1;
         resetSa_rf(aPrintData, sa);
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
            sa[iCh][jFrequency] = dataPrint[iSpecies][iCh];
            if (iCh > maxCh && sa[iCh][jFrequency] > 0.0) {
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
}  //PrintUser24
