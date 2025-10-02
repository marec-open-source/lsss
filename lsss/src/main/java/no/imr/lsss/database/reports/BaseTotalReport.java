package no.imr.lsss.database.reports;

import no.imr.tools.io.FileUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

abstract class BaseTotalReport extends BaseReport {
   private @Nullable PrintWriter printWriter;

   BaseTotalReport(int type, ReportEngine reportEngine) {
      super(type, reportEngine);
   }

   private PrintWriter printWriter() {
      if (printWriter == null) {
         throw new IllegalStateException("Writer is null");
      }
      return printWriter;
   }

   void open(PrintTotalData aPrintTotalData, Path aDirectory) throws IOException {
      String postfix = PrintTotalData.getScatterTypeName(aPrintTotalData.getScatterType());
      Path file = makeFile(aDirectory, aPrintTotalData.getFrequency(), aPrintTotalData.getTransceiver(), postfix);
      printWriter = FileUtils.newPrintWriter(file, getReportEngine().getCharset());
      printHeader(printWriter, aPrintTotalData);
   }

   abstract void printHeader(PrintWriter aPrintWriter, PrintTotalData aPrintTotalData);

   void print(PrintTotalData aPrintTotalData) {
      print(printWriter(), aPrintTotalData);
   }

   abstract void print(PrintWriter aPrintWriter, PrintTotalData aPrintTotalData);

   @Override
   void close() {
      if (printWriter != null) {
         printWriter.close();
         printWriter = null;
      }
   }

   private Path makeFile(Path aDir, int aFrequency, short aTransceiver, String aPostfix) {
      return aDir.resolve(getFilePrefix() +
            "F" + aFrequency +
            "_T" + aTransceiver +
            aPostfix +
            getFileSuffix());
   }
}
