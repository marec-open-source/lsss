package no.imr.lsss.database.reports;

import org.jspecify.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;

abstract class BaseMultiFrequencyTextReport extends BaseMultiFrequencyReport {
   private @Nullable PrintWriter printWriter;

   BaseMultiFrequencyTextReport(int type, ReportEngine reportEngine) {
      super(type, reportEngine);
   }

   private PrintWriter printWriter() {
      if (printWriter == null) {
         throw new IllegalStateException("Writer is null");
      }
      return printWriter;
   }

   @Override
   void open(PrintData.Pelagic aPrintData, Path aDirectory, ReportMode aMode, float aStartObservationDistance, float aStopObservationDistance) throws IOException {
      String postfix = GetPostfix.getPostfix(aPrintData);
      Path file;
      if (getReportEngine().getDistanceFileExtension()) {
         file = makeFile(aDirectory, aStartObservationDistance, aStopObservationDistance, postfix);
      } else {
         file = makeFile(aDirectory, postfix);
      }
      OutputStream outputStream = getReportEngine().newOutputStream(file);
      printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, getReportEngine().getCharset())));
      printHeader(printWriter, aPrintData);
   }

   abstract void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData);

   @Override
   void print(PrintData.Pelagic aPrintData, ReportMode aMode, PrintData.Bottom aPrintDataBottom) {
      print(printWriter(), aPrintData, aMode, aPrintDataBottom);
   }

   abstract void print(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, ReportMode aMode, PrintData.Bottom aPrintDataBottom);

   @Override
   void close() {
      if (printWriter != null) {
         printWriter.close();
         printWriter = null;
      }
   }
}
