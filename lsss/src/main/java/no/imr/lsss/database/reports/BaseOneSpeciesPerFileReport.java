package no.imr.lsss.database.reports;

import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

abstract class BaseOneSpeciesPerFileReport extends BaseSingleFrequencyReport {
   private List<PrintWriter> printWriters = List.of();

   BaseOneSpeciesPerFileReport(int type, ReportEngine reportEngine) {
      super(type, reportEngine);
   }

   @Override
   void open(PrintData.Pelagic aPrintData, Path aDirectory, ReportMode aMode, int aPrintFrequency, short aPrintTransceiver, float aStartObservationDistance, float aStopObservationDistance) throws IOException {
      close();
      int n = Math.min(aPrintData.getPrintCount(), getReportEngine().getMaxSpecialReportSpecies());
      printWriters = new ArrayList<>(n);
      for (int j = 0; j < n; j++) {
         String postfix = getLanguageUtils().getAcCatInitials(aPrintData.getAcousticCategory(j));
         postfix = postfix.replace('/', '-');
         postfix += GetPostfix.getPostfix(aPrintData);
         Path file;
         if (getReportEngine().getDistanceFileExtension()) {
            file = makeFile(aDirectory, aPrintFrequency, aPrintTransceiver, aStartObservationDistance, aStopObservationDistance, postfix);
         } else {
            file = makeFile(aDirectory, aPrintFrequency, aPrintTransceiver, postfix);
         }
         printWriters.add(FileUtils.newPrintWriter(file, getReportEngine().getCharset()));
         printHeader(printWriters.get(j), aPrintData, getType(), aMode);
      }
   }

   @Override
   void print(PrintData.Pelagic aPrintData, int aPrintFrequency, short aPrintTransceiver, ReportMode aMode, PrintData.Bottom aPrintDataBottom) {
      for (int j = 0; j < printWriters.size(); j++) {  // j = acousticCategoryIndex, but also gReportStream index
         int acousticCategoryIndex = j;
         print(printWriters.get(j), aPrintData, aPrintDataBottom, aMode, acousticCategoryIndex);
      }
   }

   abstract void print(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, PrintData.Bottom aPrintDataBottom, ReportMode aMode, int aAcousticCategoryIndex);

   @Override
   void close() {
      for (PrintWriter printWriter : printWriters) {
         printWriter.close();
      }
      printWriters = List.of();
   }
}
