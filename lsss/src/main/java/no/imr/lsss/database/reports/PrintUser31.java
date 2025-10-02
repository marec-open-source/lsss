package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.tools.Utils;
import no.imr.tools.io.Print;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

final class PrintUser31 extends BaseOneSpeciesPerFileReport {
   PrintUser31(ReportEngine reportEngine) {
      super(31, reportEngine);
   }

   @Override
   void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int printType, ReportMode aMode) {
      ReportUtils.writeStandardHeader(aPrintWriter, "1.7.2");
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
      aPrintWriter.printf("Transceiver[-],Threshold[dB],AcousticCategory[-]%n");

      // Line 2
      aPrintWriter.printf("%% ChannelThickness[m],MaxPelagicChannel[-],sA_Ch0,sA_Ch1,sA_Ch2,...");
      aPrintWriter.println();

      // Line 3
      aPrintWriter.printf("%% ChannelThickness[m],MaxBottomChannel[-],sA_Ch0,sA_Ch1,sA_Ch2,...");
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
      float[][] bottomDataPrint = aPrintDataBottom.getSa(aMode);

      // Line 1: general information
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
      Print.commaAndValue(aPrintWriter, df3.format(aPrintData.getScatter(aMode).getMinBottomDepth()));
      Print.commaAndValue(aPrintWriter, df3.format(aPrintData.getScatter(aMode).getMaxBottomDepth()));
      Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getFrequency());
      Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getTransceiver());
      Print.commaAndValue(aPrintWriter, df3.format(aPrintData.getScatter(aMode).getThreshold()));
      Print.commaAndValue(aPrintWriter, aPrintData.getAcousticCategory(aAcousticCategoryIndex).getCompId().getAcousticCategory());
      aPrintWriter.print(",");
      aPrintWriter.println();

      // Line 2:
      aPrintWriter.print(df3.format(aPrintData.getScatter(aMode).getChannelThickness()));

      //New: aPrintData.getMaxChannel() used instead of maxPelagicCh. aPrintData contains pelagic data.
      Print.commaAndValue(aPrintWriter, aPrintData.getMaxChannel(aMode));

      for (int ch = 0; ch <= aPrintData.getMaxChannel(aMode); ch++) { // ch=0 is sum channel, ch>0 is real channels
         Print.commaAndValue(aPrintWriter, df3.format(dataPrint[aAcousticCategoryIndex][ch]));
      }
      aPrintWriter.println();

      // Line 3:

      int maxBottomCh = 0;
      Scatter bottomScatter = aPrintDataBottom.getScatter(aMode);
      if (bottomScatter != null) {
         float bottomDepth = bottomScatter.getMaxBottomDepth();
         float upper = bottomDepth + bottomScatter.getUpperInterpretationDepth();
         if (upper < bottomScatter.getUpperDepth()) {
            upper = bottomScatter.getUpperDepth();
         }
         float bottomThickness = bottomDepth - upper;
         maxBottomCh = (int) Math.ceil(bottomThickness / bottomScatter.getChannelThickness());
         aPrintWriter.print(df3.format(bottomScatter.getChannelThickness()));
      } else {
         aPrintWriter.print(df3.format(0));
      }
      Print.commaAndValue(aPrintWriter, maxBottomCh);
      for (int ch = 0; ch <= maxBottomCh; ch++) { // ch=0 is sum channel, ch>0 is real channels
         Print.commaAndValue(aPrintWriter, df3.format(bottomDataPrint[aAcousticCategoryIndex][ch]));
      }
      aPrintWriter.println();
   } //print()
}
