package no.imr.korona.region.schooledit;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class ScaleMaskComputation {
   private final PingContainer pingContainer;
   private final EchogramPingSettings pingSettings;
   private final EchogramZSettings zSettings;
   private final NavigableMap<PingIndex, FloatRangeSet> originalMask;
   private final boolean isPartOfSmoothing;
   private final PingRange editPingRange;

   public ScaleMaskComputation(PingContainer pingContainer, EchogramPingSettings pingSettings, EchogramZSettings zSettings,
                               NavigableMap<PingIndex, FloatRangeSet> originalMask, boolean isPartOfSmoothing) {
      this.pingContainer = pingContainer;
      this.pingSettings = pingSettings;
      this.zSettings = zSettings;
      this.originalMask = originalMask;
      this.isPartOfSmoothing = isPartOfSmoothing;
      editPingRange = isPartOfSmoothing ? PingRange.from(originalMask, pingContainer) : pingContainer.getTotalRange();
   }

   public NavigableMap<PingIndex, FloatRangeSet> computeMask(float dz) {
      if (dz > 0) {
         return expand(dz);
      } else if (dz < 0) {
         return shrink(-dz);
      }
      return originalMask;
   }

   private NavigableMap<PingIndex, FloatRangeSet> expand(float dz) {
      Map<PingIndex, FloatRangeSet> result = new HashMap<>();
      float dy = zSettings.zToY(dz) - zSettings.zToY(0);
      originalMask.forEach((pingIndex, ranges) -> {
         result.put(pingIndex, ranges.expandEachRange(dz));
      });
      originalMask.forEach((pingIndex, ranges) -> {
         float x = pingSettings.pingIndexToX(pingIndex);
         iterate(result, pingIndex, x, ranges, dy, -1, true);
         iterate(result, pingIndex, x, ranges, dy, 1, true);
      });
      return new TreeMap<>(result);
   }

   private NavigableMap<PingIndex, FloatRangeSet> shrink(float dz) {
      if (originalMask.isEmpty()) {
         return originalMask;
      }
      PingIndex first = originalMask.firstKey();
      boolean firstIsAtEdge = first.getPingNumber() == pingContainer.getTotalRange().begin().getPingNumber();
      PingIndex begin = pingContainer.previousOrSame(first);

      PingIndex last = originalMask.lastKey();
      boolean lastIsAtEdge = last.getPingNumber() == pingContainer.getTotalRange().end().getPingNumber() - 1;
      PingIndex end = pingContainer.nextOrSame(pingContainer.nextOrSame(last));

      float dy = zSettings.zToY(dz) - zSettings.zToY(0);
      Map<PingIndex, FloatRangeSet> result = new HashMap<>();
      originalMask.forEach((pingIndex, ranges) -> {
         if (!isPartOfSmoothing && (firstIsAtEdge && pingIndex.equals(first) || lastIsAtEdge && pingIndex.equals(last))) {
            return;
         }
         FloatRangeSet shrunkRanges = ranges.expandEachRange(-dz);
         if (!shrunkRanges.isEmpty()) {
            result.put(pingIndex, shrunkRanges);
         }
      });
      PingRange iterationPingRange = PingRange.of(begin, end).intersection(editPingRange);
      pingContainer.getPingIndices(iterationPingRange).forEach(pingIndex -> {
         FloatRangeSet ranges;
         if (!isPartOfSmoothing && (firstIsAtEdge && pingIndex.equals(first) || lastIsAtEdge && pingIndex.equals(last))) {
            ranges = FloatRangeSet.of(FloatRange.ALL);
         } else {
            ranges = originalMask.getOrDefault(pingIndex, FloatRangeSet.of()).complement();
         }
         float x = pingSettings.pingIndexToX(pingIndex);
         iterate(result, pingIndex, x, ranges, dy, -1, false);
         iterate(result, pingIndex, x, ranges, dy, 1, false);
      });
      return new TreeMap<>(result);
   }

   private void iterate(Map<PingIndex, FloatRangeSet> result, PingIndex pingIndex, float x,
                        FloatRangeSet ranges, float dy, int direction, boolean add) {
      PingIndex other = pingIndex;
      while (true) {
         other = pingContainer.getPingIndexOrNullExcludingEnd(other.getPingNumber() + direction);
         if (other == null) {
            break;
         }
         if (!editPingRange.contains(other)) {
            break;
         }
         float dx = Math.abs(pingSettings.pingIndexToX(other) - x);
         if (dx > dy) {
            break;
         }
         float dyOther = (float) Math.sqrt(dy * dy - dx * dx);
         float dzOther = zSettings.yToZ(dyOther) - zSettings.yToZ(0);
         FloatRangeSet expandedRanges = ranges.expandEachRange(dzOther);
         if (add) {
            add(result, other, expandedRanges);
         } else {
            subtract(result, other, expandedRanges);
         }
      }
   }

   private static void add(Map<PingIndex, FloatRangeSet> result, PingIndex pingIndex, FloatRangeSet addend) {
      result.merge(pingIndex, addend, FloatRangeSet::add);
   }

   private static void subtract(Map<PingIndex, FloatRangeSet> result, PingIndex pingIndex, FloatRangeSet subtrahend) {
      result.computeIfPresent(pingIndex, (k, ranges) -> {
         return ranges.subtract(subtrahend).nullIfEmpty();
      });
   }
}
