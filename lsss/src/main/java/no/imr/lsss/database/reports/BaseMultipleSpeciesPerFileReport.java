package no.imr.lsss.database.reports;

import no.imr.tools.io.FileUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

abstract class BaseMultipleSpeciesPerFileReport extends BaseSingleFrequencyReport {
   private @Nullable PrintWriter printWriter;

   BaseMultipleSpeciesPerFileReport(int type, ReportEngine reportEngine) {
      super(type, reportEngine);
   }

   private PrintWriter printWriter() {
      if (printWriter == null) {
         throw new IllegalStateException("Writer is null");
      }
      return printWriter;
   }

   @Override
   void open(PrintData.Pelagic aPrintData, Path aDirectory, ReportMode aMode, int aPrintFrequency, short aPrintTransceiver, float aStartObservationDistance, float aStopObservationDistance) throws IOException {
      close();
      String postfix = GetPostfix.getPostfix(aPrintData);
      Path file;
      if (getReportEngine().getDistanceFileExtension()) {
         file = makeFile(aDirectory, aPrintFrequency, aPrintTransceiver, aStartObservationDistance, aStopObservationDistance, postfix);
      } else {
         file = makeFile(aDirectory, aPrintFrequency, aPrintTransceiver, postfix);
      }
      printWriter = FileUtils.newPrintWriter(file, getReportEngine().getCharset());
      printHeader(printWriter, aPrintData, getType(), aMode);
   }

   @Override
   void print(PrintData.Pelagic aPrintData, int aPrintFrequency, short aPrintTransceiver, ReportMode aMode, PrintData.Bottom aPrintDataBottom) {
      print(printWriter(), aPrintData, aPrintFrequency, aPrintTransceiver, aMode);
   }

   abstract void print(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int aPrintFrequency, short aPrintTransceiver, ReportMode aMode);

   @Override
   void close() {
      if (printWriter != null) {
         printWriter.close();
         printWriter = null;
      }
   }
}
