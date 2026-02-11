package no.imr.korona.computation.categorization.netcdf;

import no.imr.korona.Korona;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.computation.ModuleUtils;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.computation.categorization.netcdf.pojo.AnnotationConfig;
import no.imr.korona.computation.netcdf.NcAnnotation;
import no.imr.korona.data.KoronaRegion;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.Cad0Datagram;
import no.imr.korona.data.datagrams.Cas0Datagram;
import no.imr.korona.data.datagrams.RegionBorderDatagram;
import no.imr.korona.data.datagrams.RegionInfoDatagram;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.util.geometry.EchogramUtils;
import no.imr.korona.viewer.ResampleMode;
import no.imr.korona.viewer.Resampler;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class CategorizationNetcdfWriterModuleComputation extends SimplePingModuleComputation {
   private final long referenceTimeInMillis;
   private final int referenceChannel;
   private final NetcdfFormatWriter writer;
   private final Variable pingTimeVar;
   private final Variable annotationVar;
   private final int rangeLength;
   private final FloatRange rangeRange;
   private final Map<Byte, Integer> categoryNumberToIndex;
   private int pingTimeIndex;
   private final List<PingIndex> pingIndices = new ArrayList<>();
   private final NavigableMap<PingIndex, PingInfo> pingInfos = new TreeMap<>();

   CategorizationNetcdfWriterModuleComputation(CategorizationNetcdfWriterModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
      super(module, computationContext, pingSource);

      Path koronaDirectory = computationContext.getAssociatedKoronaDirectory();
      if (koronaDirectory == null) {
         throw new ModuleConfigurationException(module, "No destination directory configured");
      }
      Path ncDir = koronaDirectory.resolve(module.dirName.getValue());
      FileUtils.createDirectories(ncDir);
      String ncFileName = FileUtils.baseName(computationContext.getPingReader().getFile()) + ".nc";
      Path ncFile = ncDir.resolve(ncFileName);

      AnnotationConfig annotationConfig = readAnnotationConfig(ncDir);
      Map<String, Integer> koronaNameToAnnotationId = annotationConfig.categories().stream()
            .collect(Collectors.toUnmodifiableMap(
                  annotationCategory -> annotationCategory.koronaCategory().name(),
                  AnnotationConfig.AnnotationCategory::id
            ));

      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      referenceTimeInMillis = pingConfiguration.getRawFileConfiguration().getTimeInMillis();

      referenceChannel = ModuleUtils.getMainChannelOrThrow(this, module.mainFrequency.getValue());

      Optional<Float> optDeltaRange = module.deltaRange.getValue();
      float deltaRange = optDeltaRange.isPresent()
            ? optDeltaRange.get()
            : ModuleUtils.getInputChannelDataOrThrow(this, referenceChannel).getSampleDistance();

      Optional<Float> optMaxRange = module.maxRange.getValue();
      float maxRange = optMaxRange.isPresent()
            ? optMaxRange.get()
            : ModuleUtils.getInputChannelDataOrThrow(this, referenceChannel).getMaxRange();

      rangeLength = (int) Math.floor(maxRange / deltaRange);
      rangeRange = FloatRange.of(0, deltaRange * rangeLength);

      Cac0Datagram cac0Datagram = pingConfiguration.getConfigurationItem(Cac0Datagram.class);
      if (cac0Datagram == null) {
         throw new ModuleConfigurationException(module, "Cannot find categorization configuration");
      }
      List<Cac0Datagram.Category> categories = cac0Datagram.getCategories();
      categoryNumberToIndex = IntStream.range(0, categories.size())
            .boxed()
            .collect(Collectors.toUnmodifiableMap(
                  i -> categories.get(i).getNumber(),
                  Function.identity()
            ));

      NetcdfFormatWriter.Builder fileBuilder = NcWrite.newBuilder(ncFile)
            .addAttribute(new Attribute("content_type_name", "CRIMAC-predictions"))
            .addAttribute(new Attribute("content_type_version", "0.1"))
            .addAttribute(new Attribute("content_type_description", "Categorization"))
            .addAttribute(new Attribute("producer_name", "KORONA"))
            .addAttribute(new Attribute("producer_version", Korona.VERSION))
            .addAttribute(new Attribute("producer_git_commit", Utils.GIT_COMMIT))
            .addAttribute(new Attribute("creation_time", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()));

      Dimension pingTimeDim = fileBuilder.addUnlimitedDimension(NcAnnotation.PING_TIME);
      Dimension rangeDim = fileBuilder.addDimension(NcAnnotation.RANGE, rangeLength);
      Dimension categoryDim = fileBuilder.addDimension(NcAnnotation.CATEGORY, categories.size());

      fileBuilder.addVariable(NcAnnotation.PING_TIME, DataType.LONG, List.of(pingTimeDim))
            .addAttribute(new Attribute(CF.CALENDAR, "proleptic_gregorian"))
            .addAttribute(new Attribute(CF.UNITS, "nanoseconds since " + Instant.ofEpochMilli(referenceTimeInMillis)));
      fileBuilder.addVariable(NcAnnotation.RANGE, DataType.DOUBLE, List.of(rangeDim));
      fileBuilder.addVariable(NcAnnotation.CATEGORY, DataType.INT, List.of(categoryDim));

      NcWrite.addFloatVariable(fileBuilder.getRootGroup(), NcAnnotation.ANNOTATION, List.of(categoryDim, pingTimeDim, rangeDim), List.of());

      writer = fileBuilder.build();

      try {
         try {
            pingTimeVar = findVariable(NcAnnotation.PING_TIME);
            annotationVar = findVariable(NcAnnotation.ANNOTATION);

            Variable categoryVar = findVariable(NcAnnotation.CATEGORY);
            int[] categoryArray = categories.stream()
                  .mapToInt(category -> {
                     return koronaNameToAnnotationId.getOrDefault(category.getName(),
                           1_000_000 + category.getNumber());
                  })
                  .toArray();
            writer.write(categoryVar, new int[]{0}, Array.makeFromJavaArray(categoryArray));

            Variable rangeVar = findVariable(NcAnnotation.RANGE);
            double[] ranges = new double[rangeLength];
            for (int i = 0; i < rangeLength; i++) {
               ranges[i] = i * deltaRange;
            }
            writer.write(rangeVar, new int[]{0}, Array.makeFromJavaArray(ranges));

         } catch (Exception e) {
            try {
               writer.close();
            } catch (IOException suppressed) {
               e.addSuppressed(suppressed);
            }
            throw e;
         }
      } catch (InvalidRangeException e) {
         throw new IOException(e);
      }
   }

   static AnnotationConfig readAnnotationConfig(Path ncDir) {
      Path file = ncDir.resolve("annotationConfig.json");
      try {
         return JsonUtils.JSON_MAPPER.readValue(file, AnnotationConfig.class);
      } catch (Exception e) {
         if (!FileUtils.notExists(e, file)) {
            Log.global.warning("Error reading file " + file + ": " + e);
         }
         return new AnnotationConfig(List.of());
      }
   }

   @Override
   protected void processPing(Ping ping) throws IOException {
      pingIndices.add(ping.getPingIndex());
      try {
         long t = (ping.getTimeInMillis() - referenceTimeInMillis) * 1_000_000;
         writer.write(pingTimeVar, new int[]{pingTimeIndex}, Array.makeFromJavaArray(new long[]{t}));

         ChannelData referenceChannelData = ping.getChannelData(referenceChannel);
         if (referenceChannelData == null) {
            return;
         }
         Cad0Datagram cad0Datagram = ping.getPingItem(Cad0Datagram.class);
         if (cad0Datagram == null) {
            return;
         }

         FloatRange categoryDepthRange = cad0Datagram.getDepthRange();
         FloatRange categoryRangeRange = categoryDepthRange.add(-referenceChannelData.getHeaveCorrectedTransducerDepth());

         int categoryCount = cad0Datagram.getCategoryCount();
         int pixelCount = cad0Datagram.getPixelCount();

         float[][] annotationDataBeforeResampling = new float[categoryNumberToIndex.size()][pixelCount];
         for (int categoryPriority = 0; categoryPriority < categoryCount; categoryPriority++) {
            for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
               byte categoryNumber = cad0Datagram.getCategory(categoryPriority, pixelIndex);
               int categoryIndex = categoryNumberToIndex.get(categoryNumber);
               float annotationValue = toAnnotationValue(categoryPriority);
               annotationDataBeforeResampling[categoryIndex][pixelIndex] = annotationValue;
            }
         }
         float[][] annotationData = new float[categoryNumberToIndex.size()][rangeLength];
         for (int categoryIndex = 0; categoryIndex < annotationData.length; categoryIndex++) {
            float[] outputData = annotationData[categoryIndex];
            float[] categoryValues = annotationDataBeforeResampling[categoryIndex];
            Resampler.sampleFloatData(categoryValues, categoryRangeRange, outputData, rangeRange, ResampleMode.AVERAGE);
         }

         PingInfo pingInfo = new PingInfo(pingTimeIndex, referenceChannelData.getHeaveCorrectedTransducerDepth(), annotationData, new HashSet<>());
         pingInfos.put(ping.getPingIndex(), pingInfo);

         processRegions(ping);

         writeFinishedPings();

         pingTimeIndex++;

      } catch (InvalidRangeException e) {
         throw new IOException(e);
      }
   }

   private static float toAnnotationValue(int categoryPriority) {
      // todo: Use probability, discriminant? But they do not preserve order.
      return 1.0f / (categoryPriority + 1);
   }

   private void processRegions(Ping ping) {
      ping.getPingItems(RegionBorderDatagram.class)
            .flatMap(regionBorderDatagram -> regionBorderDatagram.getBorderInfos().stream())
            .map(RegionBorderDatagram.BorderInfo::id)
            .forEach(pingInfos.get(ping.getPingIndex()).borderIds::add);

      Map<Integer, Cas0Datagram> regionIdToCas0 = ping.getPingItems(Cas0Datagram.class)
            .collect(Collectors.toMap(Cas0Datagram::getRegionId, Function.identity()));

      ping.getPingItems(RegionInfoDatagram.class).forEach(regionInfoDatagram -> {
         Set<Integer> borderIds = IntStream.of(regionInfoDatagram.getBorderIds())
               .boxed()
               .collect(Collectors.toSet());
         pingInfos.values().forEach(pingInfo -> pingInfo.borderIds.removeAll(borderIds));
         if (!regionInfoDatagram.isAccepted()) {
            return;
         }
         Cas0Datagram cas0Datagram = regionIdToCas0.get(regionInfoDatagram.getRegionId());
         if (cas0Datagram == null) {
            return;
         }
         PingContainer pingContainer = EchogramUtils.listPingContainer(getPingConfiguration(), pingIndices);
         NavigableMap<PingIndex, FloatRangeSet> mask = KoronaRegion.createMask(regionInfoDatagram, pingContainer);
         mask.forEach((pingIndex, regionDepthRanges) -> {
            PingInfo pingInfo = pingInfos.get(pingIndex);
            if (pingInfo == null) {
               return;
            }
            for (FloatRange regionDepthRange : regionDepthRanges) {
               FloatRange regionRangeRange = regionDepthRange.add(-pingInfo.heaveCorrectedTransducerDepth);
               int iBegin = Math.round(Math.clamp(rangeRange.valueToFraction(regionRangeRange.min()), 0, 1) * rangeLength);
               int iEnd = Math.round(Math.clamp(rangeRange.valueToFraction(regionRangeRange.max()), 0, 1) * rangeLength);

               // Remove pixel categorization:
               for (int categoryIndex = 0; categoryIndex < pingInfo.annotationData.length; categoryIndex++) {
                  Arrays.fill(pingInfo.annotationData[categoryIndex], iBegin, iEnd, 0);
               }

               // Use school categorization:
               int categoryCount = cas0Datagram.getCategoryCount();
               for (int categoryPriority = 0; categoryPriority < categoryCount; categoryPriority++) {
                  byte categoryNumber = cas0Datagram.getCategory(categoryPriority);
                  int categoryIndex = categoryNumberToIndex.get(categoryNumber);
                  float annotationValue = toAnnotationValue(categoryPriority);
                  Arrays.fill(pingInfo.annotationData[categoryIndex], iBegin, iEnd, annotationValue);
               }
            }
         });
      });
   }

   private void writeFinishedPings() throws IOException {
      while (true) {
         Map.Entry<PingIndex, PingInfo> firstEntry = pingInfos.firstEntry();
         if (firstEntry == null) {
            break;
         }
         PingInfo pingInfo = firstEntry.getValue();
         if (!pingInfo.borderIds.isEmpty()) {
            break;
         }
         write(pingInfo);
         pingInfos.remove(firstEntry.getKey());
      }
   }

   private void write(PingInfo pingInfo) throws IOException {
      try {
         for (int categoryIndex = 0; categoryIndex < pingInfo.annotationData.length; categoryIndex++) {
            writer.write(annotationVar, new int[]{categoryIndex, pingInfo.pingTimeIndex, 0},
                  Array.makeFromJavaArray(new float[][][]{{pingInfo.annotationData[categoryIndex]}}));
         }
      } catch (InvalidRangeException e) {
         throw new IOException(e);
      }
   }

   @Override
   public void close() throws IOException {
      for (PingInfo pingInfo : pingInfos.values()) {
         write(pingInfo);
      }
      pingInfos.clear();

      writer.close();
   }

   private Variable findVariable(String name) {
      return Objects.requireNonNull(writer.findVariable(name), name);
   }

   private record PingInfo(
         int pingTimeIndex,
         float heaveCorrectedTransducerDepth,
         float[][] annotationData,
         Set<Integer> borderIds
   ) {
   }
}
