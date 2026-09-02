package no.imr.lsss.server.util;

import com.google.common.base.Splitter;
import com.google.common.primitives.Primitives;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.Region;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.server.pojo.ApiEchogramPoint;
import no.imr.lsss.server.pojo.ApiFloatRange;
import no.imr.lsss.server.pojo.ApiPingIndex;
import no.imr.lsss.server.pojo.ApiPingMask;
import no.imr.lsss.server.pojo.ParameterInfo;
import no.imr.lsss.server.pojo.values.FloatRangeValue;
import no.imr.lsss.server.pojo.values.ObjectValue;
import no.imr.lsss.server.pojo.values.StringListValue;
import no.imr.lsss.server.pojo.values.StringValue;
import no.imr.tools.NoCanDoException;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.misc.ThrowingConsumer;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.DynamicListParameter;
import no.imr.tools.parameter.MultiParameter;
import no.imr.tools.parameter.RangeParameter;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.parameter.VoidParameter;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.web.WebUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class LsssServerUtils {
   private LsssServerUtils() {
   }

   public static Response getZip(Path dir) {
      StreamingOutput streamingOutput = out -> {
         try (ZipOutputStream zipOutputStream = new ZipOutputStream(out)) {
            FileUtils.zip("", zipOutputStream, dir, new AsyncHandle(), _ -> true);
         }
      };
      return Response.ok(streamingOutput, WebUtils.APPLICATION_ZIP)
            .header("Content-Disposition", "attachment; filename=\"" + dir.getFileName() + ".zip\"")
            .build();
   }

   public static void saveZip(InputStream in, Path parentDir, String dirName) throws IOException {
      FileUtils.createDirectories(parentDir);
      Path newDir = Files.createTempDirectory(parentDir, dirName + "-new-");
      try {
         FileUtils.unzip(new ZipInputStream(in), newDir);
         if (FileUtils.isEmptyDirectory(newDir)) {
            Files.delete(newDir);
            throw new BadRequestException("No files extracted");
         }
         Path dir = parentDir.resolve(dirName);
         Path backupDir = null;
         if (Files.exists(dir)) {
            backupDir = Files.createTempDirectory(parentDir, dirName + "-backup-");
            Files.delete(backupDir);
            Files.move(dir, backupDir);
         }
         Files.move(newDir, dir);
         if (backupDir != null) {
            FileUtils.deleteRecursively(backupDir);
         }
      } catch (IOException e) {
         try {
            FileUtils.deleteRecursively(newDir);
         } catch (IOException suppressed) {
            e.addSuppressed(suppressed);
         }
         throw e;
      }
   }

   public static void doInGuiThread(Runnable runnable) {
      AtomicReference<@Nullable NoCanDoException> noCanDoExceptionRef = new AtomicReference<>();
      GuiUtils.invokeNowOrWait(() -> {
         try {
            runnable.run();
         } catch (NoCanDoException e) {
            noCanDoExceptionRef.set(e);
         }
      });
      NoCanDoException noCanDoException = noCanDoExceptionRef.get();
      if (noCanDoException != null) {
         throw new NoCanDoException(noCanDoException.getMessage(), noCanDoException);
      }
   }

   public static Stream<ParameterInfo> getParameterInfos(Configurable configurable) {
      return getParameterInfos("", configurable.getSubConfigurables());
   }

   private static Stream<ParameterInfo> getParameterInfos(String path, Collection<? extends Configurable> configurables) {
      return configurables.stream()
            .flatMap(configurable -> getParameterInfos(appendToPath(path, configurable), configurable));
   }

   private static Stream<ParameterInfo> getParameterInfos(String path, Configurable configurable) {
      return switch (configurable) {
         case ValueParameter<?> valueParameter -> {
            yield Stream.of(new ParameterInfo(path, valueParameter));
         }
         case RangeParameter rangeParameter -> {
            yield Stream.of(new ParameterInfo(path, rangeParameter));
         }
         case DynamicListParameter<?> dynamicListParameter -> {
            yield Stream.of(new ParameterInfo(path, dynamicListParameter));
         }
         case ButtonParameter buttonParameter -> {
            yield Stream.of(new ParameterInfo(path, buttonParameter));
         }
         default -> {
            yield getParameterInfos(path, configurable.getSubConfigurables());
         }
      };
   }

   private static String appendToPath(String path, Configurable configurable) {
      return path.isEmpty()
            ? configurable.getName().persistentName()
            : path + "/" + configurable.getName().persistentName();
   }

   private static BaseParameter<?> getParameter(Configurable configurable, String path) {
      for (String part : Splitter.on('/').omitEmptyStrings().split(path)) {
         configurable = configurable.getSubConfigurable(part);
         if (configurable == null) {
            throw new NotFoundException(path);
         }
      }
      if (configurable instanceof BaseParameter<?> parameter) {
         return parameter;
      }
      throw parameterPathException(path);
   }

   private static BadRequestException parameterPathException(String path) {
      return new BadRequestException("Parameter path '" + path + "' cannot be accessed this way");
   }

   public static ObjectValue getParameterValue(Configurable configurable, String path) {
      BaseParameter<?> parameter = getParameter(configurable, path);
      return switch (parameter) {
         case ValueParameter<?> valueParameter -> getParameterValue(valueParameter);
         case RangeParameter rangeParameter -> getParameterValue(rangeParameter);
         case DynamicListParameter<?> dynamicListParameter -> getParameterValue(dynamicListParameter);
         case ButtonParameter _ -> getButtonParameterValue();
         case MultiParameter<?> _ -> throw parameterPathException(path);
         case VoidParameter _ -> throw parameterPathException(path);
      };
   }

   public static ObjectValue getButtonParameterValue() {
      return new ObjectValue(null);
   }

   public static ObjectValue getParameterValue(DynamicListParameter<?> dynamicListParameter) {
      return new ObjectValue(dynamicListParameter.getStringValues());
   }

   public static ObjectValue getParameterValue(RangeParameter rangeParameter) {
      return new ObjectValue(new ApiFloatRange(rangeParameter.getValue()));
   }

   public static ObjectValue getParameterValue(ValueParameter<?> valueParameter) {
      Object value = valueParameter.getValue();
      Class<?> c = value.getClass();
      if (Primitives.isWrapperType(c)) {
         return new ObjectValue(value);
      }
      return new ObjectValue(valueParameter.getStringValue());
   }

   public static void setParameterValue(Configurable configurable, String path, String value) {
      BaseParameter<?> parameter = getParameter(configurable, path);
      switch (parameter) {
         case ValueParameter<?> valueParameter -> {
            StringValue stringValue = parseParameterValue(value, StringValue.class);
            valueParameter.setStringValue(stringValue.value);
         }
         case RangeParameter rangeParameter -> {
            FloatRangeValue floatRangeValue = parseParameterValue(value, FloatRangeValue.class);
            rangeParameter.setValue(floatRangeValue.value.toFloatRange());
         }
         case DynamicListParameter<?> dynamicListParameter -> {
            StringListValue stringListValue = parseParameterValue(value, StringListValue.class);
            dynamicListParameter.setStringValues(stringListValue.value);
         }
         case ButtonParameter buttonParameter -> {
            buttonParameter.notifyListeners();
         }
         case MultiParameter<?> _ -> throw parameterPathException(path);
         case VoidParameter _ -> throw parameterPathException(path);
      }
   }

   private static <T> T parseParameterValue(String value, Class<T> valueType) {
      try {
         return JsonUtils.JSON_MAPPER.readValue(value, valueType);
      } catch (Exception _) {
         throw new BadRequestException("Cannot parse value: " + value);
      }
   }

   public static <T extends Enum<T>> T nameToEnum(Class<T> clazz, String name) {
      try {
         return Enum.valueOf(clazz, name);
      } catch (Exception _) {
         throw new BadRequestException("Illegal value: " + name + ". Must be one of " + Arrays.toString(clazz.getEnumConstants()));
      }
   }

   public static String toJsonString(Object value) {
      try {
         return JsonUtils.JSON_MAPPER.writeValueAsString(value);
      } catch (Exception e) {
         throw new InternalServerErrorException("Error serializing value to JSON string", e);
      }
   }

   public static void validateEchogramPoints(List<ApiEchogramPoint> apiEchogramPoints, int expectedCount) {
      if (apiEchogramPoints.size() != expectedCount) {
         throw new BadRequestException("Expected " + expectedCount + " points, but got " + apiEchogramPoints.size());
      }
      int nullIndex = apiEchogramPoints.indexOf(null);
      if (nullIndex >= 0) {
         throw new BadRequestException("null at index " + nullIndex);
      }
   }

   public static @Nullable PingIndex toPingIndexOrNull(DataFileSet dataFileSet, ApiPingIndex apiPingIndex) {
      ValueAndPingMapping valueAndPingMapping = ValueAndPingMapping.from(apiPingIndex);
      if (valueAndPingMapping == null) {
         return null;
      }
      return toPingIndexOrNull(dataFileSet, valueAndPingMapping);
   }

   public static @Nullable PingIndex toPingIndexOrNull(DataFileSet dataFileSet, ValueAndPingMapping valueAndPingMapping) {
      if (dataFileSet.isEmpty()) {
         return null;
      }
      if (valueAndPingMapping.pingMapping() == PingMapping.NUMBER) {
         return dataFileSet.getPingIndexOrNull(Math.round(valueAndPingMapping.value()));
      }
      return dataFileSet.getClosestPingIndex(valueAndPingMapping.value(), valueAndPingMapping.pingMapping());
   }

   public static PingRange toPingRange(DataFileSet dataFileSet, List<ApiEchogramPoint> apiEchogramPoints) {
      validateEchogramPoints(apiEchogramPoints, 2);
      PingIndex a = toPingIndexOrNull(dataFileSet, apiEchogramPoints.get(0).pingIndex);
      PingIndex b = toPingIndexOrNull(dataFileSet, apiEchogramPoints.get(1).pingIndex);
      if (a == null) {
         a = dataFileSet.getTotalRange().begin();
      }
      if (b == null) {
         b = dataFileSet.getTotalRange().end();
      }
      return PingRange.ofUnsorted(a, b);
   }

   public static List<ApiEchogramPoint> toBoundingBox(Region region, LSSS lsss) {
      DataFileSet dataFileSet = lsss.getInterpretationSettings().getDataFileSet();
      PingRange pingRange = region.getPingRange();
      PingIndex lastPingIndex = dataFileSet.previousOrSame(pingRange.end());
      FloatRange depthRange = dataFileSet.getPingIndexStream(pingRange)
            .map(pingIndex -> lsss.getRegionManager().getNonMaskedRegionDepthRanges(region, pingIndex).getBoundingRange())
            .reduce(FloatRange.EMPTY_RANGE, FloatRange::union);
      return List.of(
            new ApiEchogramPoint(pingRange.begin(), depthRange.min()),
            new ApiEchogramPoint(lastPingIndex, depthRange.max())
      );
   }

   public static NavigableMap<PingIndex, FloatRangeSet> toMask(DataFileSet dataFileSet, List<ApiPingMask> apiPingMasks) {
      NavigableMap<PingIndex, FloatRangeSet> mask = new TreeMap<>();
      for (ApiPingMask apiPingMask : apiPingMasks) {
         PingIndex pingIndex = toPingIndexOrNull(dataFileSet, apiPingMask.pingIndex);
         if (pingIndex == null) {
            throw new BadRequestException("No ping index for " + toJsonString(apiPingMask));
         }
         List<FloatRange> depthRanges = apiPingMask.depthRanges.stream()
               .map(ApiFloatRange::toFloatRange)
               .toList();
         FloatRangeSet depthRangeSet = FloatRangeSet.of(depthRanges);
         if (!depthRangeSet.isEmpty()) {
            mask.put(pingIndex, depthRangeSet);
         }
      }
      return mask;
   }

   public static Survey getSurvey(LSSS lsss) {
      Survey survey = lsss.getConfigurationManager().getSurveyConf().getSurvey();
      if (survey == null) {
         throw new BadRequestException("No survey selected");
      }
      return survey;
   }

   public static void withTmpDir(String dirPrefix, ThrowingConsumer<Path, IOException> dirConsumer) throws IOException {
      Path mainTmpDir = Utils.getTmpDir();
      FileUtils.createDirectories(mainTmpDir);
      Path tmpDir = Files.createTempDirectory(mainTmpDir, dirPrefix);
      Utils.tryAndCleanup(() -> dirConsumer.accept(tmpDir), () -> FileUtils.deleteRecursively(tmpDir));
   }
}
