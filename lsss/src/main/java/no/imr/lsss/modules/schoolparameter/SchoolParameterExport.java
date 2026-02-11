package no.imr.lsss.modules.schoolparameter;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.region.Region;
import no.imr.korona.region.School;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.export.ExportFile;
import no.imr.lsss.framework.export.ExportUtils;
import no.imr.lsss.framework.export.StreamingExporter;
import no.imr.lsss.framework.export.pojo.ExportInfo;
import no.imr.tools.ProgressHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.Listener;
import no.imr.tools.misc.JsonWriter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.GeoPoint;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class SchoolParameterExport extends StreamingExporter {
   public SchoolParameterExport(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("SchoolParameters", "School parameters"), "School parameters for selected schools");
   }

   @Override
   protected void doExport(AsyncHandle asyncHandle, ProgressHandler progressHandler) throws IOException {
      ExportFile exportFile = new ExportFile(getName().persistentName(), ".json");
      initExportFiles("", exportFile);

      exportToStream(asyncHandle, progressHandler, exportFile);
   }

   @Override
   public void exportToStream(AsyncHandle asyncHandle, ProgressHandler progressHandler, OutputStream out, ObjectWriter objectWriter) {
      PingRange pingRange = getLSSS().getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return;
      }

      // Must wait until SchoolParameterModule is done:
      getLSSS().getInterpretationSettings().waitUntilFinished();

      DataFileSet dataFileSet = getLSSS().getInterpretationSettings().getDataFileSet();
      RawFileConfiguration rawFileConfiguration = dataFileSet.getRawFileConfiguration();

      List<Integer> channels = IntStream.rangeClosed(1, rawFileConfiguration.getTransducerCount()).boxed().toList();

      try (JsonGenerator json = objectWriter.createGenerator(out)) {
         json.writeStartObject();

         json.writePOJOProperty("info", getExportInfo());

         json.writeName("schools");
         json.writeStartArray();

         JsonWriter jsonWriter = new JsonWriter(json);
         List<School> schools = new ArrayList<>(getLSSS().getRegionManager().getSchoolManager().getSelectedRegions());
         schools.sort(Comparator.comparingInt(Region::getObjectNumber));
         Listener progressListener = progressHandler.asCountingListener(schools.size());
         for (School school : schools) {
            if (asyncHandle.isCancelled()) {
               return;
            }
            progressListener.listen();

            jsonWriter.writeObject(() -> {
               json.writeNumberProperty("objectNumber", school.getObjectNumber());
               json.writeBooleanProperty("dataProcessed", school.getParameters().dataProcessed());
               json.writePOJOProperty("scrutiny", ExportUtils.makeScrutiny(getLSSS(), channels, school.getInterpretation()));
               json.writeStringProperty("fileName", dataFileSet.getDataFile(school.getPingRange().begin()).getSegmentHandle().getDisplayName());

               json.writeStringProperty("timeStart", school.getPingRange().begin().getInstant().toString());
               json.writeStringProperty("timeEnd", school.getPingRange().end().getInstant().toString());

               GeoPoint beginGeoPos = school.getPingRange().begin().getGeographicalPosition();
               GeoPoint endGeoPos = school.getPingRange().end().getGeographicalPosition();
               json.writeNumberProperty("longitudeStart", ExportRounding.geoPos().applyAsDouble(beginGeoPos != null ? beginGeoPos.getLongitude() : Double.NaN));
               json.writeNumberProperty("longitudeEnd", ExportRounding.geoPos().applyAsDouble(endGeoPos != null ? endGeoPos.getLongitude() : Double.NaN));

               json.writeNumberProperty("latitudeStart", ExportRounding.geoPos().applyAsDouble(beginGeoPos != null ? beginGeoPos.getLatitude() : Double.NaN));
               json.writeNumberProperty("latitudeEnd", ExportRounding.geoPos().applyAsDouble(endGeoPos != null ? endGeoPos.getLatitude() : Double.NaN));

               for (Map.Entry<String, Float> entry : school.getParameters().values().entrySet()) {
                  json.writeNumberProperty(entry.getKey(), entry.getValue());
               }
               jsonWriter.writeArrayField("channels", school.getParameters().perChannelValues().entrySet(), perChannelEntry -> {
                  jsonWriter.writeObject(() -> {
                     int channel = perChannelEntry.getKey();
                     json.writeNumberProperty("frequency", rawFileConfiguration.getTransducers().get(channel - 1).getFrequency());
                     for (Map.Entry<String, Float> entry : perChannelEntry.getValue().entrySet()) {
                        json.writeNumberProperty(entry.getKey(), entry.getValue());
                     }
                  });
               });
            });
         }
         json.writeEndArray();

         json.writeEndObject();
      }
   }

   private ExportInfo getExportInfo() {
      ExportInfo exportInfo = new ExportInfo("2.8.0", getName().persistentName());

      exportInfo.units.put("objectNumber", "1");
      exportInfo.units.put("frequency", "Hz");
      exportInfo.units.put("longitudeStart", Unit.DEGREES.formalName());
      exportInfo.units.put("longitudeEnd", Unit.DEGREES.formalName());
      exportInfo.units.put("latitudeStart", Unit.DEGREES.formalName());
      exportInfo.units.put("latitudeEnd", Unit.DEGREES.formalName());

      SchoolParameterModule schoolParameterModule = getLSSS().getModuleManager().getModule(SchoolParameterModule.class);
      schoolParameterModule.getSchoolParameters().forEach((name, parameter) -> {
         exportInfo.units.put(name, parameter.unit().formalName());
      });

      return exportInfo;
   }
}
