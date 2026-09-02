package no.imr.lsss.modules.reflog;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.ExtendedSurveyLine;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.map.MapModule;
import no.imr.lsss.modules.map.overlays.BaseMapOverlay;
import no.imr.tools.geo.GeoTransform;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.Supplier;

public final class RefLogMapOverlay extends BaseMapOverlay {
   private static final float MARKER_WIDTH = 10;

   private final BooleanParameter textOn = new BooleanParameter(
         new Name("TextOn", "Text on"),
         true,
         "Check this if station name should be shown");

   private final Supplier<RefLogDataModule> refLogDataModule = moduleSupplier(RefLogDataModule.class);

   public RefLogMapOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, MapModule mapModule) {
      super(moduleInfo, mapModule);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            textOn
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            getMapModule().getGeographicalAreaChangeManager(),
            getInterpretationSettings().getMapSettings().getExtendedSurveyLine().getChangeManager(),
            refLogDataModule.get().getChangeManager()
      ));

      registry.add(this::repaint, List.of(
            textOn,
            refLogDataModule.get().getActiveLogLineChangeManager()
      ));
   }

   @Override
   public void onDeactivate() {
      refLogDataModule.get().setActiveLogLine(null);
   }

   @Override
   public void mouseClicked(MouseEvent mouseEvent) {
      refLogDataModule.get().clickActiveLogLine();
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      Rectangle2D geoRect = getMapModule().getGeoRect();
      GeoTransform geoTransform = getMapModule().getGeoTransform();
      Point2D.Float pixPos = new Point2D.Float();

      ExtendedSurveyLine extendedSurveyLine = getInterpretationSettings().getMapSettings().getExtendedSurveyLine();
      PingRange pingRange = extendedSurveyLine.getTotalPingRange();
      List<Marker> markers = refLogDataModule.get().getAllDisplayableLogLines(pingRange)
            .<Marker>mapMulti((logLine, consumer) -> {
               if (logLine.activityType() == ActivityType.CTD) {
                  // Do not plot CTD, this information comes from CTD stations.
                  return;
               }
               PingIndex pingIndex = extendedSurveyLine.getClosestPingIndex(PingMapping.instantToTimeValue(logLine.time()), PingMapping.TIME);
               GeoPoint geoPosition = pingIndex.getGeographicalPosition();
               if (geoPosition == null) {
                  return;
               }
               if (!geoRect.contains(geoPosition)) {
                  return;
               }
               geoTransform.geoToPix(geoPosition, pixPos);
               float x = pixPos.x;
               float y = pixPos.y;

               Shape shape = switch (logLine.activityType()) {
                  case BOTTOM_TRAWL -> square(x, y);
                  case PELAGIC_TRAWL -> triangle(x, y);
                  default -> cross(x, y);
               };
               TextPlacement textPlacement = null;
               if (logLine.start()) {
                  Point2D tangent = extendedSurveyLine.getTangent(pingIndex);
                  if (tangent != null) {
                     textPlacement = Math.abs(tangent.getX()) > Math.abs(tangent.getY()) ? TextPlacement.BELOW : TextPlacement.RIGHT;
                  }
               }
               consumer.accept(new Marker(logLine, shape, x, y, textPlacement));
            })
            .toList();
      if (markers.isEmpty()) {
         return null;
      }
      return transformed(new DisplayData(markers));
   }

   private static Shape square(float x, float y) {
      return new Rectangle2D.Float(x - MARKER_WIDTH / 2, y - MARKER_WIDTH / 2, MARKER_WIDTH, MARKER_WIDTH);
   }

   private static Shape triangle(float x, float y) {
      Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO, 4);
      path.moveTo(x, y - MARKER_WIDTH * 0.6667f);
      path.lineTo(x - MARKER_WIDTH / 2, y + MARKER_WIDTH * 0.3333f);
      path.lineTo(x + MARKER_WIDTH / 2, y + MARKER_WIDTH * 0.3333f);
      path.closePath();
      return path;
   }

   private static Shape cross(float x, float y) {
      Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO, 4);
      path.moveTo(x - MARKER_WIDTH / 2, y - MARKER_WIDTH / 2);
      path.lineTo(x + MARKER_WIDTH / 2, y + MARKER_WIDTH / 2);
      path.moveTo(x + MARKER_WIDTH / 2, y - MARKER_WIDTH / 2);
      path.lineTo(x - MARKER_WIDTH / 2, y + MARKER_WIDTH / 2);
      return path;
   }

   @Override
   public @Nullable String getToolTipText(Point point) {
      return RefLogDataModule.getToolTip(refLogDataModule.get().getActiveLogLine());
   }

   @Override
   public JPopupMenu getPopupMenu(Point point) {
      JPopupMenu popupMenu = getDefaultPopupMenu(point);
      popupMenu.addSeparator();

      JMenuItem visualizerItem = MiscIcons.SCATTER_PLOT.on(popupMenu.add("Visualizer dialog..."));
      visualizerItem.addActionListener(_ -> new RefLogVisualizerDialog(refLogDataModule.get()));

      return popupMenu;
   }

   private enum TextPlacement {
      RIGHT, BELOW
   }

   private record Marker(LogLine logLine, Shape shape, float x, float y, @Nullable TextPlacement textPlacement) {
   }

   private final class DisplayData implements OverlayDisplayData {
      private final List<Marker> markers;

      private DisplayData(List<Marker> markers) {
         this.markers = markers;
      }

      @Override
      public void draw(Graphics2D g2d) {
         g2d.setStroke(GuiUtils.STROKE_2);
         g2d.setColor(Color.DARK_GRAY);

         LogLine activeLogLine = refLogDataModule.get().getActiveLogLine();
         Marker activeMarker = null;
         for (Marker marker : markers) {
            if (marker.logLine == activeLogLine) {
               activeMarker = marker;
               continue;
            }
            g2d.draw(marker.shape);
         }

         if (activeMarker != null) {
            g2d.setColor(Color.RED);
            g2d.draw(activeMarker.shape);
         }
      }

      @Override
      public void drawText(Graphics2D g2d) {
         if (!textOn.getBooleanValue()) {
            return;
         }
         LogLine activeLogLine = refLogDataModule.get().getActiveLogLine();
         Marker activeMarker = null;
         for (Marker marker : markers) {
            if (marker.logLine == activeLogLine) {
               activeMarker = marker;
               continue;
            }
            drawText(g2d, marker, Color.BLUE);
         }
         if (activeMarker != null) {
            drawText(g2d, activeMarker, Color.RED);
         }
      }

      private static void drawText(Graphics2D g2d, Marker marker, Color color) {
         if (marker.textPlacement != null) {
            switch (marker.textPlacement) {
               case RIGHT -> {
                  GuiText.draw(g2d, marker.logLine.localStationNumber(), color, marker.x + MARKER_WIDTH, marker.y, GuiText.HorizontalAlignment.LEFT, GuiText.VerticalAlignment.CENTER, null);
               }
               case BELOW -> {
                  GuiText.draw(g2d, marker.logLine.localStationNumber(), color, marker.x, marker.y + MARKER_WIDTH, GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.TOP, null);
               }
            }
         }
      }

      @Override
      public boolean intersects(Rectangle2D rectangle) {
         LogLine logLine = getIntersectingLogLine(rectangle);
         refLogDataModule.get().setActiveLogLine(logLine);
         return logLine != null;
      }

      private @Nullable LogLine getIntersectingLogLine(Rectangle2D rectangle) {
         double centerX = rectangle.getCenterX();
         double centerY = rectangle.getCenterY();
         double maxDx = rectangle.getWidth() / 2 + MARKER_WIDTH / 2;
         double maxDy = rectangle.getHeight() / 2 + MARKER_WIDTH / 2;
         double minDist = Math.sqrt(maxDx * maxDx + maxDy * maxDy);
         LogLine logLine = null;
         for (Marker marker : markers) {
            double dx = Math.abs(marker.x - centerX);
            if (dx > minDist) {
               continue;
            }
            double dy = Math.abs(marker.y - centerY);
            if (dy > minDist) {
               continue;
            }
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < minDist && marker.shape.intersects(rectangle)) {
               minDist = dist;
               logLine = marker.logLine;
            }
         }
         return logLine;
      }
   }
}
