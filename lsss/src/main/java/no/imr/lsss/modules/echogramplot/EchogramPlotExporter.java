package no.imr.lsss.modules.echogramplot;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.export.ExportFile;
import no.imr.lsss.framework.export.StreamingExporter;
import no.imr.lsss.framework.export.pojo.ExportInfo;
import no.imr.lsss.modules.echogramplot.functions.BooleanFunction;
import no.imr.lsss.modules.echogramplot.functions.PingFunction;
import no.imr.tools.ProgressHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.Listener;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class EchogramPlotExporter extends StreamingExporter {
   public EchogramPlotExporter(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("EchogramPlot", "Echogram plot"), "The functions selected by the Echogram plot module");
   }

   @Override
   protected void doExport(AsyncHandle asyncHandle, ProgressHandler progressHandler) throws IOException {
      PingRange pingRange = getLSSS().getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return;
      }
      DataFileSet dataFileSet = getLSSS().getInterpretationSettings().getDataFileSet();

      ExportFile exportFile = new ExportFile("EchogramPlot", ".json");
      ExportFile cdsFile = createCdsFileWrapper();
      initExportFiles(pingRange, exportFile, cdsFile);
      writeModuleSetup(cdsFile, dataFileSet, pingRange);

      exportToStream(asyncHandle, progressHandler, exportFile);
   }

   @Override
   public void exportToStream(AsyncHandle asyncHandle, ProgressHandler progressHandler, OutputStream out, ObjectWriter objectWriter) {
      PingRange pingRange = getLSSS().getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return;
      }
      DataFileSet dataFileSet = getLSSS().getInterpretationSettings().getDataFileSet();

      EchogramPlotModule echogramPlotModule = getLSSS().getModuleManager().getModule(EchogramPlotModule.class);
      List<PingFunction> pingFunctions = echogramPlotModule.getSelectedPingFunctions();
      boolean plotAllChannels = echogramPlotModule.plotAllChannels.getBooleanValue();
      int transducerCount = dataFileSet.getRawFileConfiguration().getTransducerCount();
      int pingCount = pingRange.getPingCount();
      Listener progressListener = progressHandler.asCountingListener(pingCount);
      double[] time = new double[pingCount];
      double[] vesselDistance = new double[pingCount];
      long[] pingNumber = new long[pingCount];
      long[] timeInMillis = new long[pingCount];
      float[] bottom = new float[pingCount];
      List<Result> results = new ArrayList<>();
      List<PerChannelResult> perChannelResults = new ArrayList<>();
      for (PingFunction pingFunction : pingFunctions) {
         if (plotAllChannels && pingFunction.isChannelDependent()) {
            perChannelResults.add(new PerChannelResult(pingFunction, IntStream.range(0, transducerCount)
                  .mapToObj(_ -> new float[pingCount])
                  .toList()));
         } else {
            results.add(new Result(pingFunction, new float[pingCount]));
         }
      }
      int currentChannel = getLSSS().getInterpretationSettings().getChannel();
      int i = 0;
      for (PingIndex pingIndex : dataFileSet.getPingIndices(pingRange)) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         progressListener.listen();
         time[i] = PingMapping.ntDateToTimeValue(pingIndex.getNTDate());
         vesselDistance[i] = ExportRounding.vesselDistance().applyAsDouble(pingIndex.getVesselDistance());
         pingNumber[i] = pingIndex.getPingNumber();
         timeInMillis[i] = pingIndex.getTimeInMillis();
         bottom[i] = dataFileSet.getCoordinatedDepth(pingIndex);
         Ping ping = dataFileSet.getPing(pingIndex);
         for (Result result : results) {
            double value = result.pingFunction.compute(dataFileSet, ping, currentChannel);
            result.values[i] = (float) value;
         }
         for (PerChannelResult result : perChannelResults) {
            for (int channel = 1; channel <= transducerCount; channel++) {
               double value = result.pingFunction.compute(dataFileSet, ping, channel);
               result.channelValues.get(channel - 1)[i] = (float) value;
            }
         }
         i++;
      }

      try (JsonGenerator json = objectWriter.createGenerator(out)) {
         json.writeStartObject();

         json.writePOJOProperty("info", getExportInfo(pingFunctions, perChannelResults));

         json.writePOJOProperty("time", time);
         json.writePOJOProperty("vesselDistance", vesselDistance);
         json.writePOJOProperty("pingNumber", pingNumber);
         for (Result result : results) {
            float[] values = result.values;
            float[] postprocessedValues = result.pingFunction.postprocess(values, timeInMillis, bottom);
            Object exportValues = toExportValues(result.pingFunction, postprocessedValues);
            json.writePOJOProperty(result.pingFunction.getName().persistentName(), exportValues);
         }
         if (!perChannelResults.isEmpty()) {
            List<Object> channels = new ArrayList<>();
            for (int channel = 1; channel <= transducerCount; channel++) {
               RawFileTransducer transducer = dataFileSet.getRawFileConfiguration().getTransducers().get(channel - 1);
               Map<String, Object> channelMap = new LinkedHashMap<>();
               channelMap.put("id", transducer.getChannelId());
               channelMap.put("nominalFrequency", transducer.getFrequency());
               for (PerChannelResult result : perChannelResults) {
                  float[] values = result.channelValues.get(channel - 1);
                  float[] postprocessedValues = result.pingFunction().postprocess(values, timeInMillis, bottom);
                  Object exportValues = toExportValues(result.pingFunction, postprocessedValues);
                  channelMap.put(result.pingFunction.getName().persistentName(), exportValues);
               }
               channels.add(channelMap);
            }
            json.writePOJOProperty("channels", channels);
         }

         json.writeEndObject();
      }
   }

   private ExportInfo getExportInfo(List<PingFunction> pingFunctions, List<PerChannelResult> perChannelResults) {
      ExportInfo exportInfo = new ExportInfo("2.16.0", getName().persistentName());

      exportInfo.units.put("time", Unit.SECONDS_SINCE_EPOCH.formalName());
      exportInfo.units.put("vesselDistance", Unit.NAUTICAL_MILES.formalName());
      exportInfo.units.put("pingNumber", Unit.COUNT.formalName());
      pingFunctions.forEach(pingFunction -> {
         String unit = pingFunction.getUnit().formalName();
         if (!unit.isEmpty()) {
            exportInfo.units.put(pingFunction.getName().persistentName(), unit);
         }
      });
      if (!perChannelResults.isEmpty()) {
         exportInfo.units.put("nominalFrequency", Unit.HZ.formalName());
      }

      return exportInfo;
   }

   private static Object toExportValues(PingFunction pingFunction, float[] values) {
      return switch (pingFunction) {
         case BooleanFunction _ -> {
            int[] exportValues = new int[values.length];
            for (int i = 0; i < values.length; i++) {
               exportValues[i] = (int) values[i];
            }
            yield exportValues;
         }
         case PingFunction f -> {
            ExportTransform transform = f.getParameterExport().transform();
            float[] exportValues = new float[values.length];
            for (int i = 0; i < values.length; i++) {
               exportValues[i] = (float) transform.applyAsDouble(values[i]);
            }
            yield exportValues;
         }
      };
   }

   private record Result(PingFunction pingFunction, float[] values) {
   }

   private record PerChannelResult(PingFunction pingFunction, List<float[]> channelValues) {
   }
}
