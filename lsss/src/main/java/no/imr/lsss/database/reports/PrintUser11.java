package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.tools.Utils;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

final class PrintUser11 extends BaseOneSpeciesPerFileReport {
   PrintUser11(ReportEngine reportEngine) {
      super(11, reportEngine);
   }

   @Override
   void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int printType, ReportMode aMode) {
      int maxPrintPelagic = getReportEngine().getMaxPrintPelagicCh();  //Channel 0 is not counted

      ReportUtils.writeStandardHeader(aPrintWriter, "1.9.1");
      aPrintWriter.printf("%% Compact printout sorted by time, one species per file.%n");
      aPrintWriter.printf("%% Nation: %s", aPrintData.getNation().getNationName());
      aPrintWriter.printf("(%d);  ", aPrintData.getNation().getNation());
      aPrintWriter.printf("Platform: %s", aPrintData.getPlatform().findPlatformName(aPrintData.getSurvey()));
      aPrintWriter.printf("(%d);  %n", aPrintData.getPlatform().getCompId().getPlatform());
      aPrintWriter.printf("%% Survey: %s (%d);  %n", aPrintData.getSurvey().getSurveyTitle(),
            aPrintData.getSurvey().getCompId().getSurvey());
      aPrintWriter.printf("%% Description: %s;  %n", aPrintData.getSurvey().getSurveyDescription().replace("\n", "\n% "));
      if (aPrintData.getPrintDataScatterType() == ScatterTypeEnum.PELAGIC_SCHOOL.getValue()) {
         aPrintWriter.printf("%% Scatter type: SCHOOL(%d);  %n", aPrintData.getPrintDataScatterType());
      } else if (aPrintData.getPrintDataScatterType() == ScatterTypeEnum.PELAGIC.getValue()) {
         aPrintWriter.printf("%% Scatter type: SCATTER(%d);  %n", aPrintData.getPrintDataScatterType());
      }
      aPrintWriter.printf("%% Frequency: %d[Hz];  Transceiver: %d[-];  %n",
            aPrintData.getScatter(aMode).getCompId().getFrequency(),
            aPrintData.getScatter(aMode).getCompId().getTransceiver());
      aPrintWriter.printf("%%%n");

      aPrintWriter.printf("%% DATE[yyyymmdd],TIME[hhmmssxx],START_LOG[nmi],STOP_LOG[nmi],Latitude[deg],");
      aPrintWriter.printf("Longitude[deg],MinDepth[m],MaxDepth[m],Frequency[Hz],Transceiver[-],Threshold[dB],");
      aPrintWriter.printf("Species[-],ScatterPCT[m],NoPelagic[-],P_CH#[m2/nmi2],...,P_CH#[m2/nmi2],BottomUpper[m],");
      aPrintWriter.printf("B_CH#[m2/nmi2],...,B_CH#[m2/nmi2]%n");

      aPrintWriter.printf("%10s ", "DATE");
      aPrintWriter.printf("%10s ", "TIME");
      aPrintWriter.printf("%10s ", "START_LOG");
      aPrintWriter.printf("%10s ", "STOP_LOG");
      aPrintWriter.printf("%10s ", "Latitude");  /*Pos. in decimal deg.*/
      aPrintWriter.printf("%10s ", "Longitude"); /*Pos. in decimal deg.*/
      aPrintWriter.printf("%10s ", "MinDepth");       /*Min bottom depth*/
      aPrintWriter.printf("%10s ", "MaxDepth");       /*Max bottom depth*/
      aPrintWriter.printf("%10s ", "Frequency");
      aPrintWriter.printf("%10s ", "Transcei.");
      aPrintWriter.printf("%10s ", "Threshold");
      aPrintWriter.printf("%10s ", "Species");
      aPrintWriter.printf("%10s ", "ScatterPCT"); /*Pelagic Channel Thickness*/
      aPrintWriter.printf("%10s ", "NoPelagic");          /*Number of pelagic ch.*/
      for (int i = 0; i <= maxPrintPelagic; i++) {
         aPrintWriter.printf("%13s%03d ", "P_CH", i);
      }
      aPrintWriter.printf("%16s ", "BottomUpper");
      aPrintWriter.printf("%16s", "B_CH0");
      aPrintWriter.println();
   }

   /**
    * HISTORY:     The program is requested by Aril Slotte (IMR) to be used by the EXCEL.
    *
    * @param aPrintWriter           print file
    * @param aPrintData             pelagic data
    * @param aPrintDataBottom       bottom data
    * @param aAcousticCategoryIndex index in species-array
    */
   @Override
   void print(
         PrintWriter aPrintWriter,
         PrintData.Pelagic aPrintData,
         PrintData.Bottom aPrintDataBottom,
         ReportMode aMode,
         int aAcousticCategoryIndex                 // Index in AcousticCategory array in PrintData
   ) {
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df = new DecimalFormat("#0.000", dfs);
      DecimalFormat df2 = new DecimalFormat("#0.00000", dfs);

      int maxPrintPelagic = getReportEngine().getMaxPrintPelagicCh();

      float[][] dataPrint = aPrintData.getSa(aMode);
      float[][] bottomDataPrint = aPrintDataBottom.getSa(aMode);

      aPrintWriter.printf("%10d ", aPrintData.getScatter(aMode).getCompId().getObservationDate());
      aPrintWriter.printf("%10d ", aPrintData.getScatter(aMode).getCompId().getObservationTime());

      aPrintWriter.printf("%10s ", df.format(aPrintData.getObservation(aMode).getDistance()));
      aPrintWriter.printf("%10s ", df.format(aPrintData.getObservation(aMode).getDistance() + aPrintData.getScatter(aMode).getDistanceInterval()));
      aPrintWriter.printf("%10s ", df.format(aPrintData.getObservation(aMode).getLatitude()));
      aPrintWriter.printf("%10s ", df.format(aPrintData.getObservation(aMode).getLongitude()));
      aPrintWriter.printf("%10s ", df.format(aPrintData.getScatter(aMode).getMinBottomDepth()));
      aPrintWriter.printf("%10s ", df.format(aPrintData.getScatter(aMode).getMaxBottomDepth()));
      aPrintWriter.printf("%10d ", aPrintData.getScatter(aMode).getCompId().getFrequency());
      aPrintWriter.printf("%10d ", aPrintData.getScatter(aMode).getCompId().getTransceiver());
      aPrintWriter.printf("%10s ", df.format(aPrintData.getScatter(aMode).getThreshold()));
      aPrintWriter.printf("%10d ", aPrintData.getAcousticCategory(aAcousticCategoryIndex).getCompId().getAcousticCategory());
      aPrintWriter.printf("%10s ", df.format(aPrintData.getScatter(aMode).getChannelThickness()));

      // Print pelagic values
      int til = aPrintData.getMaxChannel(aMode);
      aPrintWriter.printf("%10d ", aPrintData.getMaxChannel(aMode));         //Number of pelagic channels
      /* Bug: fixed 2011.05.25
      for ( int i=0; i<=aPrintData.getMaxChannel(aMode); i++ ) {
         printStream.printf("%16s ", df2.format(dataPrint[0][i]));     //Integral
      } /*for*/
      for (int ch = 0; ch <= aPrintData.getMaxChannel(aMode); ch++) { // ch=0 is sum channel, ch>0 is real channels
         aPrintWriter.printf("%16s", df2.format(dataPrint[aAcousticCategoryIndex][ch]));
      }

      float tom = -1;

      /* Print dummy values to get same number of data each time*/
      for (int i = til + 1; i <= maxPrintPelagic; i++) {
         aPrintWriter.printf("%16s ", df2.format(tom));
      } /*for*/

      // Print total bottom integrator values:
      float upperDepth;
      float bottomSa;
      Scatter bottomScatter = aPrintDataBottom.getScatter(aMode);
      if (bottomScatter != null) {
         upperDepth = bottomScatter.getUpperDepth();
         bottomSa = bottomDataPrint[0][0];
      } else {
         upperDepth = 0;
         bottomSa = 0;
      }
      aPrintWriter.printf("%16s ", df.format(upperDepth)); //Bottom-window limitation
      aPrintWriter.printf("%16s", df2.format(bottomSa));   //Integral

      aPrintWriter.println();
   } //print()
}
