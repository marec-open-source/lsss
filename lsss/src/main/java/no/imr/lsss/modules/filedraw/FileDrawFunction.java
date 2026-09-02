package no.imr.lsss.modules.filedraw;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.tools.math.Function1D;
import no.imr.tools.misc.ToFloatFunction;

import java.awt.geom.Point2D;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.DoubleUnaryOperator;

final class FileDrawFunction implements ToFloatFunction<PingIndex> {
   private final NavigableMap<PingIndex, Float> pingIndexToDepth = new TreeMap<>();

   FileDrawFunction(EchogramModule echogramModule, FileDrawLine line, float offset) {
      DataFileSet dataFileSet = echogramModule.getLSSS().getInterpretationSettings().getDataFileSet();

      EchogramPoint beginEchogramPoint = null;
      Point2D beginImagePoint = null;

      BiFunction<Float, Float, Float> mergeFunction = offset >= 0 ? Math::max : Math::min;
      for (FileDrawPoint point : line.points()) {
         double timeValue = PingMapping.instantToTimeValue(point.time());
         PingIndex endPingIndex = dataFileSet.getContainingPingIndex(timeValue, PingMapping.TIME);
         if (endPingIndex == null) {
            beginEchogramPoint = null;
            beginImagePoint = null;
            continue;
         }
         float endDepth = dataFileSet.getDataConfiguration().physicalDepthToDepth(point.depth());
         pingIndexToDepth.merge(endPingIndex, endDepth + offset, mergeFunction);
         EchogramPoint endEchogramPoint = new EchogramPoint(endPingIndex, endDepth);
         Point2D endImagePoint = echogramModule.echogramPointToImagePoint(endEchogramPoint);
         if (beginImagePoint != null) {
            DoubleUnaryOperator linearFunction = Function1D.linear(beginImagePoint, endImagePoint);
            dataFileSet.getPingIndices(PingRange.ofUnsorted(beginEchogramPoint.pingIndex(), endEchogramPoint.pingIndex())).forEach(pingIndex -> {
               double x = echogramModule.getPingSettings().pingIndexToX(pingIndex);
               double y = linearFunction.applyAsDouble(x);
               float depth = echogramModule.getZSettings().yToDepth(y, pingIndex);
               pingIndexToDepth.merge(pingIndex, depth + offset, mergeFunction);
            });
         }
         beginEchogramPoint = endEchogramPoint;
         beginImagePoint = endImagePoint;
      }
   }

   @Override
   public float applyAsFloat(PingIndex pingIndex) {
      Map.Entry<PingIndex, Float> floorEntry = pingIndexToDepth.floorEntry(pingIndex);
      if (floorEntry != null) {
         return floorEntry.getValue();
      }
      Map.Entry<PingIndex, Float> firstEntry = pingIndexToDepth.firstEntry();
      if (firstEntry != null) {
         return firstEntry.getValue();
      }
      return Float.NaN;
   }
}
