package no.imr.korona.data;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.primitives.Ints;
import no.imr.korona.data.datagrams.Cas0Datagram;
import no.imr.korona.data.datagrams.RegionInfoDatagram;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.mask.MaskOutlineTracer;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Utilities for regions detected by KORONA.
 */
public final class KoronaRegion {
   private KoronaRegion() {
   }

   public static @Nullable Cas0Datagram findCas0Datagram(Ping ping, RegionInfoDatagram regionInfoDatagram) {
      return ping.getPingItems(Cas0Datagram.class)
            .filter(cas0Datagram -> Ints.contains(regionInfoDatagram.getBorderIds(), cas0Datagram.getRegionId()))
            .findFirst()
            .orElse(null);
   }

   public static NavigableMap<PingIndex, FloatRangeSet> createMask(RegionInfoDatagram regionInfoDatagram, PingContainer pingContainer) {
      List<RegionInfoDatagram.MaskInterval> maskIntervals = regionInfoDatagram.getMaskIntervals();
      if (maskIntervals != null) {
         return createMaskFromIntervals(maskIntervals, pingContainer);
      }
      List<RegionInfoDatagram.PerimeterPoint> perimeterPoints = regionInfoDatagram.getPerimeterPoints();
      if (perimeterPoints != null) {
         return createMaskFromPerimeter(perimeterPoints, pingContainer);
      }
      return Collections.emptyNavigableMap();
   }

   private static NavigableMap<PingIndex, FloatRangeSet> createMaskFromIntervals(List<RegionInfoDatagram.MaskInterval> maskIntervals, PingContainer pingContainer) {
      NavigableMap<PingIndex, FloatRangeSet> mask = new TreeMap<>();
      for (RegionInfoDatagram.MaskInterval maskInterval : maskIntervals) {
         PingIndex pingIndex = pingContainer.getClosestPingIndex(PingMapping.instantToTimeValue(maskInterval.instant()), PingMapping.TIME);
         FloatRange depthRange = FloatRange.of(maskInterval.minDepth(), maskInterval.maxDepth());
         mask.merge(pingIndex, FloatRangeSet.of(depthRange), FloatRangeSet::add);
      }
      return mask;
   }

   private static NavigableMap<PingIndex, FloatRangeSet> createMaskFromPerimeter(List<RegionInfoDatagram.PerimeterPoint> perimeterPoints, PingContainer pingContainer) {
      NavigableMap<PingIndex, FloatRangeSet> mask = new TreeMap<>();
      List<EchogramPoint> trace = new ArrayList<>();
      for (RegionInfoDatagram.PerimeterPoint perimeterPoint : perimeterPoints) {
         PingIndex pingIndex = pingContainer.getClosestPingIndex(PingMapping.instantToTimeValue(perimeterPoint.instant()), PingMapping.TIME);
         trace.add(new EchogramPoint(pingIndex, perimeterPoint.depth()));
      }
      if (trace.isEmpty()) {
         return mask;
      }
      // Remove the repeated point.
      trace.removeFirst();
      ListMultimap<PingIndex, EchogramPoint> pingIndexToPointsMap = ArrayListMultimap.create();
      for (EchogramPoint echogramPoint : trace) {
         pingIndexToPointsMap.put(echogramPoint.pingIndex(), echogramPoint);
      }
      for (PingIndex pingIndex : pingIndexToPointsMap.keySet()) {
         mask.put(pingIndex, MaskOutlineTracer.getDepthRanges(pingIndexToPointsMap.get(pingIndex)));
      }
      return mask;
   }
}
