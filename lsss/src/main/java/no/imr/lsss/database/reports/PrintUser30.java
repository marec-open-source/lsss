package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.tools.Utils;
import no.imr.tools.io.Print;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

final class PrintUser30 extends BaseMultipleSpeciesPerFileReport {
   PrintUser30(ReportEngine reportEngine) {
      super(30, reportEngine);
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
         aPrintWriter.printf("%% Scatter type: SCHOOL(%d);  %n", aPrintData.getPrintDataScatterType());
      } else if (aPrintData.getPrintDataScatterType() == ScatterTypeEnum.PELAGIC.getValue()) {
         aPrintWriter.printf("%% Scatter type: SCATTER(%d);  %n", aPrintData.getPrintDataScatterType());
      }
      aPrintWriter.printf("%% Frequency: %d[Hz];  Transceiver: %d[-];  %n",
            aPrintData.getScatter(aMode).getCompId().getFrequency(),
            aPrintData.getScatter(aMode).getCompId().getTransceiver());
      aPrintWriter.printf("%%%n");

      aPrintWriter.printf("%% Object[-],Date[yyyymmdd],Time[hhmmssxx],StartLog[nmi],StopLog[nmi],Latitude[deg],");
      aPrintWriter.printf("Longitude[deg],Depth[m],sA_species1[m2/nmi2],...,Total[m2/nmi2], ...%n");

      aPrintWriter.print("Object,Date,Time,StartLog,StopLog,Latitude,Longitude,Depth");

      // Species already ordered. Initials for "TOTAL" printed below
      for (int i = 0; i < aPrintData.getPrintCount(); i++) {
         aPrintWriter.print(',');
         aPrintWriter.print(getLanguageUtils().getAcCatInitials(aPrintData.getAcousticCategory(i)));
      }

      aPrintWriter.print(",Total");
      aPrintWriter.println();
   }

   @Override
   void print(
         PrintWriter aPrintWriter,
         PrintData.Pelagic aPrintData,
         int aPrintFrequency, short aPrintTransceiver, ReportMode aMode) {
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df2 = new DecimalFormat("#0.00", dfs);
      DecimalFormat df5 = new DecimalFormat("#0.00000", dfs);

      Scatter scat = aPrintData.getScatter(aMode);
      float[][] dataPrint = aPrintData.getSa(aMode);

      // Print data
      aPrintWriter.print(aPrintData.getScatter(aMode).getCompId().getObject());
      Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getObservationDate());
      Print.commaAndValue(aPrintWriter, aPrintData.getScatter(aMode).getCompId().getObservationTime());
      //vessel distance
      Print.commaAndValue(aPrintWriter, df5.format(aPrintData.getObservation(aMode).getDistance()));
      Print.commaAndValue(aPrintWriter, df5.format(aPrintData.getObservation(aMode).getDistance() + scat.getDistanceInterval()));
      //position
      Print.commaAndValue(aPrintWriter, df5.format(aPrintData.getObservation(aMode).getLatitude()));
      Print.commaAndValue(aPrintWriter, df5.format(aPrintData.getObservation(aMode).getLongitude()));
      //average depth
      float averageDepth = (scat.getMinBottomDepth() + scat.getMaxBottomDepth()) / 2;
      Print.commaAndValue(aPrintWriter, df2.format(averageDepth));

      // Print species scatter and total_sum(i=aPrintData.getPrintCount)
      for (int i = 0; i <= aPrintData.getPrintCount(); i++) { // "i" is species number in array
         Print.commaAndValue(aPrintWriter, df2.format(dataPrint[i][0]));
      }

      aPrintWriter.println();
   } //print()
}
