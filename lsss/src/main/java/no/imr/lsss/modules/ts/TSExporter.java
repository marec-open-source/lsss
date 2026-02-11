package no.imr.lsss.modules.ts;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.Region;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.export.ExportFile;
import no.imr.lsss.framework.export.StreamingExporter;
import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.Listener;
import no.imr.tools.parameter.Name;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.time.NTDate;
import no.marec.lsss.api.util.GeoPoint;
import tools.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

public final class TSExporter extends StreamingExporter {
   public TSExporter(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("TSExport", "TS"), "TS targets in selected regions");
   }

   @Override
   public boolean isJson() {
      return false;
   }

   @Override
   public void doExport(AsyncHandle asyncHandle, ProgressHandler progressHandler) throws IOException {
      PingRange pingRange = getLSSS().getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return;
      }
      DataFileSet dataFileSet = getLSSS().getInterpretationSettings().getDataFileSet();

      ExportFile tsFile = new ExportFile("TS", ".txt");
      ExportFile cdsFile = createCdsFileWrapper();
      initExportFiles(pingRange, tsFile, cdsFile);
      writeModuleSetup(cdsFile, dataFileSet, pingRange);

      try (PrintWriter out = tsFile.newPrintWriter()) {
         write(asyncHandle, progressHandler, out);
      }
   }

   @Override
   public void exportToStream(AsyncHandle asyncHandle, ProgressHandler progressHandler, OutputStream out, ObjectWriter objectWriter) {
      try (PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(out, Utils.UTF_8))) {
         write(asyncHandle, progressHandler, printWriter);
      }
   }

   private void write(AsyncHandle asyncHandle, ProgressHandler progressHandler, PrintWriter out) {
      PingRange pingRange = getLSSS().getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return;
      }
      DataFileSet dataFileSet = getLSSS().getInterpretationSettings().getDataFileSet();
      TSModule tsModule = getLSSS().getModuleManager().getModule(TSModule.class);

      writeStandardHeader(out, "1.9.3");
      out.println("# Note that sV = 4 * PI * 1852 * 1852 * sv    (as everywhere else in LSSS)");
      out.println("# Target strength of visual parts of selected regions");
      out.println("# Frequency [Hz]: " + getLSSS().getInterpretationSettings().getFrequency());
      out.println("#");
      out.println("# Settings used to detect targets:");
      out.println("# MinTS = " + Utils.toString(tsModule.minTS.getFloatValue()));
      out.println("# MaxGainCompensation = " + Utils.toString(tsModule.maxGainCompensation.getFloatValue()));
      out.println("# PulseLengthDeterminationLevel = " + Utils.toString(tsModule.pulseLengthDeterminationLevel.getFloatValue()));
      out.println("# MinEchoLength = " + Utils.toString(tsModule.minEchoLength.getFloatValue()));
      out.println("# MaxEchoLength = " + Utils.toString(tsModule.maxEchoLength.getFloatValue()));
      out.println("# DoPhaseDeviationCheck = " + tsModule.doPhaseDeviationCheck.getBooleanValue());
      out.println("# MaxPhaseDevPhaseSteps = " + Utils.toString(tsModule.maxPhaseDevPhaseSteps.getFloatValue()));
      out.println("#");

      writeColumnNames(out, List.of("Date[YYYYMMDD]", "Time[HHMMSSXX]", "Latitude[deg]", "Longitude[deg]",
            "Range[m]", "TSC[dB re m2]", "TSU[dB re m2]", "AlongshipAngle[deg]", "AthwartshipAngle[deg]", "sV_of_peak"));

      int channel = getLSSS().getInterpretationSettings().getChannel();
      TsDataComputer tsDataComputer = tsModule.createTSDataComputer();
      List<Region> selectedRegions = getLSSS().getRegionManager().getSelectedRegions();
      List<PingIndex> visiblePingIndexes = getVisiblePingIndexes(selectedRegions);
      Listener progressListener = progressHandler.asCountingListener(visiblePingIndexes.size());
      for (PingIndex pingIndex : visiblePingIndexes) {
         progressListener.listen();
         Ping ping = dataFileSet.getPing(pingIndex);
         if (asyncHandle.isCancelled()) {
            return;
         }
         FloatRangeSet depthRanges = getDepthRanges(selectedRegions, ping, channel);

         for (TSData tsData : tsDataComputer.compute(ping, channel, depthRanges.getFloatRanges())) {
            GeoPoint geoPos = tsData.geoPos();
            DatabaseTime databaseTime = new DatabaseTime(NTDate.ntDateToTimeInMillis(tsData.ntDate()));
            List<String> values = List.of(Integer.toString(databaseTime.getDate()),
                  Utils.format("%08d", databaseTime.getTime()),
                  geoPos != null ? Utils.format("%.6f", geoPos.getLatitude()) : MISSING_LAT_LON,
                  geoPos != null ? Utils.format("%.6f", geoPos.getLongitude()) : MISSING_LAT_LON,
                  Utils.format("%.3f", tsData.range()),
                  Utils.format("%.2f", tsData.tsc()),
                  Utils.format("%.2f", tsData.tsu()),
                  Utils.format("%.2f", tsData.alongshipAngle()),
                  Utils.format("%.2f", tsData.athwartshipAngle()),
                  Utils.format("%.3f", tsData.sv()));
            writeValues(out, values);
         }
      }
   }
}
