package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.Mask;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.EchogramSettings;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.resources.LsssCursors;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Background overlay for editing the masking.
 */
public final class MaskingEditOverlay extends BaseEchogramOverlay {
   public enum MaskingMode {
      DELETE_CURRENT, DELETE_ALL, UNDELETE_CURRENT, UNDELETE_ALL
   }

   public enum DrawMode {
      BOX, VERTICAL_BAR, HORIZONTAL_BAR
   }

   private final EchogramSettings echogramSettings;

   private boolean operating;
   private final Point2D center = new Point2D.Double();
   private final Rectangle2D currentBox = new Rectangle2D.Double();
   private Rectangle2D lastUsedBox = new Rectangle2D.Double();

   public MaskingEditOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

      echogramSettings = getInterpretationSettings().getEchogramSettings();
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(GuiListeners.coalescingLater(this::update), List.of(
            echogramSettings.deleteSubMode,
            echogramSettings.deleteDrawMode,
            echogramSettings.deleteBoxSize,
            echogramSettings.deleteVerticalBarWidth,
            echogramSettings.deleteHorizontalBarHeight
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      return isActive() ? new DisplayData() : null;
   }

   @Override
   public boolean isBackgroundOverlay() {
      return true;
   }

   @Override
   public void onActivate() {
      setCursor(LsssCursors.EMPTY);

      Point mousePosition = getEchogramModule().getMousePosition();
      if (mousePosition != null) {
         setCenter(mousePosition);
      }
      recompute();
   }

   @Override
   public void onDeactivate() {
      recompute();
   }

   @Override
   public void mousePressed(MouseEvent mouseEvent) {
      if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
         operating = true;
         lastUsedBox = (Rectangle2D) currentBox.clone();
         doMasking();
      }
   }

   @Override
   public void mouseReleased(MouseEvent mouseEvent) {
      if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
         doMasking();
         operating = false;
         echogramSettings.useDefaultIfNotSticky();
      }
   }

   @Override
   public void mouseExited(MouseEvent mouseEvent) {
      repaint();
   }

   @Override
   public void mouseDragged(MouseEvent mouseEvent) {
      setCenter(mouseEvent.getPoint());
      doMasking();
   }

   @Override
   public void mouseMoved(MouseEvent mouseEvent) {
      setCenter(mouseEvent.getPoint());
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      switch (keyEvent.getKeyCode()) {
         case KeyEvent.VK_ESCAPE -> {
            if (operating) {
               operating = false;
               echogramSettings.useDefaultIfNotSticky();
            } else {
               echogramSettings.useDefault();
            }
         }
         case KeyEvent.VK_SPACE -> {
            if (keyEvent.isControlDown()) {
               echogramSettings.deleteDrawMode.shiftValue(keyEvent.isShiftDown() ? -1 : 1);
            } else {
               echogramSettings.deleteSubMode.shiftValue(keyEvent.isShiftDown() ? -1 : 1);
            }
         }
         case KeyEvent.VK_PLUS, KeyEvent.VK_ADD -> {
            adjustSize(2);
         }
         case KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT -> {
            adjustSize(-2);
         }
         default -> {
            return false;
         }
      }
      return true;
   }

   private void setCenter(Point2D point) {
      center.setLocation(point);
      updateGraphics();
   }

   private IntParameter getSizeParameter() {
      return switch (echogramSettings.deleteDrawMode.getValue()) {
         case BOX -> echogramSettings.deleteBoxSize;
         case VERTICAL_BAR -> echogramSettings.deleteVerticalBarWidth;
         case HORIZONTAL_BAR -> echogramSettings.deleteHorizontalBarHeight;
      };
   }

   private int getSize() {
      return getSizeParameter().getIntValue();
   }

   private void adjustSize(int adjustment) {
      // Make size even to avoid staggered motion when changing size on low resolution displays.
      int size = getSize() & ~1;
      getSizeParameter().setIntValue(Math.max(2, size + adjustment));
   }

   private void update() {
      updateGraphics();
      doMasking();
   }

   private void updateGraphics() {
      double x = center.getX();
      double y = center.getY();
      float radius = getSize() / 2f;
      switch (echogramSettings.deleteDrawMode.getValue()) {
         case BOX -> {
            currentBox.setFrameFromCenter(x, y, x + radius, y + radius);
         }
         case VERTICAL_BAR -> {
            currentBox.setRect(x - radius, -10, 2 * radius, getHeight() + 20);
         }
         case HORIZONTAL_BAR -> {
            currentBox.setRect(-10, y - radius, getWidth() + 20, 2 * radius);
         }
      }
      repaint();
   }

   private void doMasking() {
      if (!operating) {
         return;
      }
      Rectangle2D boxCopy = (Rectangle2D) currentBox.clone();
      executeIfEnabled(() -> {
         asyncDoMasking(lastUsedBox, boxCopy.getCenterX(), boxCopy.getCenterY());
         lastUsedBox = boxCopy;
      });
   }

   private void asyncDoMasking(Rectangle2D box, double newCenterX, double newCenterY) {
      double minX1 = box.getMinX();
      double maxX1 = box.getMaxX();
      double minY1 = box.getMinY();
      double maxY1 = box.getMaxY();

      double minX2 = newCenterX - box.getWidth() / 2;
      double maxX2 = newCenterX + box.getWidth() / 2;
      double minY2 = newCenterY - box.getHeight() / 2;
      double maxY2 = newCenterY + box.getHeight() / 2;

      double minMinY = Math.min(minY1, minY2);
      double maxMaxY = Math.max(maxY1, maxY2);

      double dx = minX2 - minX1;
      double slope = (minY2 - minY1) / dx;
      boolean ignoreSlope = Math.abs(dx) < 1;

      double minMinX = Math.min(minX1, minX2);
      double maxMaxX = Math.max(maxX1, maxX2);
      if (minMinX > getWidth() || maxMaxX < 0) {
         return;
      }
      PingIndex a = getPingSettings().xToContainingOrClosestPingIndex(minMinX);
      PingIndex b = getPingSettings().xToContainingOrClosestPingIndex(maxMaxX);
      b = getInterpretationSettings().getDataFileSet().nextOrSame(b);
      PingRange pingRange = PingRange.of(a, b).intersection(getInterpretationSettings().getPingRange());

      Map<PingIndex, FloatRangeSet> maskingMap = HashMap.newHashMap(pingRange.getPingCount());

      switch (echogramSettings.deleteDrawMode.getValue()) {
         case BOX -> {
            getRegionManager().writeablePingRanges(pingRange).stream()
                  .flatMap(getInterpretationSettings().getDataFileSet()::getPingIndexStream)
                  .forEach(pingIndex -> {
                     double minY;
                     double maxY;

                     if (ignoreSlope) {
                        minY = minMinY;
                        maxY = maxMaxY;
                     } else {
                        minY = Double.MAX_VALUE;
                        maxY = -Double.MAX_VALUE;
                        for (int i = 0; i < 2; i++) {
                           double x = (i == 0) ? getPingSettings().pingIndexToX(pingIndex)
                                 : getPingSettings().pingIndexToX(getInterpretationSettings().getDataFileSet().nextOrSame(pingIndex));

                           double dyMinX = (x - minX1) * slope;
                           double dyMaxX = (x - maxX1) * slope;

                           minY = Math.min(minY, Math.clamp(minY1 + Math.min(dyMinX, dyMaxX), minMinY, maxMaxY));
                           maxY = Math.max(maxY, Math.clamp(maxY1 + Math.max(dyMinX, dyMaxX), minMinY, maxMaxY));
                        }
                     }

                     float depth1 = getZSettings().yToDepth(minY, pingIndex);
                     float depth2 = getZSettings().yToDepth(maxY, pingIndex);
                     maskingMap.put(pingIndex, FloatRangeSet.of(FloatRange.ofUnsorted(depth1, depth2)));
                  });
         }
         case VERTICAL_BAR -> {
            getRegionManager().writeablePingRanges(pingRange).stream()
                  .flatMap(getInterpretationSettings().getDataFileSet()::getPingIndexStream)
                  .forEach(pingIndex -> {
                     maskingMap.put(pingIndex, FloatRangeSet.of(Mask.ENTIRE_PING_DEPTH_RANGE));
                  });
         }
         case HORIZONTAL_BAR -> {
            getRegionManager().writeablePingRanges(getInterpretationSettings().getPingRange()).stream()
                  .flatMap(getInterpretationSettings().getDataFileSet()::getPingIndexStream)
                  .forEach(pingIndex -> {
                     float depth1 = getZSettings().yToDepth(minMinY, pingIndex);
                     float depth2 = getZSettings().yToDepth(maxMaxY, pingIndex);
                     maskingMap.put(pingIndex, FloatRangeSet.of(FloatRange.ofUnsorted(depth1, depth2)));
                  });
         }
      }

      switch (echogramSettings.deleteSubMode.getValue()) {
         case DELETE_CURRENT -> {
            getRegionManager().getMaskingManager().mask(maskingMap, getInterpretationSettings().getChannel());
         }
         case DELETE_ALL -> {
            getRegionManager().getMaskingManager().maskAllChannels(maskingMap);
         }
         case UNDELETE_CURRENT -> {
            getRegionManager().getMaskingManager().unmask(maskingMap, getInterpretationSettings().getChannel());
         }
         case UNDELETE_ALL -> {
            getRegionManager().getMaskingManager().unmaskAllChannels(maskingMap);
         }
      }
   }

   private final class DisplayData implements OverlayDisplayData {
      private DisplayData() {
      }

      @Override
      public void draw(Graphics2D g2d) {
         Point mousePosition = getEchogramModule().getMousePosition();
         if (mousePosition == null) {
            return;
         }

         g2d.setColor(Color.BLACK);
         g2d.setStroke(GuiUtils.STROKE_3);
         g2d.draw(currentBox);

         g2d.setColor(Color.WHITE);
         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.draw(currentBox);

         String text = switch (echogramSettings.deleteSubMode.getValue()) {
            case DELETE_CURRENT -> "Delete (on current frequency)";
            case DELETE_ALL -> "Delete (on all frequencies)";
            case UNDELETE_CURRENT -> "Undelete (on current frequency)";
            case UNDELETE_ALL -> "Undelete (on all frequencies)";
         };

         switch (echogramSettings.deleteDrawMode.getValue()) {
            case BOX -> {
               GuiText.draw(g2d, text, Color.BLACK, (float) currentBox.getCenterX(), (float) currentBox.getMinY() - 1, GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.BOTTOM, null);
            }
            case VERTICAL_BAR, HORIZONTAL_BAR -> {
               GuiText.draw(g2d, text, Color.BLACK, mousePosition.x, mousePosition.y, GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.CENTER, null);
            }
         }
      }

      @Override
      public boolean intersects(Rectangle2D rectangle) {
         return true;
      }
   }
}
