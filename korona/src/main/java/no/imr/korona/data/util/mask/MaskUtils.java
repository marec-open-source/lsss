package no.imr.korona.data.util.mask;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.EchogramUtils;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.schooledit.ScaleMaskComputation;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.math.MathUtils;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;

public final class MaskUtils {
   private MaskUtils() {
   }

   public static boolean contains(NavigableMap<PingIndex, FloatRangeSet> mask, PingIndex pingIndex, float depth) {
      FloatRangeSet depthRanges = mask.get(pingIndex);
      return depthRanges != null && depthRanges.contains(depth);
   }

   public static boolean contains(NavigableMap<PingIndex, FloatRangeSet> mask, EchogramPoint echogramPoint) {
      return contains(mask, echogramPoint.pingIndex(), echogramPoint.depth());
   }

   public static boolean isContainedIn(NavigableMap<PingIndex, FloatRangeSet> mask, Function<PingIndex, FloatRange> container) {
      for (Map.Entry<PingIndex, FloatRangeSet> entry : mask.entrySet()) {
         FloatRangeSet maskDepthRanges = entry.getValue();
         FloatRange containerDepthRange = container.apply(entry.getKey());
         if (!containerDepthRange.contains(maskDepthRanges.getBoundingRange())) {
            return false;
         }
      }
      return true;
   }

   public static NavigableMap<PingIndex, FloatRangeSet> add(NavigableMap<PingIndex, FloatRangeSet> mask1,
                                                            NavigableMap<PingIndex, FloatRangeSet> mask2) {
      NavigableMap<PingIndex, FloatRangeSet> result = new TreeMap<>(mask1);
      addAccumulate(result, mask2);
      return result;
   }

   public static NavigableMap<PingIndex, FloatRangeSet> add(Collection<NavigableMap<PingIndex, FloatRangeSet>> masks) {
      NavigableMap<PingIndex, FloatRangeSet> result = new TreeMap<>();
      for (NavigableMap<PingIndex, FloatRangeSet> mask : masks) {
         addAccumulate(result, mask);
      }
      return result;
   }

   public static void addAccumulate(NavigableMap<PingIndex, FloatRangeSet> result,
                                    NavigableMap<PingIndex, FloatRangeSet> mask) {
      mask.forEach((key, rangeSet) -> {
         result.merge(key, rangeSet, FloatRangeSet::add);
      });
   }

   public static NavigableMap<PingIndex, FloatRangeSet> subtract(NavigableMap<PingIndex, FloatRangeSet> minuend,
                                                                 NavigableMap<PingIndex, FloatRangeSet> subtrahend) {
      NavigableMap<PingIndex, FloatRangeSet> result = new TreeMap<>();
      minuend.forEach((key, rangeSet) -> {
         FloatRangeSet subtrahendRangeSet = subtrahend.get(key);
         if (subtrahendRangeSet != null) {
            FloatRangeSet difference = rangeSet.subtract(subtrahendRangeSet);
            if (!difference.isEmpty()) {
               result.put(key, difference);
            }
         } else {
            result.put(key, rangeSet);
         }
      });
      return result;
   }

   public static NavigableMap<PingIndex, FloatRangeSet> intersection(NavigableMap<PingIndex, FloatRangeSet> mask1,
                                                                     NavigableMap<PingIndex, FloatRangeSet> mask2) {
      NavigableMap<PingIndex, FloatRangeSet> result = new TreeMap<>();
      mask1.forEach((key, rangeSet1) -> {
         FloatRangeSet rangeSet2 = mask2.get(key);
         if (rangeSet2 != null) {
            FloatRangeSet intersection = rangeSet1.intersection(rangeSet2);
            if (!intersection.isEmpty()) {
               result.put(key, intersection);
            }
         }
      });
      return result;
   }

   public static NavigableMap<PingIndex, FloatRangeSet> intersection(NavigableMap<PingIndex, FloatRangeSet> mask1,
                                                                     Function<PingIndex, FloatRange> mask2) {
      NavigableMap<PingIndex, FloatRangeSet> result = new TreeMap<>();
      mask1.forEach((key, rangeSet1) -> {
         FloatRange rangeSet2 = mask2.apply(key);
         FloatRangeSet intersection = rangeSet1.intersection(rangeSet2);
         if (!intersection.isEmpty()) {
            result.put(key, intersection);
         }
      });
      return result;
   }

   public static boolean intersects(NavigableMap<PingIndex, FloatRangeSet> mask1,
                                    NavigableMap<PingIndex, FloatRangeSet> mask2) {
      for (Map.Entry<PingIndex, FloatRangeSet> entry1 : mask1.entrySet()) {
         PingIndex key = entry1.getKey();
         FloatRangeSet rangeSet1 = entry1.getValue();
         FloatRangeSet rangeSet2 = mask2.get(key);
         if (rangeSet2 != null && rangeSet1.intersects(rangeSet2)) {
            return true;
         }
      }
      return false;
   }

   public static NavigableMap<PingIndex, FloatRangeSet> xor(NavigableMap<PingIndex, FloatRangeSet> mask1,
                                                            NavigableMap<PingIndex, FloatRangeSet> mask2) {
      NavigableMap<PingIndex, FloatRangeSet> result = new TreeMap<>(mask1);
      xorAccumulate(result, mask2);
      return result;
   }

   public static void xorAccumulate(NavigableMap<PingIndex, FloatRangeSet> result,
                                    NavigableMap<PingIndex, FloatRangeSet> mask) {
      mask.forEach((key, maskRangeSet) -> {
         FloatRangeSet resultRangeSet = result.get(key);
         if (resultRangeSet == null) {
            result.put(key, maskRangeSet);
         } else {
            FloatRangeSet xor = resultRangeSet.xor(maskRangeSet);
            if (xor.isEmpty()) {
               result.remove(key);
            } else {
               result.put(key, xor);
            }
         }
      });
   }

   public static NavigableMap<PingIndex, FloatRangeSet> complement(NavigableMap<PingIndex, FloatRangeSet> mask, PingContainer pingContainer) {
      PingRange pingRange = PingRange.from(mask, pingContainer);
      NavigableMap<PingIndex, FloatRangeSet> result = new TreeMap<>();
      pingContainer.getPingIndices(pingRange).forEach(pingIndex -> {
         result.put(pingIndex, mask.getOrDefault(pingIndex, FloatRangeSet.of()).complement());
      });
      return result;
   }

   public static List<NavigableMap<PingIndex, FloatRangeSet>> toDisjointMasks(NavigableMap<PingIndex, FloatRangeSet> mask, PingContainer pingContainer) {
      if (mask.isEmpty()) {
         return List.of();
      }
      List<NavigableMap<PingIndex, FloatRangeSet>> result = new ArrayList<>();
      NavigableMap<PingIndex, FloatRangeSet> remainingMask = mask;
      while (!remainingMask.isEmpty()) {
         Map.Entry<PingIndex, FloatRangeSet> firstEntry = remainingMask.firstEntry();
         NavigableMap<PingIndex, FloatRangeSet> grownMask = new GrowEngine(pingContainer, depthRangeExtractor(remainingMask))
               .addSeed(firstEntry.getKey(), firstEntry.getValue().getFloatRanges().getFirst())
               .growSchoolMask(new AsyncHandle())
               .orElseThrow();
         result.add(grownMask);
         remainingMask = subtract(remainingMask, grownMask);
      }
      return result;
   }

   public static NavigableMap<PingIndex, FloatRangeSet> findHoleMask(NavigableMap<PingIndex, FloatRangeSet> mask, PingContainer pingContainer) {
      if (mask.isEmpty()) {
         return Collections.emptyNavigableMap();
      }
      NavigableMap<PingIndex, FloatRangeSet> complementMask = complement(mask, pingContainer);
      NavigableMap<PingIndex, FloatRangeSet> outsideMask = new GrowEngine(pingContainer, depthRangeExtractor(complementMask))
            .addSeed(complementMask.firstKey(), complementMask.firstEntry().getValue().getFloatRanges())
            .addSeed(complementMask.lastKey(), complementMask.lastEntry().getValue().getFloatRanges())
            .growSchoolMask(new AsyncHandle())
            .orElse(Collections.emptyNavigableMap());
      return subtract(complementMask, outsideMask);
   }

   private static Function<PingIndex, FloatRangeSet> depthRangeExtractor(NavigableMap<PingIndex, FloatRangeSet> mask) {
      return pingIndex -> mask.getOrDefault(pingIndex, FloatRangeSet.of());
   }

   public static Function<PingIndex, FloatRangeSet> schoolCandidateDepthRangeExtractor(RegionManager regionManager, DataFileSet dataFileSet, int channel) {
      return pingIndex -> {
         Ping ping = dataFileSet.getPing(pingIndex);
         PowerData powerData = ping.getPowerData(channel);
         if (powerData == null) {
            return FloatRangeSet.of();
         }
         FloatRange boundaryDepthRange = regionManager.getLayerManager().getBoundaryDepthRange(pingIndex);
         FloatRange logSvRange = regionManager.getThresholdManager().getLogSvRange(pingIndex);
         FloatRangeSet depthRanges = FloatRangeSet.of(DataUtils.findDepthRanges(powerData, boundaryDepthRange, powerData.getLogSv(), logSvRange));
         FloatRangeSet schoolDepthRanges = regionManager.getSchoolManager().depthRangesForPingIndex(pingIndex);
         return depthRanges.subtract(schoolDepthRanges);
      };
   }

   public static NavigableMap<PingIndex, FloatRangeSet> fillHoles(NavigableMap<PingIndex, FloatRangeSet> mask, PingContainer pingContainer) {
      NavigableMap<PingIndex, FloatRangeSet> holes = findHoleMask(mask, pingContainer);
      return add(mask, holes);
   }

   public static NavigableMap<PingIndex, FloatRangeSet> smooth(NavigableMap<PingIndex, FloatRangeSet> mask, float dz,
                                                               EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      NavigableMap<PingIndex, FloatRangeSet> result = mask;
      if (dz != 0) {
         NavigableMap<PingIndex, FloatRangeSet> convexHull = convexHull(result, pingSettings, zSettings.getDepthTransform());
         result = new ScaleMaskComputation(pingSettings, zSettings, result, true)
               .computeMask(dz);
         result = new ScaleMaskComputation(pingSettings, zSettings, result, true)
               .computeMask(-dz);
         result = subtract(result, complement(convexHull, pingSettings.getPingContainer()));
      }
      return result;
   }

   public static NavigableMap<PingIndex, FloatRangeSet> convexHull(NavigableMap<PingIndex, FloatRangeSet> mask,
                                                                   EchogramPingSettings pingSettings, DepthTransform depthTransform) {
      record HullPoint(PingIndex pingIndex, float x, float z) {
         private static void add(List<HullPoint> points, HullPoint c, boolean upper) {
            while (points.size() > 1) {
               HullPoint b = points.getLast();
               HullPoint a = points.get(points.size() - 2);
               float cross = (b.x - a.x) * (c.z - b.z) - (c.x - b.x) * (b.z - a.z);
               if (upper && cross > 0 || !upper && cross < 0) {
                  break;
               }
               points.removeLast();
            }
            points.add(c);
         }

         private static List<EchogramPoint> toBoundary(List<HullPoint> points, PingContainer pingContainer, DepthTransform depthTransform) {
            return EchogramUtils.addMissingPoints(points.stream()
                  .map(p -> new EchogramPoint(p.pingIndex, depthTransform.zToDepth(p.z, p.pingIndex)))
                  .toList(), pingContainer, depthTransform);
         }
      }

      List<HullPoint> upperHull = new ArrayList<>();
      List<HullPoint> lowerHull = new ArrayList<>();

      for (Map.Entry<PingIndex, FloatRangeSet> entry : mask.entrySet()) {
         PingIndex pingIndex = entry.getKey();
         float x = pingSettings.pingIndexToX(pingIndex);
         FloatRange boundingRange = entry.getValue().getBoundingRange();

         HullPoint cUpper = new HullPoint(pingIndex, x, depthTransform.depthToZ(boundingRange.min(), pingIndex));
         HullPoint.add(upperHull, cUpper, true);

         HullPoint cLower = new HullPoint(pingIndex, x, depthTransform.depthToZ(boundingRange.max(), pingIndex));
         HullPoint.add(lowerHull, cLower, false);
      }

      List<EchogramPoint> upperBoundary = HullPoint.toBoundary(upperHull, pingSettings.getPingContainer(), depthTransform);
      List<EchogramPoint> lowerBoundary = HullPoint.toBoundary(lowerHull, pingSettings.getPingContainer(), depthTransform);
      NavigableMap<PingIndex, FloatRangeSet> result = new TreeMap<>();
      for (int i = 0; i < upperBoundary.size(); i++) {
         EchogramPoint upper = upperBoundary.get(i);
         EchogramPoint lower = lowerBoundary.get(i);
         result.put(upper.pingIndex(), FloatRangeSet.of(FloatRange.of(upper.depth(), lower.depth())));
      }
      return result;
   }

   public static NavigableMap<PingIndex, FloatRangeSet> smoothBoundary(NavigableMap<PingIndex, FloatRangeSet> mask, PingContainer pingContainer) {
      NavigableMap<PingIndex, FloatRangeSet> result = new TreeMap<>(mask);
      for (List<EchogramPoint> points : MaskOutlineTracer.createBoundary(mask, pingContainer)) {
         int n = points.size();
         for (int i = 0; i < n; i++) {
            EchogramPoint p0 = points.get(i);
            FloatRangeSet depthRangeSet = result.get(p0.pingIndex());
            if (depthRangeSet == null) {
               continue;
            }
            EchogramPoint pm1 = points.get(MathUtils.mod(i - 1, n));
            EchogramPoint p1 = points.get(MathUtils.mod(i + 1, n));
            if (pm1.pingIndex().getPingNumber() < p0.pingIndex().getPingNumber()) {
               // pm1 to p0 is left to right.
               if (p0.pingIndex().getPingNumber() >= p1.pingIndex().getPingNumber()) {
                  // p0 to p1 is NOT left to right.
                  continue;
               }
               EchogramPoint p2 = points.get(MathUtils.mod(i + 2, n));
               if (p1.pingIndex().getPingNumber() >= p2.pingIndex().getPingNumber()) {
                  // p1 to p2 is NOT left to right, so
                  // p1 is extrapolated and should not be used for smoothing.
                  continue;
               }
            } else if (pm1.pingIndex().getPingNumber() > p0.pingIndex().getPingNumber()) {
               // pm1 to p0 is right to left.
               if (p0.pingIndex().getPingNumber() <= p1.pingIndex().getPingNumber()) {
                  // p0 to p1 is NOT right to left.
                  continue;
               }
               EchogramPoint pm2 = points.get(MathUtils.mod(i - 2, n));
               if (pm2.pingIndex().getPingNumber() <= pm1.pingIndex().getPingNumber()) {
                  // pm2 to pm1 is NOT right to left, so
                  // pm1 is extrapolated and should not be used for smoothing.
                  continue;
               }
            } else {
               // pm1 -> p0 is vertical.
               continue;
            }
            float depth = (pm1.depth() + 2 * p0.depth() + p1.depth()) / 4;
            if (!depthRangeSet.contains(depth)) {
               result.put(p0.pingIndex(), depthRangeSet.add(FloatRange.ofUnsorted(p0.depth(), depth)));
            }
         }
      }
      return result;
   }

   public static NavigableMap<PingIndex, FloatRangeSet> incompleteBoundaryToMask(List<EchogramPoint> incompleteBoundary, PingContainer pingContainer, DepthTransform depthTransform) {
      List<EchogramPoint> filledBoundary = new ArrayList<>();
      for (int i = 0; i < incompleteBoundary.size(); i++) {
         EchogramPoint point = incompleteBoundary.get(i);
         filledBoundary.add(point);
         EchogramPoint nextPoint = incompleteBoundary.get((i + 1) % incompleteBoundary.size());
         if (Math.abs(point.pingIndex().getPingNumber() - nextPoint.pingIndex().getPingNumber()) > 1) {
            List<EchogramPoint> line = EchogramUtils.computeLine(depthTransform, point, nextPoint, pingContainer);
            filledBoundary.addAll(line.subList(1, line.size() - 1));
         }
      }
      return MaskOutlineTracer.createMaskForSingleBoundary(filledBoundary);
   }

   public static NavigableMap<PingIndex, FloatRangeSet> boxMask(PingRange pingRange, FloatRange zRange, DepthTransform depthTransform, PingContainer pingContainer) {
      NavigableMap<PingIndex, FloatRangeSet> result = new TreeMap<>();
      for (PingIndex pingIndex : pingContainer.getPingIndices(pingRange)) {
         FloatRange depthRange = depthTransform.zToDepth(zRange, pingIndex);
         if (depthRange.isEmpty()) {
            continue;
         }
         result.put(pingIndex, FloatRangeSet.of(depthRange));
      }
      return result;
   }
}
