package no.imr.korona.viewer.overlays;

import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.listening.Listener;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.animatedshape.AnimatedShape;
import no.imr.tools.swing.animatedshape.DashedAnimatedShape;
import no.marec.lsss.api.util.observing.Subscription;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public final class RectangleSelectionOverlay extends EchogramOverlay {
   private final RectangleRegion rectangleRegion;
   private final AnimatedShape animatedDraw = new AnimatedShape(this, new DashedAnimatedShape());
   private final Subscription subscription;
   private int outcode;
   private Point previousPosition = new Point();

   public RectangleSelectionOverlay(RectangleRegion rectangleRegion) {
      this.rectangleRegion = rectangleRegion;
      subscription = rectangleRegion.getChangeManager().subscribe(Listener.of(this::repaint));
   }

   @Override
   public void draw(Graphics2D g) {
      animatedDraw.draw(g, getSelectedRectangle());
   }

   @Override
   public void close() {
      super.close();
      animatedDraw.stop();
      subscription.unsubscribe();
   }

   @Override
   public boolean overlaps(Rectangle2D rectangle2D) {
      Rectangle2D selectedRectangle = getSelectedRectangle();
      boolean intersects = selectedRectangle.intersects(rectangle2D);
      if (intersects) {
         double rectHalfWidth = Math.max(1, rectangle2D.getWidth() * 0.5);
         double rectHalfHeight = Math.max(1, rectangle2D.getHeight() * 0.5);
         double edgeWidth = selectedRectangle.getWidth() >= rectHalfWidth * 2 + 3
               ? rectHalfWidth
               : selectedRectangle.getWidth() >= (rectHalfWidth - 1) * 2 + 3 ? rectHalfWidth - 1 : 0;
         double edgeHeight = selectedRectangle.getHeight() >= rectHalfHeight * 2 + 3
               ? rectHalfHeight
               : selectedRectangle.getHeight() >= (rectHalfHeight - 1) * 2 + 3 ? rectHalfHeight - 1 : 0;
         Rectangle2D rect = new Rectangle2D.Double(selectedRectangle.getMinX() + edgeWidth, selectedRectangle.getMinY() + edgeHeight,
               selectedRectangle.getWidth() - 2 * edgeWidth, selectedRectangle.getHeight() - 2 * edgeHeight);

         Point2D center = new Point2D.Double(rectangle2D.getCenterX(), rectangle2D.getCenterY());
         outcode = rect.outcode(center);
      }
      return intersects;
   }

   private Rectangle2D getSelectedRectangle() {
      FloatRange pingOffset = rectangleRegion.getPingOffset();
      float yMin = getEchogramDisplay().depthToY(rectangleRegion.getDepthRange().min());
      float yMax = getEchogramDisplay().depthToY(rectangleRegion.getDepthRange().max());
      return new Rectangle2D.Float(getWidth() + pingOffset.min(), yMin, Math.max(1, pingOffset.getSize()), Math.max(1, yMax - yMin));
   }

   @Override
   public Cursor getCursor() {
      return switch (outcode) {
         case 0 -> Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
         case Rectangle.OUT_LEFT -> Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
         case Rectangle.OUT_RIGHT -> Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
         case Rectangle.OUT_TOP -> Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
         case Rectangle.OUT_BOTTOM -> Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
         case Rectangle.OUT_TOP + Rectangle.OUT_LEFT -> Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
         case Rectangle.OUT_TOP + Rectangle.OUT_RIGHT -> Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
         case Rectangle.OUT_BOTTOM + Rectangle.OUT_LEFT -> Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
         case Rectangle.OUT_BOTTOM + Rectangle.OUT_RIGHT -> Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
         default -> throw new ShouldNotHappenException(Integer.toString(outcode));
      };
   }

   @Override
   public void mousePressed(MouseEvent e) {
      previousPosition = e.getPoint();
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      int offset = Math.clamp(e.getX() - getWidth(), -getWidth(), 0);
      float depth = getEchogramDisplay().getDepthRange().clamp(getEchogramDisplay().yToDepth(e.getY()));
      switch (outcode) {
         case 0 -> {
            move(e.getX() - previousPosition.x, getEchogramDisplay().getDepthRange().getSize() * (e.getY() - previousPosition.y) / getHeight());
         }
         case Rectangle.OUT_LEFT -> extendLeft(offset);
         case Rectangle.OUT_RIGHT -> extendRight(offset);
         case Rectangle.OUT_TOP -> extendTop(depth);
         case Rectangle.OUT_BOTTOM -> extendBottom(depth);

         case Rectangle.OUT_TOP + Rectangle.OUT_LEFT -> {
            extendLeft(offset);
            extendTop(depth);
         }
         case Rectangle.OUT_TOP + Rectangle.OUT_RIGHT -> {
            extendRight(offset);
            extendTop(depth);
         }
         case Rectangle.OUT_BOTTOM + Rectangle.OUT_LEFT -> {
            extendLeft(offset);
            extendBottom(depth);
         }
         case Rectangle.OUT_BOTTOM + Rectangle.OUT_RIGHT -> {
            extendRight(offset);
            extendBottom(depth);
         }
         default -> {
            throw new ShouldNotHappenException(Integer.toString(outcode));
         }
      }
      previousPosition = e.getPoint();
   }

   @Override
   public void mouseWheelMoved(MouseWheelEvent e) {
      float zoomFactor = (float) Math.pow(1.1, -e.getWheelRotation());

      FloatRange pingOffset = rectangleRegion.getPingOffset().zoom(zoomFactor);
      rectangleRegion.setPingOffset(pingOffset.intersection(FloatRange.of(-getWidth(), 0)));

      FloatRange depthRange = rectangleRegion.getDepthRange().zoom(zoomFactor);
      rectangleRegion.setDepth(depthRange.intersection(getEchogramDisplay().getDepthRange()));
   }

   private void extendLeft(int offset) {
      FloatRange pingOffset = rectangleRegion.getPingOffset();
      float minOffset = Math.min(offset, pingOffset.max() - RectangleRegion.MIN_PINGS);
      rectangleRegion.setPingOffset(FloatRange.of(minOffset, pingOffset.max()));
   }

   private void extendRight(int offset) {
      FloatRange pingOffset = rectangleRegion.getPingOffset();
      float maxOffset = Math.max(offset, pingOffset.min() + RectangleRegion.MIN_PINGS);
      rectangleRegion.setPingOffset(FloatRange.of(pingOffset.min(), maxOffset));
   }

   private void extendTop(float depth) {
      FloatRange depthRange = rectangleRegion.getDepthRange();
      float minDepth = Math.min(depth, depthRange.max() - RectangleRegion.MIN_DEPTH);
      rectangleRegion.setDepth(FloatRange.of(minDepth, depthRange.max()));
   }

   private void extendBottom(float depth) {
      FloatRange depthRange = rectangleRegion.getDepthRange();
      float maxDepth = Math.max(depth, depthRange.min() + RectangleRegion.MIN_DEPTH);
      rectangleRegion.setDepth(FloatRange.of(depthRange.min(), maxDepth));
   }

   private void move(int deltaOffset, float deltaDepth) {
      rectangleRegion.setPingOffset(rectangleRegion.getPingOffset().add(deltaOffset).shiftToBeContainedIn(FloatRange.of(-getWidth(), 0)));
      rectangleRegion.setDepth(rectangleRegion.getDepthRange().add(deltaDepth).shiftToBeContainedIn(getEchogramDisplay().getDepthRange()));
   }

   @Override
   public void depthRangeChanged() {
      keepInVisibleRange();
   }

   public void keepInVisibleRange() {
      FloatRange pingOffset = FloatRange.of(-getWidth(), 0);
      if (!pingOffset.contains(rectangleRegion.getPingOffset())) {
         rectangleRegion.setPingOffset(rectangleRegion.getPingOffset().shiftToBeContainedIn(pingOffset));
      }
      if (!getEchogramDisplay().getDepthRange().contains(rectangleRegion.getDepthRange())) {
         rectangleRegion.setDepth(rectangleRegion.getDepthRange().shiftToBeContainedIn(getEchogramDisplay().getDepthRange()));
      }
   }
}
