package no.imr.lsss.modules.reflog;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.Supplier;

public final class RefLogEchogramOverlay extends BaseEchogramOverlay {
   private final IntParameter markerHeight = new IntParameter(
         new Name("MarkerHeight", "Marker height"),
         30, Unit.COUNT,
         "Height of marker in pixels");

   private final IntParameter lineThickness = new IntParameter(
         new Name("LineThickness", "Line thickness"),
         3, Unit.COUNT,
         "Thickness of marker in pixels");

   private final BooleanParameter textOn = new BooleanParameter(
         new Name("TextOn", "Text on"),
         true,
         "Check this if station name should be shown");

   private BasicStroke stroke = GuiUtils.STROKE_1;
   private final Supplier<RefLogDataModule> refLogDataModule = moduleSupplier(RefLogDataModule.class);

   public RefLogEchogramOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            markerHeight,
            lineThickness,
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
            getEchogramModule().echogramArea(),
            refLogDataModule.get().getChangeManager()
      ));

      Listener repaintListener = this::repaint;
      registry.add(refLogDataModule.get().getActiveLogLineChangeManager(), repaintListener);

      registry.add(getParameters(), repaintListener);

      registry.add(lineThickness, newCoalescingExecListener(this::updateStroke));

      //---

      updateStroke();
      recompute();
   }

   private void updateStroke() {
      stroke = new BasicStroke(lineThickness.getIntValue(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{1, 0}, 0);
      repaint();
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
      PingRange pingRange = getInterpretationSettings().getPingRange();
      List<Marker> markers = refLogDataModule.get().getAllDisplayableLogLines(pingRange)
            .map(logLine -> {
               PingIndex pingIndex = getInterpretationSettings().getDataFileSet().getClosestPingIndex(PingMapping.millisToTimeValue(logLine.timeInMillis()), PingMapping.TIME);
               return new Marker(logLine, getPingSettings().pingIndexToXIndex(pingIndex));
            })
            .toList();
      if (markers.isEmpty()) {
         return null;
      }
      return new DisplayData(markers);
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
      visualizerItem.addActionListener(e -> new RefLogVisualizerDialog(refLogDataModule.get()));

      return popupMenu;
   }

   private record Marker(LogLine logLine, int x) {
   }

   private final class DisplayData extends OverlayDisplayData {
      private final List<Marker> markers;

      private DisplayData(List<Marker> markers) {
         this.markers = markers;
      }

      @Override
      public void draw(Graphics2D g2d) {
         g2d.setStroke(stroke);
         g2d.setColor(Color.BLUE);

         LogLine activeLogLine = refLogDataModule.get().getActiveLogLine();
         Marker activeMarker = null;
         for (Marker marker : markers) {
            if (marker.logLine == activeLogLine) {
               activeMarker = marker;
               continue;
            }
            int x = marker.x;
            g2d.drawLine(x, 0, x, markerHeight.getIntValue());
         }

         if (activeMarker != null) {
            g2d.setColor(Color.RED);
            int x = activeMarker.x;
            g2d.drawLine(x, 0, x, markerHeight.getIntValue());
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

      private void drawText(Graphics2D g2d, Marker marker, Color color) {
         LogLine logLine = marker.logLine;
         if (logLine.start()) {
            int x = marker.x;
            int y = markerHeight.getIntValue();
            GuiText.draw(g2d, logLine.activityType().shortName, color, x, y, GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.BOTTOM, null);
            GuiText.draw(g2d, logLine.localStationNumber(), color, x, y, GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.TOP, null);
         }
      }

      @Override
      public boolean intersects(Rectangle2D rectangle) {
         LogLine logLine = getIntersectingLogLine(rectangle);
         refLogDataModule.get().setActiveLogLine(logLine);
         return logLine != null;
      }

      private @Nullable LogLine getIntersectingLogLine(Rectangle2D rectangle) {
         if (rectangle.getMinY() > markerHeight.getIntValue()) {
            return null;
         }
         double centerX = rectangle.getCenterX();
         double minDx = rectangle.getWidth() / 2;
         LogLine logLine = null;
         for (Marker marker : markers) {
            double dx = Math.abs(marker.x - centerX);
            if (dx < minDx && rectangle.contains(marker.x, rectangle.getY())) {
               minDx = dx;
               logLine = marker.logLine;
            }
         }
         return logLine;
      }
   }
}
