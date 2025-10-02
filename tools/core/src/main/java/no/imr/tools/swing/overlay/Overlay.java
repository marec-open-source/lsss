package no.imr.tools.swing.overlay;

import no.imr.tools.LateInit;
import no.imr.tools.swing.Repaintable;
import org.jspecify.annotations.Nullable;

import javax.swing.JPopupMenu;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;

/**
 * An overlay on an {@link OverlaidComponent}.
 */
public abstract class Overlay implements Repaintable {
   private final LateInit<OverlaidComponent<?>> overlaidComponent = new LateInit<>();

   protected Overlay() {
   }

   public OverlaidComponent<?> getOverlaidComponent() {
      return overlaidComponent.get();
   }

   void setOverlaidComponent(OverlaidComponent<?> overlaidComponent) {
      this.overlaidComponent.init(overlaidComponent);
   }

   public int getWidth() {
      return getOverlaidComponent().getWidth();
   }

   public int getHeight() {
      return getOverlaidComponent().getHeight();
   }

   @Override
   public void repaint() {
      getOverlaidComponent().repaint();
   }

   public void setCursor(Cursor cursor) {
      getOverlaidComponent().setCursor(cursor);
   }

   public boolean overlaps(Rectangle2D rectangle2D) {
      return false;
   }

   public Cursor getCursor() {
      return Cursor.getDefaultCursor();
   }

   public void resized(GraphicsConfiguration graphicsConfiguration, int width, int height) {
   }

   public void draw(Graphics2D g2d) {
   }

   public void drawText(Graphics2D g2d) {
   }

   public void keyTyped(KeyEvent e) {
   }

   public void keyPressed(KeyEvent e) {
   }

   public void keyReleased(KeyEvent e) {
   }

   public void mouseClicked(MouseEvent e) {
   }

   public void mousePressed(MouseEvent e) {
   }

   public void mouseReleased(MouseEvent e) {
   }

   public void mouseEntered(MouseEvent e) {
   }

   public void mouseExited(MouseEvent e) {
   }

   public void mouseDragged(MouseEvent e) {
   }

   public void mouseMoved(MouseEvent e) {
   }

   public void mouseWheelMoved(MouseWheelEvent e) {
   }

   /**
    * {@return true if the overlay should be drawn}
    */
   public boolean isEnabled() {
      return true;
   }

   public @Nullable JPopupMenu getPopupMenu(Point point) {
      return null;
   }
}
