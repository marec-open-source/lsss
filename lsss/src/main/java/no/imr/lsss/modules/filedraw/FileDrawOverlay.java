package no.imr.lsss.modules.filedraw;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.misc.ToFloatFunction;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.SimpleInputDialog;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.linestrip.LineStripBuilders;
import no.marec.lsss.api.util.LineStripBuilder;
import org.jspecify.annotations.Nullable;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class FileDrawOverlay extends BaseEchogramOverlay {
   private final IntParameter lineThickness = new IntParameter(
         new Name("LineThickness", "Line thickness"),
         2, Unit.COUNT,
         "Thickness of line in pixels");

   private Stroke stroke = GuiUtils.STROKE_1;
   private final Supplier<FileDrawDataModule> fileDrawDataModule = moduleSupplier(FileDrawDataModule.class);
   private @Nullable FileDrawLine activeLine;

   public FileDrawOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            lineThickness
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(lineThickness, newCoalescingExecListener(this::updateStroke));

      registry.add(createRecomputeListener(), List.of(
            fileDrawDataModule.get().fileDrawData(),
            getEchogramModule().echogramArea()
      ));

      //---

      updateStroke();
      recompute();
   }

   @Override
   protected void onDisable() {
      activeLine = null;
   }

   private void updateStroke() {
      stroke = new BasicStroke(lineThickness.getIntValue(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
      repaint();
   }

   @Override
   public @Nullable JPopupMenu getPopupMenu(Point point) {
      if (activeLine == null) {
         return null;
      }

      JPopupMenu menu = new JPopupMenu();

      JMenuItem addHorizontalLayerBoundaryItem = MiscIcons.ADD.on(menu.add("Add horizontal layer boundary..."));
      PingIndex pingIndex = getPingSettings().xToContainingPingIndex(point.getX());
      if (pingIndex == null || getRegionManager().isReadOnly(pingIndex)) {
         addHorizontalLayerBoundaryItem.setEnabled(false);
      } else {
         addHorizontalLayerBoundaryItem.addActionListener(e -> {
            new SimpleInputDialog<>("Add horizontal layer boundary", "Depth offset to file draw line", "", Float::parseFloat)
                  .setUnit(Unit.METER)
                  .setBelowText("A positive offset means deeper.")
                  .show(getEchogramModule().getComponent())
                  .ifPresent(inputOffset -> {
                     float offset = getLSSS().getDataManager().getDataConfiguration().isSeabedMounted() ? -inputOffset : inputOffset;
                     ToFloatFunction<PingIndex> pingIndexToDepth = new FileDrawFunction(getEchogramModule(), activeLine, offset);
                     getRegionManager().addHorizontalLayerBoundary(pingIndex, pingIndexToDepth);
                  });
         });
      }

      return menu;
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      List<FileDrawLine> lines = fileDrawDataModule.get().fileDrawData().getValue().lines();
      if (lines.isEmpty()) {
         return null;
      }
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      Rectangle bounds = getEchogramModule().getBounds();
      List<Marker> markers = lines.stream()
            .map(line -> {
               Path2D.Float path = new Path2D.Float();
               LineStripBuilder pathBuilder = LineStripBuilders.coalescing(path, bounds);
               for (FileDrawPoint point : line.points()) {
                  double timeValue = PingMapping.millisToTimeValue(point.time());
                  PingIndex pingIndex = dataFileSet.getContainingPingIndex(timeValue, PingMapping.TIME);
                  if (pingIndex == null) {
                     pathBuilder.endLineStrip();
                     continue;
                  }
                  float x = getPingSettings().valueToX(timeValue, PingMapping.TIME);
                  float depth = dataFileSet.getDataConfiguration().physicalDepthToDepth(point.depth());
                  float y = getZSettings().depthToY(depth, pingIndex);
                  pathBuilder.addPoint(x, y);
               }
               pathBuilder.endLineStrip();
               if (pathBuilder.isEmpty()) {
                  return null;
               }
               path.trimToSize();
               return new Marker(line, path);
            })
            .filter(Objects::nonNull)
            .toList();
      if (markers.isEmpty()) {
         return null;
      }
      return new DisplayData(markers);
   }

   private void setActiveLine(@Nullable FileDrawLine activeLine) {
      if (this.activeLine != activeLine) {
         this.activeLine = activeLine;
         repaint();
      }
   }

   private record Marker(FileDrawLine line, Path2D.Float path) {
   }

   private final class DisplayData extends TransformedDisplayData {
      private final List<Marker> markers;

      private DisplayData(List<Marker> markers) {
         this.markers = markers;
      }

      @Override
      protected void transformedDraw(Graphics2D g2d) {
         g2d.setStroke(stroke);
         g2d.setColor(Color.BLACK);
         Path2D.Float activePath = null;
         for (Marker marker : markers) {
            if (marker.line == activeLine) {
               activePath = marker.path;
               continue;
            }
            g2d.draw(marker.path);
         }
         if (activePath != null) {
            g2d.setColor(Color.RED);
            g2d.draw(activePath);
         }
      }

      @Override
      public boolean intersects(Rectangle2D rectangle) {
         FileDrawLine line = getIntersectingLine(rectangle);
         setActiveLine(line);
         return line != null;
      }

      private @Nullable FileDrawLine getIntersectingLine(Rectangle2D rectangle) {
         for (Marker marker : markers) {
            if (GuiUtils.intersects(marker.path, rectangle)) {
               return marker.line;
            }
         }
         return null;
      }
   }
}
