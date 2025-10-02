package no.imr.lsss.modules.sv;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Lists;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.region.Region;
import no.imr.korona.region.School;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.export.ExportFile;
import no.imr.lsss.framework.export.StreamingExporter;
import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.Listener;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeMap;
import no.imr.tools.time.NTDate;
import no.marec.lsss.api.util.GeoPoint;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

public final class SvExporter extends StreamingExporter {
   private final BooleanParameter allFrequencies = new BooleanParameter(
         new Name("AllFrequencies", "All frequencies"),
         false);

   private final BooleanParameter schoolsInSeparateFiles = new BooleanParameter(
         new Name("SchoolsInSeparateFiles", "Schools in separate files"),
         false);

   public SvExporter(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("Sv", "Sv"), "Sv sample values in selected regions");
   }

   @Override
   public boolean isJson() {
      return false;
   }

   @Override
   protected List<? extends BaseParameter<?>> getSettingsParameters() {
      return List.of(allFrequencies, schoolsInSeparateFiles);
   }

   @Override
   protected void doExport(AsyncHandle asyncHandle, ProgressHandler progressHandler) throws IOException {
      PingRange pingRange = getLSSS().getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return;
      }
      DataFileSet dataFileSet = getLSSS().getInterpretationSettings().getDataFileSet();

      ExportFile svFile = new ExportFile("Sv", ".txt");
      ExportFile cdsFile = createCdsFileWrapper();
      initExportFiles(pingRange, svFile, cdsFile);
      writeModuleSetup(cdsFile, dataFileSet, pingRange);

      Collection<? extends Region> regions;

      if (schoolsInSeparateFiles.getBooleanValue()) {
         for (School school : getLSSS().getRegionManager().getSchoolManager().getSelectedRegions()) {
            ExportFile schoolSvFile = new ExportFile("SvSchool" + school.getObjectNumber(), ".txt");
            initExportFiles(pingRange, schoolSvFile);
            doExport(asyncHandle, progressHandler, List.of(school), schoolSvFile);
         }
         regions = getLSSS().getRegionManager().getLayerManager().getSelectedRegions();
      } else {
         regions = getLSSS().getRegionManager().getSelectedRegions();
      }

      if (!regions.isEmpty()) {
         doExport(asyncHandle, progressHandler, regions, svFile);
      }
   }

   private void doExport(AsyncHandle asyncHandle, ProgressHandler progressHandler, Collection<? extends Region> regions, ExportFile exportFile) throws IOException {
      try (PrintWriter out = exportFile.newPrintWriter()) {
         write(asyncHandle, progressHandler, out, regions);
      }
   }

   @Override
   public void exportToStream(AsyncHandle asyncHandle, ProgressHandler progressHandler, OutputStream out, ObjectWriter objectWriter) {
      try (PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(out, Utils.UTF_8))) {
         write(asyncHandle, progressHandler, printWriter, getLSSS().getRegionManager().getSelectedRegions());
      }
   }

   private void write(AsyncHandle asyncHandle, ProgressHandler progressHandler, PrintWriter out, Collection<? extends Region> regions) {
      DataFileSet dataFileSet = getLSSS().getInterpretationSettings().getDataFileSet();

      writeStandardHeader(out, "1.9.0");

      List<PingIndex> pingIndexes = getVisiblePingIndexes(regions);
      FloatRange depthRange = getLSSS().getRegionManager().getDepthRange(regions);

      List<String> columns = Lists.newArrayList("PingNumber", "Frequency[kHz]", "Date[YYYYMMDD]", "Time[HHMMSSXX]", "Latitude[deg]", "Longitude[deg]",
            "RangeStart[m]", "RangeStop[m]", "DepthStart[m]", "DepthStop[m]", "SampleCount");
      int svColumnCount = getSvColumnCount(dataFileSet, pingIndexes, depthRange, asyncHandle);
      for (int i = 0; i < svColumnCount; i++) {
         columns.add("Sv" + (i + 1) + "[dB]");
      }

      writeColumnNames(out, columns);

      List<Integer> channels = allFrequencies.getBooleanValue()
            ? IntStream.rangeClosed(1, dataFileSet.getRawFileConfiguration().getTransducerCount()).boxed().toList()
            : List.of(getLSSS().getInterpretationSettings().getChannel());

      Listener progressListener = progressHandler.asCountingListener(pingIndexes.size());
      for (PingIndex pingIndex : pingIndexes) {
         progressListener.listen();
         Ping ping = dataFileSet.getPing(pingIndex);
         if (asyncHandle.isCancelled()) {
            return;
         }

         for (int channel : channels) {
            DatabaseTime databaseTime = new DatabaseTime(NTDate.ntDateToTimeInMillis(pingIndex.getNTDate()));
            GeoPoint geoPos = pingIndex.getGeographicalPosition();
            List<String> values = new ArrayList<>();
            values.add(Long.toString(pingIndex.getPingNumber()));
            values.add(Integer.toString(dataFileSet.getRawFileConfiguration().getTransducers().get(channel - 1).getKHz()));
            values.add(Integer.toString(databaseTime.getDate()));
            values.add(Utils.format("%08d", databaseTime.getTime()));
            values.add(geoPos != null ? Utils.format("%.6f", geoPos.getLatitude()) : MISSING_LAT_LON);
            values.add(geoPos != null ? Utils.format("%.6f", geoPos.getLongitude()) : MISSING_LAT_LON);

            PowerData powerData = ping.getPowerData(channel);
            if (powerData != null) {
               Range<Integer> indexRange = getIndexRange(powerData, depthRange);

               values.add(Utils.format("%.2f", powerData.depthToRange(depthRange.min())));
               values.add(Utils.format("%.2f", powerData.depthToRange(depthRange.max())));

               values.add(Utils.format("%.2f", dataFileSet.getDataConfiguration().depthToPhysicalDepth(depthRange.min())));
               values.add(Utils.format("%.2f", dataFileSet.getDataConfiguration().depthToPhysicalDepth(depthRange.max())));

               values.add(Integer.toString(indexRange.end() - indexRange.begin()));

               RangeMap<Integer, Boolean> indexes = new ArrayRangeMap<>();
               indexes.put(indexRange, false);
               getDepthRanges(regions, ping, channel).forEach(d -> {
                  int beginIndex = indexRange.clamp(powerData.depthToSampleIndex(d.min()));
                  int endIndex = indexRange.clamp(powerData.depthToSampleIndex(d.max()));
                  indexes.put(beginIndex, endIndex, true);
               });

               float[] logSv = powerData.getLogSv();
               for (RangeMap.Entry<Integer, Boolean> entry : indexes) {
                  for (int sampleIndex = entry.range().begin(); sampleIndex < entry.range().end(); sampleIndex++) {
                     if (entry.value() && sampleIndex >= 0 && sampleIndex < powerData.getCount()) {
                        values.add(Utils.format("%.1f", logSv[sampleIndex]));
                     } else {
                        values.add("");
                     }
                  }
               }
            }

            writeValues(out, values);
         }
      }
   }

   private static Range<Integer> getIndexRange(PowerData powerData, FloatRange depthRange) {
      int begin = powerData.depthToSampleIndex(depthRange.min());
      float endDepth = powerData.getSampleDepth(begin) + depthRange.getSize(); // Instead of depthRange.max to get same number of samples
      int end = powerData.depthToSampleIndex(endDepth);
      return new DefaultRange<>(begin, end);
   }

   private int getSvColumnCount(DataFileSet dataFileSet, List<PingIndex> pingIndexes, FloatRange depthRange, AsyncHandle asyncHandle) {
      for (PingIndex pingIndex : pingIndexes) {
         Ping ping = dataFileSet.getPing(pingIndex);
         if (asyncHandle.isCancelled()) {
            break;
         }
         PowerData powerData = ping.getPowerData(getLSSS().getInterpretationSettings().getChannel());
         if (powerData != null) {
            Range<Integer> indexRange = getIndexRange(powerData, depthRange);
            return indexRange.end() - indexRange.begin();
         }
      }
      return 0;
   }
}
