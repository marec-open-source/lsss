package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.lsss.modules.BaseModuleOverlay;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.RangeSet;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Draws markers on echogram indicating the beginning of files.
 */
public final class FileMarkerEngine {
   private final BaseModuleOverlay overlay;
   private final EchogramPingSettings pingSettings;
   private final DataManager dataManager;

   private int markerHeight;
   private int lineThickness;

   private @Nullable DataFile activeDataFile;

   public FileMarkerEngine(BaseModuleOverlay overlay, EchogramPingSettings pingSettings, DataManager dataManager) {
      this.overlay = overlay;
      this.pingSettings = pingSettings;
      this.dataManager = dataManager;
   }

   public @Nullable OverlayDisplayData update(int markerHeight, int lineThickness) {
      this.markerHeight = markerHeight;
      this.lineThickness = lineThickness;
      PingRange pingRange = pingSettings.getPingRange();
      pingRange = PingRange.of(pingRange.begin(), dataManager.getDataFileSet().nextOrSame(pingRange.end()));
      List<DataFile> dataFiles = dataManager.getDataFileSet().getDataFiles(pingRange);
      if (!dataFiles.isEmpty() && !pingRange.contains(dataFiles.getFirst().getPingRange().begin())) {
         dataFiles = dataFiles.subList(1, dataFiles.size());
      }
      if (dataFiles.isEmpty()) {
         return null;
      }
      return new DisplayData(dataFiles);
   }

   private @Nullable DataFile getIntersectingDataFile(Rectangle2D rectangle) {
      if (rectangle.getMaxY() < overlay.getHeight() - markerHeight) {
         return null;
      }
      double centerX = rectangle.getCenterX();
      PingIndex pingIndex = pingSettings.xToContainingPingIndex(centerX);
      if (pingIndex == null) {
         return null;
      }
      DataFileSet dataFileSet = dataManager.getDataFileSet();
      int i = dataFileSet.getContainingDataFileIndex(pingIndex);
      List<DataFile> dataFiles = dataFileSet.getDataFiles();
      DataFile dataFile = dataFiles.get(i);
      double dx = Math.abs(pingSettings.pingIndexToX(dataFile.getPingRange().begin()) - centerX);
      if (i + 1 < dataFiles.size()) {
         DataFile nextDataFile = dataFiles.get(i + 1);
         double nextDx = Math.abs(pingSettings.pingIndexToX(nextDataFile.getPingRange().begin()) - centerX);
         if (nextDx < dx) {
            dataFile = nextDataFile;
            dx = nextDx;
         }
      }
      if (dx < (rectangle.getWidth() + lineThickness) / 2) {
         return dataFile;
      }
      return null;
   }

   private void setActiveDataFile(@Nullable DataFile dataFile) {
      if (activeDataFile != dataFile) {
         activeDataFile = dataFile;
         overlay.repaint();
      }
   }

   public @Nullable String getToolTipText() {
      if (activeDataFile != null) {
         StringBuilder toolTipText = new StringBuilder("<html>")
               .append(activeDataFile.getSegmentHandle().getDisplayName());
         String info = activeDataFile.getSegmentData().getInfo();
         if (info != null) {
            toolTipText.append("<br>").append(info);
         }
         return toolTipText.toString();
      } else {
         return null;
      }
   }

   public void onDeactivate() {
      setActiveDataFile(null);
   }

   public void mouseClicked(MouseEvent mouseEvent) {
      if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
         if (activeDataFile != null) {
            pingSettings.zoom(activeDataFile.getPingRange());
         }
      }
   }

   private final class DisplayData extends OverlayDisplayData {
      private final float y1 = overlay.getHeight();
      private final float y0 = y1 - markerHeight;
      private final float radius = lineThickness / 2f;
      private final Path2D.Float backgroundPath;
      private final Path2D.Float foregroundPath;

      private DisplayData(List<DataFile> dataFiles) {
         RangeSet<Float> xSet = new ArrayRangeSet<>();
         for (DataFile dataFile : dataFiles) {
            PingIndex pingIndex = dataFile.getPingRange().begin();
            float x = pingSettings.pingIndexToX(pingIndex);
            xSet.add(x - radius, x + radius);
         }
         int pathCapacity = 5 * xSet.size();
         backgroundPath = new Path2D.Float(Path2D.WIND_NON_ZERO, pathCapacity);
         foregroundPath = new Path2D.Float(Path2D.WIND_NON_ZERO, pathCapacity);
         xSet.forEachBeginEnd((x0, x1) -> {
            GuiUtils.appendRectangle(foregroundPath, x0, y0, x1, y1);
            GuiUtils.appendRectangle(backgroundPath, x0 - 1, y0 - 1, x1 + 1, y1);
         });
      }

      @Override
      public void draw(Graphics2D g2d) {
         g2d.setColor(Color.BLACK);
         g2d.fill(backgroundPath);

         g2d.setColor(Color.WHITE);
         g2d.fill(foregroundPath);

         if (activeDataFile != null) {
            g2d.setColor(Color.RED);
            float x = pingSettings.pingIndexToX(activeDataFile.getPingRange().begin());
            Path2D.Float activePath = new Path2D.Float(Path2D.WIND_NON_ZERO, 5);
            GuiUtils.appendRectangle(activePath, x - radius, y0, x + radius, y1);
            g2d.fill(activePath);
         }
      }

      @Override
      public boolean intersects(Rectangle2D rectangle) {
         DataFile dataFile = getIntersectingDataFile(rectangle);
         setActiveDataFile(dataFile);
         return dataFile != null;
      }
   }
}
