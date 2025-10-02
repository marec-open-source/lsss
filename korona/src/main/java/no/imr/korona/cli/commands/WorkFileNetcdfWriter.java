package no.imr.korona.cli.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.imr.korona.Korona;
import no.imr.korona.cli.commands.pojo.AnnotationCoordinates;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.PingRangeBuilder;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.region.Layer;
import no.imr.korona.region.Mask;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.School;
import no.imr.korona.region.ThresholdManager;
import no.imr.tools.Utils;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeBuilder;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.range.IntRange;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeSet;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class WorkFileNetcdfWriter implements WorkFileProcessor {
   private static final String NC_CATEGORY = "category";
   private static final String NC_PING_TIME = "ping_time";
   private static final String NC_RANGE = "range";

   private static final String NC_ANNOTATION = "annotation";
   private static final String NC_OBJECT_NUMBER = "object_number";
   private static final String NC_OBJECT_TYPE = "object_type";
   private static final String NC_LOWER_THRESHOLD = "lower_threshold";
   private static final String NC_UPPER_THRESHOLD = "upper_threshold";

   private static final int OBJECT_NUMBER_NOTHING = -1;

   private static final int OBJECT_TYPE_LAYER = 3;
   private static final int OBJECT_TYPE_SCHOOL = 2;
   private static final int OBJECT_TYPE_DELETION = 1;
   private static final int OBJECT_TYPE_EXCLUSION = 0;
   private static final int OBJECT_TYPE_NOTHING = -1;

   private final Path outputDir;
   private final float frequency;
   private final float deltaRange;

   private final int rangeLength;

   private final AtomicInteger keyCounter = new AtomicInteger();

   WorkFileNetcdfWriter(Path outputDir, float frequency, float deltaRange, float maxRange) {
      this.outputDir = outputDir;
      this.frequency = frequency;
      this.deltaRange = deltaRange;

      rangeLength = (int) Math.floor(maxRange / deltaRange);
   }

   @Override
   public void process(DataManager dataManager, RegionManager regionManager, String workFileBaseName, @Nullable Element originalXml) throws IOException {
      DataFileSet dataFileSet = dataManager.getDataFileSet();
      int channel = dataFileSet.firstChannelClosestTo(frequency * 1000);
      if (channel < 0) {
         throw new IOException("No channel with frequency " + frequency);
      }
      List<Integer> categories = regionManager.regionStream()
            .flatMap(region -> region.getInterpretation().getChannelInterpretation(channel).getAssignments().keySet().stream())
            .distinct()
            .sorted()
            .toList();
      if (categories.isEmpty()) {
         categories = List.of(-1);
      }
      Map<Integer, Integer> categoryToIndex = IntStream.range(0, categories.size())
            .boxed()
            .collect(Collectors.toUnmodifiableMap(categories::get, Function.identity()));

      int exclusionObjectNumber = regionManager.getExclusionManager().getExclusions().isEmpty()
            ? -1
            : regionManager.getRegionConfiguration().nextObjectNumber();

      int deletionObjectNumber = regionManager.getMaskingManager().getMask(channel).isEmpty()
            ? -1
            : regionManager.getRegionConfiguration().nextObjectNumber();

      List<PingIndex> pingIndices = dataFileSet.getPingIndices();

      long referenceTimeInMillis = dataFileSet.getRawFileConfiguration().getTimeInMillis();

      Path ncFile = outputDir.resolve(workFileBaseName + ".nc");
      NetcdfFormatWriter.Builder fileBuilder = NcWrite.newBuilder(ncFile);
      Group.Builder rootGroupBuilder = fileBuilder.getRootGroup();
      rootGroupBuilder
            .addAttribute(new Attribute("annotation_coordinates", getAnnotationCoordinates(regionManager, dataFileSet, channel,
                  exclusionObjectNumber, deletionObjectNumber)))
            .addAttribute(new Attribute("name", "KORONA"))
            .addAttribute(new Attribute("description", "CRIMAC labels"))
            .addAttribute(new Attribute("time", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()))
            .addAttribute(new Attribute("version", Korona.VERSION))
            .addAttribute(new Attribute("git_commit", Utils.GIT_COMMIT));

      Dimension categoryDim = fileBuilder.addDimension(NC_CATEGORY, categories.size());
      Dimension pingTimeDim = fileBuilder.addDimension(NC_PING_TIME, pingIndices.size());
      Dimension rangeDim = fileBuilder.addDimension(NC_RANGE, rangeLength);

      fileBuilder.addVariable(NC_CATEGORY, DataType.LONG, List.of(categoryDim));
      fileBuilder.addVariable(NC_PING_TIME, DataType.LONG, List.of(pingTimeDim))
            .addAttribute(new Attribute(CF.CALENDAR, "proleptic_gregorian"))
            .addAttribute(new Attribute(CF.UNITS, "nanoseconds since " + Instant.ofEpochMilli(referenceTimeInMillis)));
      fileBuilder.addVariable(NC_RANGE, DataType.DOUBLE, List.of(rangeDim));

      NcWrite.addFloatVariable(rootGroupBuilder, NC_ANNOTATION, List.of(categoryDim, pingTimeDim, rangeDim), List.of());
      NcWrite.addVariable(rootGroupBuilder, NC_OBJECT_NUMBER, DataType.LONG, List.of(pingTimeDim, rangeDim));
      NcWrite.addVariable(rootGroupBuilder, NC_OBJECT_TYPE, DataType.LONG, List.of(pingTimeDim, rangeDim));
      NcWrite.addFloatVariable(rootGroupBuilder, NC_LOWER_THRESHOLD, List.of(pingTimeDim), List.of());
      NcWrite.addFloatVariable(rootGroupBuilder, NC_UPPER_THRESHOLD, List.of(pingTimeDim), List.of());

      try (NetcdfFormatWriter writer = fileBuilder.build()) {
         Variable categoryVar = writer.findVariable(NC_CATEGORY);
         Variable pingTimeVar = writer.findVariable(NC_PING_TIME);
         Variable rangeVar = writer.findVariable(NC_RANGE);

         Variable annotationVar = writer.findVariable(NC_ANNOTATION);
         Variable objectNumberVar = writer.findVariable(NC_OBJECT_NUMBER);
         Variable objectTypeVar = writer.findVariable(NC_OBJECT_TYPE);
         Variable lowerThresholdVar = writer.findVariable(NC_LOWER_THRESHOLD);
         Variable upperThresholdVar = writer.findVariable(NC_UPPER_THRESHOLD);

         long[] categoryArray = categories.stream()
               .mapToLong(Integer::intValue)
               .toArray();
         writer.write(categoryVar, Array.makeFromJavaArray(categoryArray));

         long[] pingTimes = pingIndices.stream()
               .mapToLong(pingIndex -> (pingIndex.getTimeInMillis() - referenceTimeInMillis) * 1_000_000)
               .toArray();
         writer.write(pingTimeVar, Array.makeFromJavaArray(pingTimes));

         double[] ranges = new double[rangeLength];
         for (int i = 0; i < rangeLength; i++) {
            ranges[i] = i * deltaRange;
         }
         writer.write(rangeVar, Array.makeFromJavaArray(ranges));

         float[] lowerThresholds = new float[pingIndices.size()];
         float[] upperThresholds = new float[pingIndices.size()];
         if (originalXml != null && originalXml.element(ThresholdManager.XML_THRESHOLDING) != null) {
            for (int i = 0; i < pingIndices.size(); i++) {
               PingIndex pingIndex = pingIndices.get(i);
               FloatRange svRange = regionManager.getThresholdManager().getLinearSvRange(pingIndex);
               lowerThresholds[i] = svRange.min() / PowerData.IMR_CONSTANT;
               upperThresholds[i] = regionManager.getThresholdManager().isUpperThresholdActive(pingIndex)
                     ? svRange.max() / PowerData.IMR_CONSTANT
                     : Float.POSITIVE_INFINITY;
            }
         } else {
            // Original work file does not contain thresholds.
            // This was added in 2011, 425cc8ed415b78750f2bbf61b6f3b01e5bb4623d.
            Arrays.fill(lowerThresholds, Float.NaN);
            Arrays.fill(upperThresholds, Float.NaN);
         }
         writer.write(lowerThresholdVar, Array.makeFromJavaArray(lowerThresholds));
         writer.write(upperThresholdVar, Array.makeFromJavaArray(upperThresholds));

         long[] objectNumbers = new long[rangeLength];
         long[] objectTypes = new long[rangeLength];
         float[][] annotations = new float[categories.size()][rangeLength];

         for (int pingTimeIndex = 0; pingTimeIndex < pingIndices.size(); pingTimeIndex++) {
            PingIndex pingIndex = pingIndices.get(pingTimeIndex);

            Arrays.fill(objectNumbers, OBJECT_NUMBER_NOTHING);
            Arrays.fill(objectTypes, OBJECT_TYPE_NOTHING);
            Utils.fill(annotations, 0);

            if (regionManager.getExclusionManager().isExcluded(pingIndex)) {
               Arrays.fill(objectNumbers, exclusionObjectNumber);
               Arrays.fill(objectTypes, OBJECT_TYPE_EXCLUSION);
            } else {
               Ping ping = dataFileSet.getPing(pingIndex);
               ChannelData channelData = ping.getChannelData(channel);
               if (channelData == null) {
                  continue;
               }

               FloatRangeSet maskedDepthRanges = regionManager.getMaskingManager().getMask(channel).get(pingIndex);
               if (!maskedDepthRanges.isEmpty()) {
                  List<IntRange> maskedIndexRanges = depthRangesToIndexRanges(maskedDepthRanges, channelData);
                  fill(objectNumbers, maskedIndexRanges, deletionObjectNumber);
                  fill(objectTypes, maskedIndexRanges, OBJECT_TYPE_DELETION);
               }

               regionManager.regionStream().forEach(region -> {
                  FloatRangeSet depthRanges = regionManager.getDepthRangesForChannel(region, ping, channel);
                  List<IntRange> indexRanges = depthRangesToIndexRanges(depthRanges, channelData);
                  fill(objectNumbers, indexRanges, region.getObjectNumber());
                  fill(objectTypes, indexRanges, objectType(region));
                  Map<Integer, Float> assignments = region.getInterpretation().getChannelInterpretation(channel).getAssignments();
                  assignments.forEach((category, assignment) -> {
                     fill(annotations[categoryToIndex.get(category)], indexRanges, assignment);
                  });
               });
            }

            writer.write(objectNumberVar, new int[]{pingTimeIndex, 0}, Array.makeFromJavaArray(new long[][]{objectNumbers}));
            writer.write(objectTypeVar, new int[]{pingTimeIndex, 0}, Array.makeFromJavaArray(new long[][]{objectTypes}));
            for (int categoryIndex = 0; categoryIndex < categories.size(); categoryIndex++) {
               writer.write(annotationVar, new int[]{categoryIndex, pingTimeIndex, 0}, Array.makeFromJavaArray(new float[][][]{{annotations[categoryIndex]}}));
            }
         }
      } catch (Exception e) {
         throw new IOException("Error writing " + ncFile, e);
      }
   }

   private String getAnnotationCoordinates(RegionManager regionManager, DataFileSet dataFileSet, int channel,
                                           int exclusionObjectNumber, int deletionObjectNumber) throws JsonProcessingException {
      Map<String, AnnotationCoordinates> keyToAnnotationCoordinates = new LinkedHashMap<>();
      regionManager.regionStream()
            .sorted(Comparator.comparingInt(Region::getObjectNumber))
            .map(region -> {
               FloatRangeBuilder rangeBuilder = new FloatRangeBuilder();
               PingRange pingRange = region.getPingRange();
               for (PingIndex pingIndex : dataFileSet.getPingIndices(pingRange)) {
                  Ping ping = dataFileSet.getPing(pingIndex);
                  ChannelData channelData = ping.getChannelData(channel);
                  if (channelData == null) {
                     continue;
                  }
                  FloatRange depthRange = regionManager.getDepthRangesForChannel(region, ping, channel).getBoundingRange();
                  rangeBuilder.expand(channelData.depthToRange(depthRange.min()));
                  rangeBuilder.expand(channelData.depthToRange(depthRange.max()));
               }
               FloatRange range = rangeBuilder.toFloatRange();
               return new AnnotationCoordinates(
                     "OK",
                     switch (region) {
                        case Layer __ -> "Layer";
                        case School __ -> "School";
                     },
                     region.getObjectNumber(),
                     new AnnotationCoordinates.BoundingBox(
                           pingRange.begin().getInstant().toString(),
                           dataFileSet.previousOrSame(pingRange.end()).getInstant().toString(),
                           range.min(),
                           range.max()
                     ),
                     region.getInterpretation().getChannelInterpretation(channel).getAssignments().entrySet().stream()
                           .map(e -> new AnnotationCoordinates.Category(e.getKey(), e.getValue()))
                           .sorted(Comparator.comparingInt(AnnotationCoordinates.Category::category))
                           .toList()
               );
            })
            .forEach(annotationCoordinates -> {
               String key = keyCounter.incrementAndGet()
                     + "__" + annotationCoordinates.objectType()
                     + "-" + annotationCoordinates.objectNumber();
               keyToAnnotationCoordinates.put(key, annotationCoordinates);
            });

      RangeSet<PingIndex> exclusions = regionManager.getExclusionManager().getExclusions();
      if (!exclusions.isEmpty()) {
         Range<PingIndex> pingRange = exclusions.stream().reduce(PingRange.EMPTY_RANGE, Range::union);
         AnnotationCoordinates annotationCoordinates = new AnnotationCoordinates(
               "OK",
               "Exclusion",
               exclusionObjectNumber,
               new AnnotationCoordinates.BoundingBox(
                     pingRange.begin().getInstant().toString(),
                     dataFileSet.previousOrSame(pingRange.end()).getInstant().toString(),
                     Float.NEGATIVE_INFINITY,
                     Float.POSITIVE_INFINITY
               ),
               List.of()
         );
         String key = keyCounter.incrementAndGet()
               + "__" + annotationCoordinates.objectType()
               + "-" + annotationCoordinates.objectNumber();
         keyToAnnotationCoordinates.put(key, annotationCoordinates);
      }

      Mask mask = regionManager.getMaskingManager().getMask(channel);
      if (!mask.isEmpty()) {
         PingRangeBuilder pingRangeBuilder = new PingRangeBuilder();
         FloatRangeBuilder rangeBuilder = new FloatRangeBuilder();
         for (PingIndex pingIndex : dataFileSet.getPingIndices()) {
            FloatRangeSet depthRanges = mask.get(pingIndex);
            if (!depthRanges.isEmpty()) {
               pingRangeBuilder.add(pingIndex);
               Ping ping = dataFileSet.getPing(pingIndex);
               ChannelData channelData = ping.getChannelData(channel);
               if (channelData == null) {
                  continue;
               }
               FloatRange depthRange = depthRanges.getBoundingRange();
               rangeBuilder.expand(channelData.depthToRange(depthRange.min()));
               rangeBuilder.expand(channelData.depthToRange(depthRange.max()));
            }
         }
         PingRange pingRange = pingRangeBuilder.build(dataFileSet);
         FloatRange range = rangeBuilder.toFloatRange();
         AnnotationCoordinates annotationCoordinates = new AnnotationCoordinates(
               "OK",
               "Deletion",
               deletionObjectNumber,
               new AnnotationCoordinates.BoundingBox(
                     pingRange.begin().getInstant().toString(),
                     dataFileSet.previousOrSame(pingRange.end()).getInstant().toString(),
                     range.min(),
                     range.max()
               ),
               List.of()
         );
         String key = keyCounter.incrementAndGet()
               + "__" + annotationCoordinates.objectType()
               + "-" + annotationCoordinates.objectNumber();
         keyToAnnotationCoordinates.put(key, annotationCoordinates);
      }

      return JsonUtils.JSON_MAPPER.writeValueAsString(keyToAnnotationCoordinates);
   }

   private static int objectType(Region region) {
      return switch (region) {
         case Layer __ -> OBJECT_TYPE_LAYER;
         case School __ -> OBJECT_TYPE_SCHOOL;
      };
   }

   private static void fill(long[] array, List<IntRange> indexRanges, int value) {
      for (IntRange indexRange : indexRanges) {
         Arrays.fill(array, indexRange.begin(), indexRange.end(), value);
      }
   }

   private static void fill(float[] array, List<IntRange> indexRanges, float value) {
      for (IntRange indexRange : indexRanges) {
         Arrays.fill(array, indexRange.begin(), indexRange.end(), value);
      }
   }

   private int rangeToIndex(float range) {
      return Math.clamp(Math.round(range / deltaRange), 0, rangeLength);
   }

   private List<IntRange> depthRangesToIndexRanges(FloatRangeSet depthRanges, ChannelData channelData) {
      return depthRanges.getFloatRanges().stream()
            .map(depthRange -> depthRangeToIndexRange(channelData, depthRange))
            .toList();
   }

   private IntRange depthRangeToIndexRange(ChannelData channelData, FloatRange depthRange) {
      return new IntRange(
            rangeToIndex(channelData.depthToRange(depthRange.min())),
            rangeToIndex(channelData.depthToRange(depthRange.max()))
      );
   }
}
