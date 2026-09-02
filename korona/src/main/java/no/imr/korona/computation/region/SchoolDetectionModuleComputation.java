package no.imr.korona.computation.region;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.computation.offset.TransducerParameterManager;
import no.imr.korona.computation.offset.TransducerParameters;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.data.datagrams.Dep0Datagram;
import no.imr.korona.data.datagrams.RegionBorderDatagram;
import no.imr.korona.data.datagrams.RegionInfoDatagram;
import no.imr.korona.data.datagrams.RegionTableOfContentsDatagram;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.data.util.geometry.EchogramUtils;
import no.imr.korona.data.util.mask.MaskOutlineTracer;
import no.imr.korona.data.util.mask.MaskUtils;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.Min;
import no.imr.tools.Utils;
import no.imr.tools.math.GeometryUtils;
import no.imr.tools.math.MathUtils;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.range.RangeMap;
import no.imr.tools.xml.XmlUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

final class SchoolDetectionModuleComputation extends SimplePingModuleComputation {
   private final SchoolDetectionModule module;
   private final int channel;
   private final float logSvThreshold;
   private final FloatRange svRange;
   private final FloatRange detectionDepthRange;
   private final float transducerMinRange;
   private final float transducerMaxRange;

   private final List<PingIndex> pingIndices = new ArrayList<>();
   private int relativePingNumberCounter;
   private double pingDistanceMeter;
   private final Set<DetectedRegion> activeRegions = new TreeSet<>(DetectedRegion.COMPARATOR);
   private final List<Instant> regionInfoInstants = new ArrayList<>();

   SchoolDetectionModuleComputation(SchoolDetectionModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
      super(module, computationContext, pingSource);

      this.module = module;
      channel = module.processLast.getBooleanValue()
            ? getPingConfiguration().getRawFileConfiguration().getTransducerCount()
            : module.channel.getIntValue();
      if (channel < 1 || channel > getPingConfiguration().getRawFileConfiguration().getTransducerCount()) {
         throw new ModuleConfigurationException(module, "Invalid channel: " + channel);
      }
      logSvThreshold = module.threshold.getFloatValue();
      svRange = FloatRange.of(PowerData.logSvToSv(logSvThreshold), Float.POSITIVE_INFINITY);

      detectionDepthRange = FloatRange.of(
            module.minDepth.getValue().orElse(Float.NEGATIVE_INFINITY),
            module.maxDepth.getValue().orElse(Float.POSITIVE_INFINITY));

      Path transducerRangesFile = module.getRequiredConfigFile(TransducerRangesFileService.NAME);
      TransducerParameterManager transducerParameterManager = new TransducerParameterManager(TransducerParameters.ParameterType.VERTICAL, XmlUtils.readDocument(transducerRangesFile));

      RawFileTransducer transducer = getPingConfiguration().getRawFileConfiguration().getTransducers().get(channel - 1);
      transducerMinRange = transducerParameterManager.getBlindZone(transducer.getKHz()).orElse(0f);
      transducerMaxRange = transducerParameterManager.getRange(transducer.getKHz()).orElse(Float.POSITIVE_INFINITY);
   }

   @Override
   protected void processPing(Ping ping) throws IOException {
      pingIndices.add(ping.getPingIndex());
      relativePingNumberCounter++;

      Ping nextPing = peekPingSourcePing(0);
      if (nextPing != null) {
         pingDistanceMeter = KoronaUtils.nmiToMeter(nextPing.getVesselDistance() - ping.getVesselDistance());
      }

      PowerData powerData = ping.getPowerData(channel);
      if (powerData == null) {
         endAllRegions(ping);
         return;
      }

      RangeMap<Float, DetectedInterval> detectedIntervals = detectIntervals(ping, powerData);

      List<DetectedRegion> endedRegions = new ArrayList<>();

      for (DetectedRegion region : activeRegions) {
         List<DetectedInterval> overlappingIntervals = region.prevDepthRanges.stream()
               .map(FloatRange::toRange)
               .flatMap(detectedIntervals::stream)
               .map(RangeMap.Entry::value)
               .toList();
         if (overlappingIntervals.isEmpty()) {
            endedRegions.add(region);
            continue;
         }
         DetectedInterval first = overlappingIntervals.getFirst();
         first.regionContinuation.overlappingRegions.add(region);
         for (int i = 1; i < overlappingIntervals.size(); i++) {
            DetectedInterval other = overlappingIntervals.get(i);
            first.mergeWith(other);
         }
      }

      for (DetectedRegion region : endedRegions) {
         endRegion(ping, region);
         activeRegions.remove(region);
      }

      List<RegionContinuation> regionContinuations = detectedIntervals.stream()
            .map(e -> e.value().regionContinuation)
            .distinct()
            .sorted(Comparator.comparingDouble(regionContinuation -> regionContinuation.depthRanges.getFloatRanges().getFirst().min()))
            .toList();

      for (RegionContinuation regionContinuation : regionContinuations) {
         DetectedRegion resultingRegion;
         if (regionContinuation.overlappingRegions.isEmpty()) {
            resultingRegion = new DetectedRegion(getComputationContext().nextSchoolId(), relativePingNumberCounter);
            activeRegions.add(resultingRegion);
         } else {
            List<DetectedRegion> regions = new ArrayList<>(regionContinuation.overlappingRegions);
            resultingRegion = regions.getFirst();
            for (int i = 1; i < regions.size(); i++) {
               DetectedRegion other = regions.get(i);
               resultingRegion.mergeWith(other);
               activeRegions.remove(other);
            }
         }
         resultingRegion.update(ping.getPingIndex(), powerData, regionContinuation.depthRanges, pingDistanceMeter);
      }

      if (!activeRegions.isEmpty()) {
         List<RegionBorderDatagram.BorderInfo> borderInfos = new ArrayList<>();
         for (DetectedRegion region : activeRegions) {
            for (FloatRange depthRange : region.prevDepthRanges) {
               borderInfos.add(new RegionBorderDatagram.BorderInfo(region.id, depthRange.min(), depthRange.max(), region.getMeanLogSv(), isAccepted(region)));
            }
         }
         ping.add(new RegionBorderDatagram(ping.getInstant(), channel, logSvThreshold, borderInfos));
      }
      if (nextPing == null) {
         endAllRegions(ping);
         if (!regionInfoInstants.isEmpty()) {
            ping.add(new RegionTableOfContentsDatagram(ping.getInstant(), regionInfoInstants));
         }
      }
   }

   private RangeMap<Float, DetectedInterval> detectIntervals(Ping ping, PowerData powerData) {
      float minDepth = Math.max(powerData.rangeToDepth(transducerMinRange), detectionDepthRange.min());
      float maxDepth = Min.of(findBottomDepthDepth(ping, powerData), powerData.rangeToDepth(transducerMaxRange), detectionDepthRange.max());
      List<FloatRange> depthRanges = DataUtils.findDepthRanges(powerData, FloatRange.of(minDepth, maxDepth), powerData.getSv(), svRange);

      RangeMap<Float, DetectedInterval> detectedIntervals = new ArrayRangeMap<>();
      for (FloatRange depthRange : depthRanges) {
         detectedIntervals.put(depthRange.toRange(), new DetectedInterval(depthRange));
      }
      return detectedIntervals;
   }

   private float findBottomDepthDepth(Ping ping, PowerData powerData) {
      Dep0Datagram dep0Datagram = ping.getDep0Datagram();
      if (dep0Datagram != null) {
         return dep0Datagram.getMinimumDepth();
      }
      double bot0Depth = ping.getBot0Datagram().getChannelDepths()[channel - 1];
      if (bot0Depth > 0) {
         return (float) bot0Depth;
      }
      return powerData.getMaxDepth();
   }

   private void endAllRegions(Ping ping) {
      for (DetectedRegion region : activeRegions) {
         endRegion(ping, region);
      }
      activeRegions.clear();
   }

   private void endRegion(Ping ping, DetectedRegion region) {
      region.end();
      Instant instant = ping.getInstant();
      if (regionInfoInstants.isEmpty() || !regionInfoInstants.getLast().equals(instant)) {
         regionInfoInstants.add(instant);
      }
      postprocess(region);
      ping.add(makeRegionInfoDatagram(instant, region));
   }

   private void postprocess(DetectedRegion region) {
      PingContainer pingContainer = EchogramUtils.listPingContainer(getPingConfiguration(), pingIndices);

      NavigableMap<PingIndex, FloatRangeSet> mask = region.mask;

      float fillVerticalGaps = module.fillVerticalGaps.getValue().orElse(0f);
      if (fillVerticalGaps > 0) {
         mask.replaceAll((_, depthRanges) -> {
            return depthRanges.fillGaps(fillVerticalGaps);
         });
      }

      int fillHorizontalGaps = module.fillHorizontalGaps.getValue().orElse(0);
      if (fillHorizontalGaps > 0) {
         // For each pair of depth masks {left, right} with max fillHorizontalGaps pings in between:
         // Add the intersection (left ∩ right) to each depth mask in between.
         List<PingIndex> pingIndices = new ArrayList<>(mask.keySet());
         for (int iLeft = 0; iLeft < pingIndices.size(); iLeft++) {
            FloatRangeSet left = mask.get(pingIndices.get(iLeft));
            int iRightEnd = Math.min(iLeft + fillHorizontalGaps + 2, pingIndices.size());
            for (int iRight = iLeft + 2; iRight < iRightEnd; iRight++) {
               FloatRangeSet right = mask.get(pingIndices.get(iRight));
               FloatRangeSet intersection = left.intersection(right);
               for (int k = iLeft + 1; k < iRight; k++) {
                  mask.computeIfPresent(pingIndices.get(k), (_, depthRangeSet) -> {
                     return depthRangeSet.add(intersection);
                  });
               }
            }
         }
      }

      int boundarySmoothingIterations = module.boundarySmoothingIterations.getValue().orElse(0);
      for (int i = 0; i < boundarySmoothingIterations; i++) {
         mask = MaskUtils.smoothBoundary(mask, pingContainer);
      }

      if (module.fillHoles.getBooleanValue()) {
         mask = MaskUtils.fillHoles(mask, pingContainer);
      }

      region.mask = mask;

      region.area = mask.entrySet().stream()
            .mapToDouble(e -> {
               double nmi = pingContainer.nextOrSame(e.getKey()).getVesselDistance() - e.getKey().getVesselDistance();
               double height = e.getValue().size();
               return KoronaUtils.nmiToMeter(nmi) * height;
            })
            .sum();

      region.perimeter = (float) MaskOutlineTracer.createBoundary(mask, pingContainer).stream()
            .mapToDouble(EchogramUtils::computeCircumference)
            .sum();

      region.length = KoronaUtils.nmiToMeter(pingContainer.nextOrSame(mask.lastKey()).getVesselDistance() - mask.firstKey().getVesselDistance());

      region.maxHeight = mask.values().stream()
            .flatMap(FloatRangeSet::stream)
            .mapToDouble(FloatRange::getSize)
            .max()
            .orElse(0);
   }

   private RegionInfoDatagram makeRegionInfoDatagram(Instant instant, DetectedRegion region) {
      List<RegionInfoDatagram.MaskInterval> maskIntervals = new ArrayList<>();
      region.mask.forEach((pingIndex, depthRanges) -> {
         for (FloatRange depthRange : depthRanges) {
            maskIntervals.add(new RegionInfoDatagram.MaskInterval(pingIndex.getInstant(), depthRange.min(), depthRange.max()));
         }
      });

      RegionInfoDatagram datagram = new RegionInfoDatagram(instant, isAccepted(region), channel, logSvThreshold,
            Utils.toInts(region.allIds), List.of(), maskIntervals);

      RegionInfoDatagram.BoundingBox boundingBox = datagram.getBoundingBox();
      boundingBox.x = region.firstRelativePingNumber;
      boundingBox.y = region.minDepth;
      boundingBox.width = region.pingCount;
      boundingBox.height = region.maxDepth - region.minDepth;

      RegionInfoDatagram.Values values = datagram.getValues();
      float meanLogSv = region.getMeanLogSv();
      values.area = (float) region.area;
      values.sampleCount = region.sumSampleCount;
      values.sumLogSv = meanLogSv * region.sumSampleCount;
      values.sumSv = (float) region.sumSv;
      values.logMeanSv = meanLogSv;
      values.sa = region.saTimesLength / region.length;
      values.perimeter = (float) region.perimeter;
      values.length = (float) region.length;
      values.maxHeight = (float) region.maxHeight;

      RegionInfoDatagram.Statistics statistics = datagram.getStatistics();
      // Not used so we don't calculate it.
      statistics.meanInside = Double.NaN;
      statistics.stdDevInside = Double.NaN;
      statistics.meanOutside = Double.NaN;
      statistics.stdDevOutside = Double.NaN;
      statistics.overlap = Double.NaN;
      statistics.crossing = Double.NaN;

      RegionInfoDatagram.Histogram histogram = datagram.getHistogram();
      // Not used so we don't calculate it.
      histogram.startValue = logSvThreshold;
      histogram.step = 0;
      histogram.counts = new int[1];

      return datagram;
   }

   private boolean isAccepted(DetectedRegion region) {
      return module.meanSv.getValue().containsIncludingEnd(region.getMeanLogSv())
            && module.maxSv.getValue().containsIncludingEnd(region.getMaxLogSv())
            && module.length.getValue().containsIncludingEnd((float) region.length)
            && module.thickness.getValue().containsIncludingEnd((float) region.maxHeight)
            && module.area.getValue().containsIncludingEnd((float) region.area)
            && module.compactness.getValue().containsIncludingEnd(region.getCircleCompactness());
   }

   private static final class DetectedInterval {
      private final FloatRange depthRange;
      private RegionContinuation regionContinuation;

      private DetectedInterval(FloatRange depthRange) {
         this.depthRange = depthRange;
         regionContinuation = new RegionContinuation(this);
      }

      @Override
      public String toString() {
         return depthRange + " -> " + regionContinuation;
      }

      private void mergeWith(DetectedInterval other) {
         regionContinuation.mergeWith(other.regionContinuation);
      }
   }

   private static final class RegionContinuation {
      private final Set<DetectedRegion> overlappingRegions = new TreeSet<>(DetectedRegion.COMPARATOR);
      private FloatRangeSet depthRanges;
      private final Set<DetectedInterval> detectedIntervals = new HashSet<>();

      private RegionContinuation(DetectedInterval detectedInterval) {
         depthRanges = FloatRangeSet.of(detectedInterval.depthRange);
         detectedIntervals.add(detectedInterval);
      }

      @Override
      public String toString() {
         return overlappingRegions + " " + depthRanges;
      }

      private void mergeWith(RegionContinuation other) {
         overlappingRegions.addAll(other.overlappingRegions);
         depthRanges = depthRanges.add(other.depthRanges);
         for (DetectedInterval otherDetectedInterval : other.detectedIntervals) {
            detectedIntervals.add(otherDetectedInterval);
            otherDetectedInterval.regionContinuation = this;
         }
      }
   }

   private static final class DetectedRegion {
      private static final Comparator<DetectedRegion> COMPARATOR = Comparator.comparingInt(region -> region.id);

      private final int id;
      private FloatRangeSet prevDepthRanges = FloatRangeSet.of();

      private final List<Integer> allIds = new ArrayList<>();
      private NavigableMap<PingIndex, FloatRangeSet> mask = new TreeMap<>();

      private int firstRelativePingNumber;
      private int pingCount;
      private float minDepth = Float.POSITIVE_INFINITY;
      private float maxDepth = Float.NEGATIVE_INFINITY;

      private double area;
      private int sumSampleCount;
      private double sumSv;
      private float maxSv;
      private double saTimesLength;
      private double perimeter;
      private double length;
      private double maxHeight;

      private DetectedRegion(int id, int relativePingNumber) {
         this.id = id;
         allIds.add(id);

         firstRelativePingNumber = relativePingNumber;
      }

      private void mergeWith(DetectedRegion other) {
         allIds.addAll(other.allIds);
         allIds.sort(null);

         MaskUtils.addAccumulate(mask, other.mask);

         firstRelativePingNumber = Math.min(firstRelativePingNumber, other.firstRelativePingNumber);
         pingCount = Math.max(pingCount, other.pingCount);
         minDepth = Math.min(minDepth, other.minDepth);
         maxDepth = Math.max(maxDepth, other.maxDepth);

         area += other.area;
         sumSampleCount += other.sumSampleCount;
         sumSv += other.sumSv;
         maxSv = Math.max(maxSv, other.maxSv);
         saTimesLength += other.saTimesLength;
         perimeter += other.perimeter;
         length = Math.max(length, other.length);
         maxHeight = Math.max(maxHeight, other.maxHeight);
      }

      @Override
      public String toString() {
         return Integer.toString(id);
      }

      private void update(PingIndex pingIndex, PowerData powerData, FloatRangeSet depthRanges, double pingDistanceMeter) {
         mask.merge(pingIndex, depthRanges, FloatRangeSet::add);

         pingCount++;
         List<FloatRange> ranges = depthRanges.getFloatRanges();
         minDepth = Math.min(minDepth, ranges.getFirst().min());
         maxDepth = Math.max(maxDepth, ranges.getLast().max());

         float[] svArray = powerData.getSv();
         float sampleDistance = powerData.getSampleDistance();
         double sumSvPing = 0;
         for (FloatRange range : ranges) {
            int beginIndex = powerData.depthToClampedSampleIndex(range.min());
            int endIndex = powerData.depthToClampedSampleIndex(range.max());
            int sampleCount = endIndex - beginIndex;
            area += sampleCount * sampleDistance * pingDistanceMeter;
            sumSampleCount += sampleCount;
            for (int i = beginIndex; i < endIndex; i++) {
               float sv = svArray[i];
               sumSvPing += sv;
               maxSv = Math.max(maxSv, sv);
            }
            maxHeight = Math.max(maxHeight, sampleCount * sampleDistance);
         }
         sumSv += sumSvPing;
         saTimesLength += sumSvPing * sampleDistance * pingDistanceMeter;
         length += pingDistanceMeter;

         updatePerimeter(depthRanges, pingDistanceMeter);

         prevDepthRanges = depthRanges;
      }

      private void end() {
         for (FloatRange range : prevDepthRanges) {
            perimeter += range.getSize();
         }
         prevDepthRanges = FloatRangeSet.of();
      }

      private void updatePerimeter(FloatRangeSet depthRanges, double pingDistanceMeter) {
         int iPrev = 0;
         int iNext = 0;
         List<FloatRange> prevRanges = prevDepthRanges.getFloatRanges();
         List<FloatRange> nextRanges = depthRanges.getFloatRanges();
         while (true) {
            if (iNext >= nextRanges.size()) {
               break;
            }
            FloatRange upperNextRange = nextRanges.get(iNext);
            while (iPrev < prevRanges.size() && prevRanges.get(iPrev).max() <= upperNextRange.min()) {
               // Prev range is above, so terminate perimeter.
               perimeter += prevRanges.get(iPrev).getSize();
               iPrev++;
            }
            if (iPrev >= prevRanges.size()) {
               break;
            }
            if (prevRanges.get(iPrev).min() >= upperNextRange.max()) {
               // Prev range is below, so terminate perimeter.
               perimeter += MathUtils.hypot(2 * pingDistanceMeter, nextRanges.get(iNext).getSize());
               iNext++;
               continue;
            }
            // Here prev and next overlaps. Find all ranges connected with this overlap.
            int iNextMax = iNext;
            int iPrevMax = iPrev;
            while (true) {
               if (iNextMax + 1 < nextRanges.size() && nextRanges.get(iNextMax + 1).intersects(prevRanges.get(iPrevMax))) {
                  iNextMax++;
                  continue;
               }
               if (iPrevMax + 1 < prevRanges.size() && prevRanges.get(iPrevMax + 1).intersects(nextRanges.get(iNextMax))) {
                  iPrevMax++;
                  continue;
               }
               break;
            }
            // Continue perimeter for outer edges of overlap.
            perimeter += MathUtils.hypot(pingDistanceMeter, prevRanges.get(iPrev).min() - nextRanges.get(iNext).min());
            perimeter += MathUtils.hypot(pingDistanceMeter, prevRanges.get(iPrevMax).max() - nextRanges.get(iNextMax).max());

            // Terminate perimeter for inner parts of prev.
            for (int i = iPrev; i < iPrevMax; i++) {
               perimeter += prevRanges.get(i + 1).min() - prevRanges.get(i).max();
            }

            // Start perimeter for inner parts of next.
            for (int i = iNext; i < iNextMax; i++) {
               perimeter += 2 * pingDistanceMeter + nextRanges.get(i + 1).min() - nextRanges.get(i).max();
            }

            iPrev = iPrevMax + 1;
            iNext = iNextMax + 1;
         }

         // Terminate perimeter for remaining prev ranges.
         while (iPrev < prevRanges.size()) {
            perimeter += prevRanges.get(iPrev).getSize();
            iPrev++;
         }

         // Start perimeter for remaining next ranges.
         while (iNext < nextRanges.size()) {
            perimeter += 2 * pingDistanceMeter + nextRanges.get(iNext).getSize();
            iNext++;
         }
      }

      private float getMeanLogSv() {
         return PowerData.svToLogSv(getMeanSv());
      }

      private float getMeanSv() {
         return sumSampleCount > 0 ? (float) (sumSv / sumSampleCount) : 0;
      }

      private float getMaxLogSv() {
         return PowerData.svToLogSv(maxSv);
      }

      private float getCircleCompactness() {
         return (float) GeometryUtils.getCircleCompactness(area, perimeter);
      }
   }
}
