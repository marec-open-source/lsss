package no.imr.korona.data.util.mask;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.tools.math.MathUtils;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public final class MaskOutlineTracer {
   private static final Comparator<Trace> TRACE_COMPARATOR = (o1, o2) -> Float.compare(o1.getStartPointIfUpper().depth(), o2.getStartPointIfUpper().depth());

   private MaskOutlineTracer() {
   }

   private static final class Trace {
      private final List<EchogramPoint> trace;
      private final TraceType startPointType;
      private final TraceType endPointType;

      private enum MergePoint {
         START, END
      }

      private enum TraceType {
         UPPER, LOWER
      }

      private Trace(EchogramPoint echogramPoint, TraceType traceType) {
         trace = new ArrayList<>();
         trace.add(echogramPoint);
         startPointType = traceType;
         endPointType = traceType;
      }

      private Trace(List<EchogramPoint> trace, TraceType startPointType, TraceType endPointType) {
         this.trace = trace;
         this.startPointType = startPointType;
         this.endPointType = endPointType;
      }

      private EchogramPoint getStartTracePoint() {
         return trace.getFirst();
      }

      private EchogramPoint getEndTracePoint() {
         return trace.getLast();
      }

      private EchogramPoint getStartPointIfUpper() {
         return startPointType == Trace.TraceType.UPPER ? getStartTracePoint() : getEndTracePoint();
      }

      private TraceType getStartPointType() {
         return startPointType;
      }

      private TraceType getEndPointType() {
         return endPointType;
      }

      private void append(EchogramPoint point) {
         trace.add(point);
      }

      private void appendStart(EchogramPoint point) {
         trace.addFirst(point);
      }
   }

   private record TraceAndSignificantDepth(float depth, Trace trace) {
   }

   public static Set<List<EchogramPoint>> createBoundary(NavigableMap<PingIndex, FloatRangeSet> mask, PingContainer pingContainer) {
      if (mask.isEmpty()) {
         return new HashSet<>();
      }
      List<Trace> traces = createTraces(mask, pingContainer);
      return mergeTraces(traces);
   }

   private static List<Trace> createTraces(NavigableMap<PingIndex, FloatRangeSet> mask, PingContainer pingContainer) {
      List<Trace> traces = new ArrayList<>();
      List<Trace> completedTraces = new ArrayList<>();
      PingIndex previousPingIndex = null;
      for (Map.Entry<PingIndex, FloatRangeSet> entry : mask.entrySet()) {
         PingIndex pingIndex = entry.getKey();
         if (previousPingIndex != null && pingIndex.getPingNumber() > previousPingIndex.getPingNumber() + 1) {
            // Close all traces when the mask has a gap in ping index.
            PingIndex pastPreviousPingIndex = pingContainer.nextOrSame(previousPingIndex);
            for (Trace trace : traces) {
               if (trace.getStartPointType() == Trace.TraceType.UPPER) {
                  trace.appendStart(new EchogramPoint(pastPreviousPingIndex, trace.getStartTracePoint().depth()));
               } else {
                  trace.append(new EchogramPoint(pastPreviousPingIndex, trace.getEndTracePoint().depth()));
               }
            }
            completedTraces.addAll(traces);
            traces.clear();
         }
         previousPingIndex = pingIndex;

         FloatRangeSet maskedDepthRangeSet = entry.getValue();
         List<Trace> traceList = new ArrayList<>(traces.size() + 2 * maskedDepthRangeSet.getFloatRanges().size());
         traceList.addAll(traces);
         for (FloatRange depthRange : maskedDepthRangeSet.getFloatRanges()) {
            traceList.add(new Trace(new EchogramPoint(pingIndex, depthRange.min()), Trace.TraceType.UPPER));
            traceList.add(new Trace(new EchogramPoint(pingIndex, depthRange.max()), Trace.TraceType.LOWER));
         }
         traceList.sort(TRACE_COMPARATOR);
         List<Trace> tracesCopy = new ArrayList<>(traces);
         tracesCopy.sort(TRACE_COMPARATOR);
         List<Trace> usedTraces = new ArrayList<>(2 * tracesCopy.size());
         float minDepth = Float.NEGATIVE_INFINITY;
         for (Trace trace : tracesCopy) {
            Trace traceToAdd = findTraceExtensionCandidate(trace, traceList, usedTraces, minDepth);
            if (traceToAdd != null) {
               if (trace.getStartPointType() == Trace.TraceType.UPPER) {
                  trace.appendStart(traceToAdd.getStartTracePoint());
               } else {
                  trace.append(traceToAdd.getStartTracePoint());
               }
               minDepth = Math.max(minDepth, traceToAdd.getStartTracePoint().depth());
               usedTraces.add(traceToAdd);
               usedTraces.add(trace);
            } else {
               //Unequal trace types - create new trace or finish existing trace
               finishOrCreateTrace(traces, completedTraces, pingIndex, trace);
               usedTraces.add(trace);
            }
         }
         traceList.removeAll(usedTraces);
         for (Trace trace : traceList) {
            finishOrCreateTrace(traces, completedTraces, pingIndex, trace);
         }
      }
      //Uncompleted traces must be added an end point
      PingIndex pastLastPingIndex = pingContainer.nextOrSame(mask.lastKey());
      for (Trace trace : traces) {
         if (trace.getStartPointType() == Trace.TraceType.UPPER) {
            trace.appendStart(new EchogramPoint(pastLastPingIndex, trace.getStartTracePoint().depth()));
         } else {
            trace.append(new EchogramPoint(pastLastPingIndex, trace.getEndTracePoint().depth()));
         }
      }
      List<Trace> result = new ArrayList<>();
      result.addAll(completedTraces);
      result.addAll(traces);
      return result;
   }

   private static @Nullable Trace findTraceExtensionCandidate(Trace trace, List<Trace> depthSortedTraces, List<Trace> usedTraces, float minDepth) {
      int index = depthSortedTraces.indexOf(trace);
      //Rule : can merge with trace of same type, unless we need to cross another trace
      //Search forward
      for (int i = index + 1; i < depthSortedTraces.size(); i++) {
         Trace traceAddCandidate = depthSortedTraces.get(i);
         if (crossesOtherTraces(trace, traceAddCandidate, depthSortedTraces, minDepth)) {
            break;
         }
         if (!usedTraces.contains(traceAddCandidate) && traceAddCandidate.getStartPointType() == trace.getStartPointType()) {
            return traceAddCandidate;
         }
      }
      //Search backward
      for (int i = index - 1; i >= 0; i--) {
         Trace traceAddCandidate = depthSortedTraces.get(i);
         if (crossesOtherTraces(trace, traceAddCandidate, depthSortedTraces, minDepth)) {
            break;
         }
         if (!usedTraces.contains(traceAddCandidate) && traceAddCandidate.getStartPointType() == trace.getStartPointType()) {
            return traceAddCandidate;
         }
      }
      return null;
   }

   private static boolean crossesOtherTraces(Trace trace, Trace traceToAdd, List<Trace> depthSortedTraces, float minDepth) {
      int index1 = depthSortedTraces.indexOf(trace);
      float d1 = trace.getStartPointIfUpper().depth();
      int index2 = depthSortedTraces.indexOf(traceToAdd);
      float d2 = traceToAdd.getStartPointIfUpper().depth();
      if (d2 < minDepth) {
         return true;
      }

      int lowIndex = Math.min(index1, index2);
      int highIndex = Math.max(index1, index2);
      float lowDepth = Math.min(d1, d2);
      float highDepth = Math.max(d1, d2);
      for (int i = lowIndex; i < highIndex; i++) {
         Trace depthSortedTrace = depthSortedTraces.get(i);
         float d = depthSortedTrace.getStartPointIfUpper().depth();
         if (d > lowDepth && d < highDepth) {
            return true;
         }
      }
      return false;
   }

   private static void finishOrCreateTrace(List<Trace> traces, List<Trace> completedTraces, PingIndex pingIndex, Trace t1) {
      if (traces.contains(t1)) {
         if (t1.getStartPointType() == Trace.TraceType.UPPER) {
            t1.appendStart(new EchogramPoint(pingIndex, t1.getStartTracePoint().depth()));
         } else {
            t1.append(new EchogramPoint(pingIndex, t1.getEndTracePoint().depth()));
         }
         completedTraces.add(t1);
         traces.remove(t1);
      } else { // add new trace
         traces.add(t1);
      }
   }

   private static Set<List<EchogramPoint>> mergeTraces(List<Trace> traces) {
      Set<List<EchogramPoint>> result = new HashSet<>();
      if (traces.isEmpty()) {
         return result;
      }
      Trace currentTrace = traces.removeFirst();
      while (true) {
         Trace mergeTrace = findMergeTrace(currentTrace, Trace.MergePoint.START, currentTrace.getStartPointType(), traces);
         while (mergeTrace != null) {
            Trace.MergePoint mergeTracePoint = mergeTrace.getStartTracePoint().pingIndex().equals(currentTrace.getStartTracePoint().pingIndex()) ?
                  Trace.MergePoint.START : Trace.MergePoint.END;
            currentTrace = mergeTraces(currentTrace, Trace.MergePoint.START, mergeTrace, mergeTracePoint);
            traces.remove(mergeTrace);
            mergeTrace = findMergeTrace(currentTrace, Trace.MergePoint.START, currentTrace.getStartPointType(), traces);
         }
         result.add(new ArrayList<>(currentTrace.trace));
         if (traces.isEmpty()) {
            break;
         }
         currentTrace = traces.removeFirst();
      }
      return result;
   }

   private static @Nullable Trace findMergeTrace(Trace currentTrace, Trace.MergePoint mergePoint, Trace.TraceType traceType, List<Trace> traces) {
      EchogramPoint mergeEchogramPoint = mergePoint == Trace.MergePoint.START ? currentTrace.getStartTracePoint() : currentTrace.getEndTracePoint();
      PingIndex mergePingIndex = mergeEchogramPoint.pingIndex();
      List<TraceAndSignificantDepth> candidates = new ArrayList<>();
      for (Trace trace : traces) {
         if (trace.getStartTracePoint().pingIndex().equals(mergePingIndex)) {
            candidates.add(new TraceAndSignificantDepth(trace.getStartTracePoint().depth(), trace));
         } else if (trace.getEndTracePoint().pingIndex().equals(mergePingIndex)) {
            candidates.add(new TraceAndSignificantDepth(trace.getEndTracePoint().depth(), trace));
         }
      }
      TraceAndSignificantDepth inputTracePoint = new TraceAndSignificantDepth(currentTrace.getStartTracePoint().depth(), currentTrace);
      candidates.add(inputTracePoint);
      //Add trace an extra time if the endpoint is at the same ping index as the start point.
      if (currentTrace.getEndTracePoint().pingIndex().equals(mergePingIndex)) {
         candidates.add(new TraceAndSignificantDepth(currentTrace.getEndTracePoint().depth(), currentTrace));
      }
      candidates.sort((o1, o2) -> Float.compare(o1.depth(), o2.depth()));
      int traceIndex = candidates.indexOf(inputTracePoint);
      int candidateIndex = traceIndex % 2 == 0 ? traceIndex + 1 : traceIndex - 1;
      if (candidateIndex >= 0 && candidateIndex < candidates.size()) {
         TraceAndSignificantDepth candidateAndDepth = candidates.get(candidateIndex);
         Trace candidate = candidateAndDepth.trace();
         if (!candidate.equals(currentTrace)) {
            Trace.TraceType type = candidate.getStartTracePoint().pingIndex().equals(mergePingIndex)
                  ? candidate.getStartPointType()
                  : candidate.getEndPointType();
            if (type != traceType) {
               return candidate;
            }
         }
      }
      return null;
   }

   private static Trace mergeTraces(Trace trace1, Trace.MergePoint trace1MergePoint,
                                    Trace trace2, Trace.MergePoint trace2MergePoint) {
      //The type of the merge points should be opposite
      Trace.TraceType type1 = trace1MergePoint == Trace.MergePoint.START ? trace1.getStartPointType() : trace1.getEndPointType();
      Trace.TraceType type2 = trace2MergePoint == Trace.MergePoint.START ? trace2.getStartPointType() : trace2.getEndPointType();
      assert type1 != type2;

      List<EchogramPoint> mergedTrace = new ArrayList<>();
      if ((type1 == Trace.TraceType.UPPER && trace1MergePoint == Trace.MergePoint.START) ||
            (type1 == Trace.TraceType.LOWER && trace1MergePoint == Trace.MergePoint.END) ||
            (type2 == Trace.TraceType.UPPER && trace2MergePoint == Trace.MergePoint.END) ||
            (type2 == Trace.TraceType.LOWER && trace2MergePoint == Trace.MergePoint.START)) {
         mergedTrace.addAll(trace2.trace);
         mergedTrace.addAll(trace1.trace);
         return new Trace(mergedTrace, trace2.getStartPointType(), trace1.getEndPointType());
      } else {
         mergedTrace.addAll(trace1.trace);
         mergedTrace.addAll(trace2.trace);
         return new Trace(mergedTrace, trace1.getStartPointType(), trace2.getEndPointType());
      }
   }

   private enum PointType {
      NORMAL, LEFT_TURNING_VERTEX, RIGHT_TURNING_VERTEX, LEFT_VERTICAL_EDGE_VERTEX, RIGHT_VERTICAL_EDGE_VERTEX, VERTICAL_LINE_VERTEX
   }

   public static NavigableMap<PingIndex, FloatRangeSet> createMaskForSingleBoundary(List<EchogramPoint> boundary) {
      ListMultimap<PingIndex, EchogramPoint> pingIndexToPointMap = makePingIndexToPointMap(boundary);
      return updatePingIndexToDepthRangeMap(pingIndexToPointMap);
   }

   public static ListMultimap<PingIndex, EchogramPoint> makePingIndexToPointMap(List<EchogramPoint> boundary) {
      ListMultimap<PingIndex, EchogramPoint> pingIndexToPointMap = ArrayListMultimap.create();
      int n = boundary.size();
      for (int i = 0; i < n; i++) {
         EchogramPoint thisPoint = boundary.get(i);
         EchogramPoint pointBefore = boundary.get(MathUtils.mod(i - 1, n));
         EchogramPoint pointAfter = boundary.get(MathUtils.mod(i + 1, n));

         PointType pointType = getPointType(thisPoint, pointBefore, pointAfter);
         switch (pointType) {
            case NORMAL, LEFT_VERTICAL_EDGE_VERTEX -> {
               pingIndexToPointMap.put(thisPoint.pingIndex(), thisPoint);
            }
            case LEFT_TURNING_VERTEX -> { //add twice
               pingIndexToPointMap.put(thisPoint.pingIndex(), thisPoint);
               pingIndexToPointMap.put(thisPoint.pingIndex(), thisPoint);
            }
            default -> {
               // Do nothing.
            }
         }
      }
      return pingIndexToPointMap;
   }

   private static PointType getPointType(EchogramPoint thisPoint, EchogramPoint pointBefore, EchogramPoint pointAfter) {
      int beforeComparedToThis = pointBefore.pingIndex().compareTo(thisPoint.pingIndex());
      int thisComparedToAfter = thisPoint.pingIndex().compareTo(pointAfter.pingIndex());
      if (beforeComparedToThis != 0 && beforeComparedToThis == thisComparedToAfter) {
         return PointType.NORMAL;
      }
      if ((beforeComparedToThis == 0 && thisComparedToAfter < 0) ||
            (beforeComparedToThis > 0 && thisComparedToAfter == 0)) {
         return PointType.LEFT_VERTICAL_EDGE_VERTEX;
      }
      if ((beforeComparedToThis == 0 && thisComparedToAfter > 0) ||
            (beforeComparedToThis < 0 && thisComparedToAfter == 0)) {
         return PointType.RIGHT_VERTICAL_EDGE_VERTEX;
      }
      if (beforeComparedToThis > 0 && thisComparedToAfter < 0) {
         return PointType.LEFT_TURNING_VERTEX;
      }
      if (beforeComparedToThis < 0 && thisComparedToAfter > 0) {
         return PointType.RIGHT_TURNING_VERTEX;
      }
      return PointType.VERTICAL_LINE_VERTEX;
   }

   private static NavigableMap<PingIndex, FloatRangeSet> updatePingIndexToDepthRangeMap(ListMultimap<PingIndex, EchogramPoint> pingIndexToPointMap) {
      NavigableMap<PingIndex, FloatRangeSet> pingIndexToDepthRangeMap = new TreeMap<>();
      for (PingIndex pingIndex : pingIndexToPointMap.keySet()) {
         FloatRangeSet value = getDepthRanges(pingIndexToPointMap.get(pingIndex));
         if (!value.isEmpty()) {
            pingIndexToDepthRangeMap.put(pingIndex, value);
         }
      }
      return pingIndexToDepthRangeMap;
   }

   public static FloatRangeSet getDepthRanges(List<EchogramPoint> points) {
      if (points.size() % 2 != 0) {
         return FloatRangeSet.of();
      }

      float[] depths = new float[points.size()];
      for (int i = 0; i < points.size(); i++) {
         depths[i] = points.get(i).depth();
      }
      Arrays.sort(depths);

      List<FloatRange> depthRanges = new ArrayList<>(depths.length / 2);
      for (int i = 0; i < depths.length; i += 2) {
         depthRanges.add(FloatRange.of(depths[i], depths[i + 1]));
      }
      return FloatRangeSet.of(depthRanges);
   }
}
