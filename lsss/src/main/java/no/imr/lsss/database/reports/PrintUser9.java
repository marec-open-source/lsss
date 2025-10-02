package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.tools.Utils;
import no.imr.tools.io.Print;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

final class PrintUser9 extends BaseOneSpeciesPerFileReport {
   PrintUser9(ReportEngine reportEngine) {
      super(9, reportEngine);
   }

   @Override
   void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int printType, ReportMode aMode) {
      ReportUtils.writeStandardHeader(aPrintWriter, "1.8.0");
      aPrintWriter.printf("%% Printout with many bottom channels sorted by date, time. %n");
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

      aPrintWriter.printf("%% StartDate[yyyymmdd],StartTime[hhmmssxx],StartLog[nmi],StopLog[nmi],Latitude[deg],");
      aPrintWriter.printf("Longitude[deg],MinDepth[m],MaxDepth[m],Frequency[Hz],Transceiver[#],Threshold[dB],Species[-],");
      aPrintWriter.printf("PelagicChThickness[m],NoPelagicCh[#],NoPelagicChWithData[#],");
      aPrintWriter.printf("Total_sA_species1[m2/nmi2],PelCh1_sA_species1[m2/nmi2],PelCh2_sA_species1[m2/nmi2],...,");
      aPrintWriter.printf("BottomChThickness[m],NoBottomCh[#],NoBottomChWithData[#],");
      aPrintWriter.printf("Total_sA_species1[m2/nmi2],BottomCh1_sA_species1[m2/nmi2], ...%n");

      aPrintWriter.printf("Date,Time,StartLog,StopLog,Latitude,Longitude,MinDepth,MaxDepth,Freq,Tran,Thresh,Species,");

      int maxPrintPelagic = getReportEngine().getMaxPrintPelagicCh();  //Channel 0 is not counted
      aPrintWriter.printf("ChPel,NoChPel,NoChPelData,PelChTot");
      for (int i = 1; i <= maxPrintPelagic; i++) {
         aPrintWriter.printf(",PelCh%02d", i);
      }

      int maxPrintBottom = getReportEngine().getMaxPrintBottomCh();    //Channel 0 is not counted
      aPrintWriter.printf(",ChBott,NoChBott,NoChBottData,BottChTot");
      for (int i = 1; i <= maxPrintBottom; i++) {
         aPrintWriter.printf(",BottCh%02d", i);
      }

      aPrintWriter.println();
   }

   /**
    * Report format designed for general use by Rolf Korneliussen (IMR) due to request by Melanie Underwood.
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
         int aAcousticCategoryIndex                 // Acoustic category
   ) {
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df = new DecimalFormat("#0.000", dfs);
      DecimalFormat saFormat = new DecimalFormat("#0.00000", dfs);

      float[][] dataPrint = aPrintData.getSa(aMode);
      float[][] bottomDataPrint = aPrintDataBottom.getSa(aMode);

      int ch;
      int maxBottomCh = 0;
      int maxPrintPelagic = getReportEngine().getMaxPrintPelagicCh();  //Channel 1-x, channel 0 is not counted
      int maxPrintBottom = getReportEngine().getMaxPrintBottomCh();    //Channel 1-y, channel 0 is not counted

      // General information
      aPrintWriter.print(aPrintData.getScatter(aMode).getCompId().getObservationDate());
      Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getObservationTime());
      Print.commaAndValue(aPrintWriter, df.format(aPrintData.getObservation(aMode).getDistance()));
      Print.commaAndValue(aPrintWriter, df.format(aPrintData.getObservation(aMode).getDistance() + aPrintData.getScatter(aMode).getDistanceInterval()));
      Print.commaAndValue(aPrintWriter, df.format(aPrintData.getObservation(aMode).getLatitude()));
      Print.commaAndValue(aPrintWriter, df.format(aPrintData.getObservation(aMode).getLongitude()));
      Print.commaAndValue(aPrintWriter, df.format(aPrintData.getScatter(aMode).getMinBottomDepth()));
      Print.commaAndValue(aPrintWriter, df.format(aPrintData.getScatter(aMode).getMaxBottomDepth()));
      Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getFrequency());
      Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getTransceiver());
      Print.commaAndValue(aPrintWriter, df.format(aPrintData.getScatter(aMode).getThreshold()));
      Print.commaAndValue(aPrintWriter, aPrintData.getAcousticCategory(aAcousticCategoryIndex).getCompId().getAcousticCategory());

      Print.commaAndValue(aPrintWriter, df.format(aPrintData.getScatter(aMode).getChannelThickness()));
      Print.commaAndValue(aPrintWriter, maxPrintPelagic);
      Print.commaAndValue(aPrintWriter, aPrintData.getMaxChannel(aMode));

      // Pelagic sA values
      for (ch = 0; ch <= maxPrintPelagic; ch++) {  // ch=0 is sum channel, ch>0 is real channels
         Print.commaAndValue(aPrintWriter, saFormat.format(dataPrint[aAcousticCategoryIndex][ch]));
      }

      Scatter bottomScatter = aPrintDataBottom.getScatter(aMode);
      if (bottomScatter != null) {
         float bottomDepth = bottomScatter.getMaxBottomDepth();
         float upper = bottomDepth + bottomScatter.getUpperInterpretationDepth();
         if (upper < bottomScatter.getUpperDepth()) {
            upper = bottomScatter.getUpperDepth();
         }
         float bottomThickness = bottomDepth - upper;
         Print.commaAndValue(aPrintWriter, df.format(bottomScatter.getChannelThickness()));
         maxBottomCh = (int) Math.ceil(bottomThickness / bottomScatter.getChannelThickness());
      } else {
         Print.commaAndValue(aPrintWriter, df.format(0));
      }
      Print.commaAndValue(aPrintWriter, maxPrintBottom);
      Print.commaAndValue(aPrintWriter, maxBottomCh);

      // sA values from bottom
      for (ch = 0; ch <= maxPrintBottom; ch++) {  // ch=0 is sum channel, ch>0 is real channels
         Print.commaAndValue(aPrintWriter, saFormat.format(bottomDataPrint[aAcousticCategoryIndex][ch]));
      }

      aPrintWriter.println();
   } // print()
}
