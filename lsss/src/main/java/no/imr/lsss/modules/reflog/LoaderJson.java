package no.imr.lsss.modules.reflog;

import no.imr.lsss.modules.reflog.pojo.RefLogField;
import no.imr.lsss.modules.reflog.pojo.RefLogFile;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

final class LoaderJson {
   private final JsonMapper jsonMapper = JsonMapper.builder()
         .build();
   private final Map<List<LogLineField>, List<LogLineField>> fieldNamesCache = new ConcurrentHashMap<>();

   LoaderJson() {
   }

   List<LogLine> load(Path file) throws IOException {
      try (InputStream in = Files.newInputStream(file)) {
         return load(in);
      }
   }

   List<LogLine> load(InputStream in) {
      RefLogFile refLogFile = jsonMapper.readValue(in, RefLogFile.class);

      int fieldCount = refLogFile.fields.size();
      List<LogLineField> fieldsBuilder = new ArrayList<>(fieldCount);
      List<String> fieldValues = new ArrayList<>(fieldCount);
      for (RefLogField field : refLogFile.fields) {
         if (field.name == null) {
            continue;
         }
         fieldsBuilder.add(new LogLineField(field.name, getUnit(field)));
         fieldValues.add(field.value != null ? field.value.toString() : "");
      }
      List<LogLineField> fields = fieldNamesCache.computeIfAbsent(fieldsBuilder, List::copyOf);

      ActivityType activityType = toActivityType(refLogFile.activityTypeCode);
      String localStationNumber = Objects.requireNonNullElse(refLogFile.localstationNumber, "");
      String name = Objects.requireNonNullElse(refLogFile.name, "");

      List<LogLine> logLines = new ArrayList<>();
      if (refLogFile.startTime != null) {
         logLines.add(new LogLine(refLogFile.startTime, activityType, true,
               name + " - start", localStationNumber,
               fields, fieldValues));
      }
      if (refLogFile.endTime != null) {
         logLines.add(new LogLine(refLogFile.endTime, activityType, false,
               name + " - stop", localStationNumber,
               fields, fieldValues));
      }
      return logLines;
   }

   private static String getUnit(RefLogField field) {
      if (field.extendedValue instanceof Map<?, ?> map) {
         Object measurementUnit = map.get("measurementUnit");
         if (measurementUnit != null) {
            return measurementUnit.toString();
         }
      }
      return "";
   }

   private static ActivityType toActivityType(@Nullable Integer activityTypeCode) {
      return switch (activityTypeCode) {
         case 1500 -> ActivityType.PELAGIC_TRAWL;
         case 1600 -> ActivityType.BOTTOM_TRAWL;
         case null, default -> ActivityType.OTHER;
      };
   }
}
