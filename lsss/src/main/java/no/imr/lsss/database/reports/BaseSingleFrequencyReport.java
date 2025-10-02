package no.imr.lsss.database.reports;

import no.imr.tools.Utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

abstract class BaseSingleFrequencyReport extends BaseReport {
   BaseSingleFrequencyReport(int type, ReportEngine reportEngine) {
      super(type, reportEngine);
   }

   abstract void open(PrintData.Pelagic aPrintData, Path aDirectory, ReportMode aMode, int aPrintFrequency, short aPrintTransceiver, float aStartObservationDistance, float aStopObservationDistance) throws IOException;

   abstract void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int printType, ReportMode aMode);

   abstract void print(PrintData.Pelagic aPrintData, int aPrintFrequency, short aPrintTransceiver, ReportMode aMode, PrintData.Bottom aPrintDataBottom);

   Path makeFile(Path aDir, int aFrequency, short aTransceiver, float aStartDistance, float aEndDistance, String aSpeciesName) {
      aSpeciesName = aSpeciesName.replaceAll("/", "_");
      aSpeciesName = aSpeciesName.replaceAll("\\\\", "_");
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df = new DecimalFormat("#0.0", dfs);
      return aDir.resolve(getFilePrefix() +
            "_F" + String.format("%06d", aFrequency) +
            "_T" + String.format("%1d", aTransceiver) +
            "_L" + df.format(aStartDistance) + "-" + df.format(aEndDistance) +
            (!aSpeciesName.isEmpty() ? "_" + aSpeciesName : "") +
            getFileSuffix());
   }

   Path makeFile(Path aDir, int aFrequency, short aTransceiver, String aSpeciesName) {
      aSpeciesName = aSpeciesName.replaceAll("/", "_");
      aSpeciesName = aSpeciesName.replaceAll("\\\\", "_");
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df = new DecimalFormat("#0.0", dfs);
      return aDir.resolve(getFilePrefix() +
            "_F" + String.format("%06d", aFrequency) +
            "_T" + String.format("%1d", aTransceiver) +
            (!aSpeciesName.isEmpty() ? "_" + aSpeciesName : "") +
            getFileSuffix());
   }
}
