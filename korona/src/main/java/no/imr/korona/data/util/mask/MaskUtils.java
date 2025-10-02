package no.imr.korona.data.util.mask;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.EchogramUtils;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.tools.CyclicList;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

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

   private static DepthRangeExtractor depthRangeExtractor(NavigableMap<PingIndex, FloatRangeSet> mask) {
      return pingIndex -> mask.getOrDefault(pingIndex, FloatRangeSet.of()).getFloatRanges();
   }

   public static NavigableMap<PingIndex, FloatRangeSet> fillHoles(NavigableMap<PingIndex, FloatRangeSet> mask, PingContainer pingContainer) {
      NavigableMap<PingIndex, FloatRangeSet> holes = findHoleMask(mask, pingContainer);
      return add(mask, holes);
   }

   public static NavigableMap<PingIndex, FloatRangeSet> incompleteBoundaryToMask(List<EchogramPoint> incompleteBoundary, PingContainer pingContainer, DepthTransform depthTransform) {
      CyclicList<EchogramPoint> filledBoundary = new CyclicList<>();
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
