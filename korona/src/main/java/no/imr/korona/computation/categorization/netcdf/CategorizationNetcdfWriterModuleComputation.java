package no.imr.korona.computation.categorization.netcdf;

import no.imr.korona.Korona;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.computation.ModuleUtils;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.computation.categorization.netcdf.pojo.AnnotationConfig;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.Cad0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.viewer.ResampleMode;
import no.imr.korona.viewer.Resampler;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.misc.ThrowingSupplier;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

      Integer referenceKHz = module.mainFrequency.getValue().orElse(null);
      referenceChannel = referenceKHz != null
            ? pingConfiguration.getRawFileConfiguration().lastChannelWithKHz(referenceKHz)
            : 1;
      if (referenceChannel <= 0) {
         throw new ModuleConfigurationException(module, "Cannot find channel with " + referenceKHz + " kHz");
      }

      ThrowingSupplier<ChannelData, IOException> referenceChannelData = new ThrowingSupplier<>() {
         private @Nullable ChannelData channelData;

         @Override
         public ChannelData get() throws IOException {
            ChannelData channelData = this.channelData;
            if (channelData == null) {
               channelData = ModuleUtils.getInputChannelData(CategorizationNetcdfWriterModuleComputation.this, referenceChannel);
               if (channelData == null) {
                  throw new ModuleConfigurationException(module, "Cannot find data on channel " + referenceChannel + " with " + referenceKHz + " kHz");
               }
               this.channelData = channelData;
            }
            return channelData;
         }
      };
      Optional<Float> optDeltaRange = module.deltaRange.getValue();
      float deltaRange = optDeltaRange.isPresent() ? optDeltaRange.get() : referenceChannelData.get().getSampleDistance();
      Optional<Float> optMaxRange = module.maxRange.getValue();
      float maxRange = optMaxRange.isPresent() ? optMaxRange.get() : referenceChannelData.get().getMaxRange();
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

      Dimension pingTimeDim = fileBuilder.addUnlimitedDimension("ping_time");
      Dimension rangeDim = fileBuilder.addDimension("range", rangeLength);
      Dimension categoryDim = fileBuilder.addDimension("category", categories.size());

      fileBuilder.addVariable("ping_time", DataType.LONG, List.of(pingTimeDim))
            .addAttribute(new Attribute(CF.CALENDAR, "proleptic_gregorian"))
            .addAttribute(new Attribute(CF.UNITS, "nanoseconds since " + Instant.ofEpochMilli(referenceTimeInMillis)));
      fileBuilder.addVariable("range", DataType.DOUBLE, List.of(rangeDim));
      fileBuilder.addVariable("category", DataType.INT, List.of(categoryDim));

      NcWrite.addFloatVariable(fileBuilder.getRootGroup(), "annotation", List.of(categoryDim, pingTimeDim, rangeDim), List.of());

      writer = fileBuilder.build();

      try {
         try {
            pingTimeVar = findVariable("ping_time");
            annotationVar = findVariable("annotation");

            Variable categoryVar = findVariable("category");
            int[] categoryArray = categories.stream()
                  .mapToInt(category -> {
                     return koronaNameToAnnotationId.getOrDefault(category.getName(),
                           1_000_000 + category.getNumber());
                  })
                  .toArray();
            writer.write(categoryVar, new int[]{0}, Array.makeFromJavaArray(categoryArray));

            Variable rangeVar = findVariable("range");
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
         return JsonUtils.readValue(file, AnnotationConfig.class);
      } catch (Exception e) {
         if (!FileUtils.notExists(e, file)) {
            Log.global.warning("Error reading file " + file + ": " + e);
         }
         return new AnnotationConfig(List.of());
      }
   }

   @Override
   protected void processPing(Ping ping) throws IOException {
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

         float[][] annotationData = new float[categoryNumberToIndex.size()][pixelCount];
         for (int categoryPriority = 0; categoryPriority < categoryCount; categoryPriority++) {
            for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
               byte categoryNumber = cad0Datagram.getCategory(categoryPriority, pixelIndex);
               int categoryIndex = categoryNumberToIndex.get(categoryNumber);
               float annotationValue = 1.0f / (categoryPriority + 1);
               // todo: Use probability, discriminant? But they do not preserve order.
               annotationData[categoryIndex][pixelIndex] = annotationValue;
            }
         }

         float[] outputData = new float[rangeLength];
         for (int categoryIndex = 0; categoryIndex < annotationData.length; categoryIndex++) {
            float[] categoryValues = annotationData[categoryIndex];
            Resampler.sampleFloatData(categoryValues, categoryRangeRange, outputData, rangeRange, ResampleMode.AVERAGE);
            writer.write(annotationVar, new int[]{categoryIndex, pingTimeIndex, 0},
                  Array.makeFromJavaArray(new float[][][]{{outputData}}));
         }

         pingTimeIndex++;

      } catch (InvalidRangeException e) {
         throw new IOException(e);
      }
   }

   @Override
   public void close() throws IOException {
      writer.close();
   }

   private Variable findVariable(String name) {
      return Objects.requireNonNull(writer.findVariable(name), name);
   }
}
