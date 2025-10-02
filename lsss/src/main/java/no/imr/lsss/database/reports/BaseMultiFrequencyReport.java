package no.imr.lsss.database.reports;

import no.imr.tools.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

abstract class BaseMultiFrequencyReport extends BaseReport {
   BaseMultiFrequencyReport(int type, ReportEngine reportEngine) {
      super(type, reportEngine);
   }

   abstract void open(PrintData.Pelagic aPrintData, Path aDirectory, ReportMode aMode, float aStartObservationDistance, float aStopObservationDistance) throws IOException;

   void printMetadata(PrintData.Pelagic aPrintData) {
   }

   abstract void print(PrintData.Pelagic aPrintData, ReportMode aMode, PrintData.Bottom aPrintDataBottom);

   Path makeFile(Path aDir, float aStartDistance, float aEndDistance, String aPostfix) {
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df = new DecimalFormat("#0.0", dfs);
      return aDir.resolve(getFilePrefix() +
            "_L" + df.format(aStartDistance) +
            "-" + df.format(aEndDistance) +
            aPostfix +
            getFileSuffix());
   }

   Path makeFile(Path aDir, String aPostfix) {
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df = new DecimalFormat("#0.0", dfs);
      return aDir.resolve(getFilePrefix() +
            aPostfix +
            getFileSuffix());
   }
}
