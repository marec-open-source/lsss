package no.imr.lsss.incubator.modules.graphicalinfo;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.tools.swing.linestrip.LineStripBuilders;
import no.marec.lsss.api.util.LineStripBuilder;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.util.List;

final class EchogramPolygon implements EchogramGraphicalInfo {
   private final List<EchogramPoint> echogramPoints;
   private final Color color;
   private final boolean filled;
   private final List<String> texts;

   EchogramPolygon(List<EchogramPoint> echogramPoints, Color color, boolean filled, List<String> texts) {
      this.echogramPoints = echogramPoints;
      this.color = color;
      this.filled = filled;
      this.texts = texts;
   }

   @Override
   public RenderedInfo render(EchogramPingSettings pingSettings, EchogramZSettings zSettings, Rectangle bounds) {
      Path2D path = new Path2D.Float();

      LineStripBuilder pathBuilder = LineStripBuilders.piecewiseHorizontal(path, bounds);
      for (EchogramPoint echogramPoint : echogramPoints) {
         addPoint(pathBuilder, echogramPoint, pingSettings, zSettings);
      }
      //close the polygon
      EchogramPoint firstPoint = echogramPoints.getFirst();
      addPoint(pathBuilder, firstPoint, pingSettings, zSettings);
      pathBuilder.endLineStrip();
      return new ColoredPath(path, color, filled, texts);
   }

   private static void addPoint(LineStripBuilder pathBuilder, EchogramPoint echogramPoint, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      PingIndex pingIndex = echogramPoint.pingIndex();
      float x = pingSettings.pingIndexToX(pingIndex);
      float y = zSettings.depthToY(echogramPoint.depth(), pingIndex);
      pathBuilder.addPoint(x, y);
   }
}
