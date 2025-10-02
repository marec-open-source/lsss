package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.EchogramRectangle;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.lsss.framework.NavigationHistory;
import no.imr.lsss.modules.echogram.EchogramModuleUtils;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.Consumer;

public final class ZoomEngine extends ZoomBaseEngine {
   private enum ZoomAction {
      NONE,
      IN_BY_CLICK,
      IN_BY_DRAG,
      OUT,
      OUT_MAXIMALLY
   }

   private enum DragDirection {
      HORIZONTALLY, VERTICALLY, BOTH
   }

   private final NavigationHistory navigationHistory;
   private final EchogramPingSettings pingSettings;
   private final EchogramZSettings zSettings;
   private final Consumer<@Nullable EchogramRectangle> listener;
   private final int mouseButton;
   private int modifiers;
   private Point2D referencePoint;
   private Point2D currentPoint;
   private @Nullable EchogramRectangle echogramRectangle;
   private @Nullable Rectangle2D imageRectangle;
   private ZoomAction action = ZoomAction.NONE;
   private DragDirection dragDirection = DragDirection.BOTH;

   private @Nullable DragDirection lastNonBothDragDirection;
   private boolean initedDragDirection;

   public ZoomEngine(MouseEvent mouseEvent, InterpretationSettings interpretationSettings,
                     EchogramPingSettings pingSettings, EchogramZSettings zSettings,
                     Consumer<@Nullable EchogramRectangle> listener) {
      navigationHistory = interpretationSettings.getNavigationHistory();
      this.pingSettings = pingSettings;
      this.zSettings = zSettings;
      this.listener = listener;
      mouseButton = mouseEvent.getButton();
      modifiers = mouseEvent.getModifiersEx();
      referencePoint = mouseEvent.getPoint();
      currentPoint = referencePoint;
      update();
   }

   @Override
   public void draw(Graphics2D g2d) {
      if (imageRectangle == null) {
         return;
      }

      g2d.setColor(Color.BLACK);
      g2d.setStroke(GuiUtils.STROKE_3);
      g2d.draw(imageRectangle);

      g2d.setColor(Color.WHITE);
      g2d.setStroke(GuiUtils.STROKE_1);
      g2d.draw(imageRectangle);
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      if (imageRectangle == null) {
         return false;
      }
      switch (keyEvent.getKeyCode()) {
         case ' ' -> {
            setNextDragDirection();
         }
         case KeyEvent.VK_LEFT -> {
            shiftReferencePoint(-1, 0);
         }
         case KeyEvent.VK_RIGHT -> {
            shiftReferencePoint(1, 0);
         }
         case KeyEvent.VK_UP -> {
            shiftReferencePoint(0, -1);
         }
         case KeyEvent.VK_DOWN -> {
            shiftReferencePoint(0, 1);
         }
         default -> {
            return false;
         }
      }
      return true;
   }

   private void setDragDirection(DragDirection dragDirection) {
      if (this.dragDirection == dragDirection) {
         return;
      }
      this.dragDirection = dragDirection;
      if (dragDirection != DragDirection.BOTH) {
         lastNonBothDragDirection = dragDirection;
      }
      update();
   }

   private void setNextDragDirection() {
      if (imageRectangle == null) {
         return; // zoom by dragging is not activated yet
      }
      if (dragDirection != DragDirection.BOTH) {
         setDragDirection(DragDirection.BOTH);
      } else {
         if (lastNonBothDragDirection == DragDirection.HORIZONTALLY) {
            setDragDirection(DragDirection.VERTICALLY);
         } else if (lastNonBothDragDirection == DragDirection.VERTICALLY) {
            setDragDirection(DragDirection.HORIZONTALLY);
         } else {
            if (imageRectangle.getWidth() >= imageRectangle.getHeight()) {
               setDragDirection(DragDirection.HORIZONTALLY);
            } else {
               setDragDirection(DragDirection.VERTICALLY);
            }
         }
      }
   }

   private void setReferencePoint(Point2D point) {
      double x = Math.clamp(point.getX(), 0, pingSettings.getWidth() - 1);
      double y = Math.clamp(point.getY(), 0, zSettings.getHeight() - 1);
      referencePoint = new Point2D.Double(x, y);
      update();
   }

   private void shiftReferencePoint(int dx, int dy) {
      setReferencePoint(new Point2D.Double(referencePoint.getX() + dx, referencePoint.getY() + dy));
   }

   @Override
   public void update(MouseEvent mouseEvent) {
      currentPoint = mouseEvent.getPoint();
      modifiers = mouseEvent.getModifiersEx();
      update();
   }

   private void update() {
      action = getZoomAction();

      if (action == ZoomAction.IN_BY_DRAG) {
         EchogramPoint a = EchogramModuleUtils.imagePointToClampedEchogramPoint(referencePoint, pingSettings, zSettings);
         EchogramPoint b = EchogramModuleUtils.imagePointToClampedEchogramPoint(currentPoint, pingSettings, zSettings);
         DepthTransform depthTransform = zSettings.getDepthTransform();
         PingRange pingRange = dragDirection == DragDirection.VERTICALLY
               ? pingSettings.getPingRange()
               : PingRange.from(List.of(a.pingIndex(), b.pingIndex()), pingSettings.getPingContainer());
         FloatRange zRange = dragDirection == DragDirection.HORIZONTALLY
               ? zSettings.getZoomedZRange()
               : FloatRange.ofUnsorted(depthTransform.depthToZ(a), depthTransform.depthToZ(b)).expandToNonDegenerated();
         echogramRectangle = new EchogramRectangle(pingRange, zRange, depthTransform);
         imageRectangle = EchogramModuleUtils.toImageRectangle(echogramRectangle, pingSettings, zSettings);
      } else {
         echogramRectangle = null;
         imageRectangle = null;
      }

      listener.accept(echogramRectangle);
   }

   private ZoomAction getZoomAction() {
      int mouseMovementThreshold = 5;
      int mouseDragThreshold = 3;

      if (mouseButton == MouseEvent.BUTTON1 && (modifiers & MouseEvent.ALT_DOWN_MASK) == 0) {
         double dx = Math.abs(currentPoint.getX() - referencePoint.getX());
         double dy = Math.abs(currentPoint.getY() - referencePoint.getY());

         if (dx > mouseMovementThreshold || dy > mouseMovementThreshold) {
            if (!initedDragDirection) {
               initedDragDirection = true;
               if (dx > mouseDragThreshold && dy > mouseDragThreshold) {
                  setDragDirection(DragDirection.BOTH);
               } else if (dx > mouseDragThreshold) {
                  setDragDirection(DragDirection.HORIZONTALLY);
               } else {
                  setDragDirection(DragDirection.VERTICALLY);
               }
            }
            return ZoomAction.IN_BY_DRAG;
         } else {
            return ZoomAction.IN_BY_CLICK;
         }
      } else if (mouseButton == MouseEvent.BUTTON2
            || (mouseButton == MouseEvent.BUTTON1 && (modifiers & MouseEvent.ALT_DOWN_MASK) != 0)) {
         if ((modifiers & MouseEvent.CTRL_DOWN_MASK) != 0) {
            return ZoomAction.OUT_MAXIMALLY;
         } else {
            return ZoomAction.OUT;
         }
      } else {
         return ZoomAction.NONE;
      }
   }

   @Override
   public void doZoom() {
      navigationHistory.doWithNoAddCheckPoint(this::privateDoZoom);
      navigationHistory.addCheckPoint();
   }

   private void privateDoZoom() {
      switch (action) {
         case NONE -> {
            // Do nothing.
         }
         case IN_BY_CLICK -> {
            pingSettings.zoom(referencePoint.getX(), 2);
         }
         case IN_BY_DRAG -> {
            if (echogramRectangle != null) {
               pingSettings.zoom(echogramRectangle.pingRange());
               zSettings.setZ(echogramRectangle.zRange());
            }
         }
         case OUT -> {
            pingSettings.zoom(referencePoint.getX(), 0.5);
            zSettings.zoom(currentPoint, 0.5);
         }
         case OUT_MAXIMALLY -> {
            pingSettings.zoomOut();
            zSettings.zoomOut();
         }
      }
   }
}
