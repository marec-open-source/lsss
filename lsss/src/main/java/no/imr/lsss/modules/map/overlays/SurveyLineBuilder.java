package no.imr.lsss.modules.map.overlays;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.lsss.modules.map.MapModule;
import no.imr.tools.geo.GeoTransform;
import no.imr.tools.time.NTDate;
import no.marec.lsss.api.util.GeoPoint;
import no.marec.lsss.api.util.LineStripBuilder;
import org.jspecify.annotations.Nullable;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Builds a data structure for a survey line in pixel coordinates.
 */
public final class SurveyLineBuilder {
   private static final int DISCONTINUITY_THRESHOLD = 30 * NTDate.UNITS_PER_SECOND;

   private final List<List<SurveyLinePoint>> lineStrips;

   public SurveyLineBuilder(MapModule mapModule, DataFileSet dataFileSet) {
      this(mapModule, dataFileSet.getPingIndices().stream());
   }

   public SurveyLineBuilder(MapModule mapModule, Stream<PingIndex> pingIndices) {
      this(mapModule.getGeoTransform(), mapModule.getBounds(), pingIndices);
   }

   public SurveyLineBuilder(GeoTransform geoTransform, Rectangle bounds, Stream<PingIndex> pingIndices) {
      lineStrips = toLineStrips(geoTransform, bounds, pingIndices);
   }

   private static List<List<SurveyLinePoint>> toLineStrips(GeoTransform geoTransform, Rectangle bounds, Stream<PingIndex> pingIndices) {
      List<List<SurveyLinePoint>> lineStrips = new ArrayList<>();
      pingIndices.forEachOrdered(new Consumer<>() {
         private @Nullable List<SurveyLinePoint> currentLineStrip;
         private @Nullable PingIndex previousEndPingIndex = null;
         private @Nullable PingIndex beginPingIndex = null;
         private final Point beginPixPos = new Point();
         private final Point endPixPos = new Point();
         private int beginOutCode = 0;

         @Override
         public void accept(PingIndex endPingIndex) {
            GeoPoint geoPos = endPingIndex.getGeographicalPosition();
            if (geoPos == null) {
               // No geographical position => Reset.
               currentLineStrip = null;
               previousEndPingIndex = null;
               beginPingIndex = null;
               beginOutCode = 0;
               return;
            }

            geoTransform.geoToPix(geoPos, endPixPos);
            int endOutCode = bounds.outcode(endPixPos);

            if (previousEndPingIndex == null || beginPingIndex == null // First time for this part of survey line after reset.
                  ||
                  (beginOutCode & endOutCode) != 0 // Line is completely outside visible rectangle.
                  ||
                  endPingIndex.getNTDate() - previousEndPingIndex.getNTDate() > DISCONTINUITY_THRESHOLD // Too long jump in time.
            ) {
               // Move (no line) to next position.
               currentLineStrip = null;
               previousEndPingIndex = endPingIndex;
               beginPingIndex = endPingIndex;
               beginPixPos.setLocation(endPixPos);
               beginOutCode = endOutCode;
               return;
            }

            if (beginPixPos.x == endPixPos.x && beginPixPos.y == endPixPos.y) {
               // Same pixel.
               previousEndPingIndex = endPingIndex;
               return;
            }

            if (currentLineStrip == null) {
               currentLineStrip = new ArrayList<>();
               lineStrips.add(currentLineStrip);
               currentLineStrip.add(new SurveyLinePoint(beginPixPos, beginPingIndex));
            }
            currentLineStrip.add(new SurveyLinePoint(endPixPos, endPingIndex));

            previousEndPingIndex = endPingIndex;
            beginPingIndex = endPingIndex;
            beginPixPos.setLocation(endPixPos);
            beginOutCode = endOutCode;
         }
      });
      return lineStrips.stream()
            .map(List::copyOf)
            .toList();
   }

   public boolean isEmpty() {
      return lineStrips.isEmpty();
   }

   public int getPointCount() {
      return lineStrips.stream()
            .mapToInt(List::size)
            .sum();
   }

   public @Nullable PingIndex getClosestPingIndex(Point2D point) {
      return getClosestPingIndex(point.getX(), point.getY());
   }

   public @Nullable PingIndex getClosestPingIndex(double x, double y) {
      double minDist = Double.POSITIVE_INFINITY;
      SurveyLinePoint minA = null;
      SurveyLinePoint minB = null;

      for (List<SurveyLinePoint> lineStrip : lineStrips) {
         for (int i = 1; i < lineStrip.size(); i++) {
            SurveyLinePoint a = lineStrip.get(i - 1);
            SurveyLinePoint b = lineStrip.get(i);
            double dist = Line2D.ptSegDistSq(a.x, a.y, b.x, b.y, x, y);
            if (dist < minDist) {
               minDist = dist;
               minA = a;
               minB = b;
            }
         }
      }

      if (minA == null) {
         return null;
      }
      double distA = Point2D.distanceSq(minA.x, minA.y, x, y);
      double distB = Point2D.distanceSq(minB.x, minB.y, x, y);
      return distA < distB ? minA.pingIndex : minB.pingIndex;
   }

   public void buildPath(LineStripBuilder lineStripBuilder) {
      for (List<SurveyLinePoint> lineStrip : lineStrips) {
         for (SurveyLinePoint point : lineStrip) {
            lineStripBuilder.addPoint(point.x, point.y);
         }
         lineStripBuilder.endLineStrip();
      }
   }

   public void buildCompositePath(Function<PingIndex, @Nullable LineStripBuilder> lineStripSelector) {
      for (List<SurveyLinePoint> lineStrip : lineStrips) {
         LineStripBuilder currentLineStripBuilder = null;
         for (SurveyLinePoint point : lineStrip) {
            LineStripBuilder lineStripBuilder = lineStripSelector.apply(point.pingIndex);

            if (lineStripBuilder != currentLineStripBuilder && currentLineStripBuilder != null) {
               // End current path.
               currentLineStripBuilder.addPoint(point.x, point.y);
               currentLineStripBuilder.endLineStrip();
            }

            if (lineStripBuilder != null) {
               lineStripBuilder.addPoint(point.x, point.y);
            }

            currentLineStripBuilder = lineStripBuilder;
         }
         if (currentLineStripBuilder != null) {
            currentLineStripBuilder.endLineStrip();
         }
      }
   }

   private record SurveyLinePoint(int x, int y, PingIndex pingIndex) {
      private SurveyLinePoint(Point pixPos, PingIndex pingIndex) {
         this(pixPos.x, pixPos.y, pingIndex);
      }
   }
}
