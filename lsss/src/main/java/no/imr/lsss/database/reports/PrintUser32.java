package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.tools.Utils;
import no.imr.tools.io.Print;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

final class PrintUser32 extends BaseOneSpeciesPerFileReport {
   PrintUser32(ReportEngine reportEngine) {
      super(32, reportEngine);
   }

   @Override
   void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int printType, ReportMode aMode) {
      ReportUtils.writeStandardHeader(aPrintWriter, "2.13.0");
      aPrintWriter.printf("%% Compact printout sorted by object-number. A school is an object, and also the ");
      aPrintWriter.printf("echogram visible when storing%n");
      aPrintWriter.printf("%% Nation: %s", aPrintData.getNation().getNationName());
      aPrintWriter.printf("(%d);  ", aPrintData.getNation().getNation());
      aPrintWriter.printf("Platform: %s", aPrintData.getPlatform().findPlatformName(aPrintData.getSurvey()));
      aPrintWriter.printf("(%d);  %n", aPrintData.getPlatform().getCompId().getPlatform());
      aPrintWriter.printf("%% Survey: %s (%d);  %n", aPrintData.getSurvey().getSurveyTitle(),
            aPrintData.getSurvey().getCompId().getSurvey());
      aPrintWriter.printf("%% Description: %s;  %n", aPrintData.getSurvey().getSurveyDescription().replace("\n", "\n% "));
      if (aPrintData.getPrintDataScatterType() == ScatterTypeEnum.PELAGIC_SCHOOL.getValue()) {
         aPrintWriter.printf("%% Scatter type: SCHOOL (%d);  %n", aPrintData.getPrintDataScatterType());
      } else if (aPrintData.getPrintDataScatterType() == ScatterTypeEnum.PELAGIC.getValue()) {
         aPrintWriter.printf("%% Scatter type: SCATTER (%d);  %n", aPrintData.getPrintDataScatterType());
      }
      aPrintWriter.printf("%% Frequency: %d[Hz];  Transceiver: %d[-];  %n",
            aPrintData.getScatter(aMode).getCompId().getFrequency(),
            aPrintData.getScatter(aMode).getCompId().getTransceiver());
      aPrintWriter.printf("%%%n");

      // Line 1
      aPrintWriter.printf("%% Survey[-],Nation[-],Platform[-],Object[-],Date[yyyymmdd],Time[hhmmssxx],StartLog[nmi],");
      aPrintWriter.printf("StopLog[nmi],Latitude[deg],Longitude[deg],MinBottomDepth[m],MaxBottomDepth[m],Frequency[Hz],");
      aPrintWriter.printf("Transceiver[-],Threshold[dB],AcousticCategory[-],");

      aPrintWriter.printf("%% ChannelThickness[m],MaxPelagicChannel[-],sA_Ch0,sA_Ch1,sA_Ch2,...");
      aPrintWriter.println();
   }

   @Override
   void print(
         PrintWriter aPrintWriter,
         PrintData.Pelagic aPrintData,
         PrintData.Bottom aPrintDataBottom,
         ReportMode aMode,
         int aAcousticCategoryIndex) { // Index in AcousticCategory array in PrintData
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df3 = new DecimalFormat("#0.000", dfs);
      DecimalFormat df5 = new DecimalFormat("#0.00000", dfs);

      float[][] dataPrint = aPrintData.getSa(aMode);

      // Do not print anything if the school for this species does not contain any data
      if (dataPrint[aAcousticCategoryIndex][0] <= 0.0) {
         return;
      }

      // General information
      aPrintWriter.print(aPrintData.getScatter(aMode).getCompId().getSurvey());
      Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getNation());
      Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getPlatform());

      Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getObject());
      Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getObservationDate());
      Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getObservationTime());
      Print.commaAndValue(aPrintWriter, df5.format(aPrintData.getObservation(aMode).getDistance()));
      Print.commaAndValue(aPrintWriter, df5.format(aPrintData.getObservation(aMode).getDistance() + aPrintData.getScatter(aMode).getDistanceInterval()));
      Print.commaAndValue(aPrintWriter, df5.format(aPrintData.getObservation(aMode).getLatitude()));
      Print.commaAndValue(aPrintWriter, df5.format(aPrintData.getObservation(aMode).getLongitude()));
      Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getFrequency());
      Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getTransceiver());
      Print.commaAndValue(aPrintWriter, df3.format(aPrintData.getScatter(aMode).getThreshold()));
      Print.commaAndValue(aPrintWriter, aPrintData.getAcousticCategory(aAcousticCategoryIndex).getCompId().getAcousticCategory());
      aPrintWriter.print(",");

      aPrintWriter.print(df3.format(aPrintData.getScatter(aMode).getChannelThickness()));

      // aPrintData.getMaxChannel() used instead of maxPelagicCh. aPrintData contains pelagic data.
      Print.commaAndValue(aPrintWriter, aPrintData.getMaxChannel(aMode));

      // Data from pelagic channels
      for (int ch = 0; ch <= aPrintData.getMaxChannel(aMode); ch++) { // ch=0 is sum channel, ch>0 is real channels
         Print.commaAndValue(aPrintWriter, df3.format(dataPrint[aAcousticCategoryIndex][ch]));
      }
      aPrintWriter.println();
   } //print()
}
