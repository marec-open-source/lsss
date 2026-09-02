package no.imr.tools.swing.overlay;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputListener;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A component with overlays.
 *
 * @see Overlay
 */
public final class OverlaidComponent<O extends Overlay> extends JComponent {
   private final List<O> overlays = new ArrayList<>();
   private Overlay activeOverlay = NullOverlay.INSTANCE;
   private @Nullable Point mousePosition;

   public OverlaidComponent() {
      addComponentListener(new ComponentAdapter() {
         @Override
         public void componentResized(ComponentEvent e) {
            for (Overlay overlay : overlays) {
               overlay.resized(getGraphicsConfiguration(), getWidth(), getHeight());
            }
         }
      });

      InputListener inputListener = new InputListener();
      addMouseListener(inputListener);
      addMouseMotionListener(inputListener);
      addMouseWheelListener(inputListener);
      addKeyListener(inputListener);
   }

   public void addOverlay(O overlay) {
      overlays.add(overlay);
      overlay.setOverlaidComponent(this);
   }

   public List<O> getOverlays() {
      return overlays;
   }

   @Override
   protected void paintComponent(Graphics g) {
      Graphics2D graphics2D = (Graphics2D) g;

      for (Overlay overlay : overlays) {
         if (overlay.isEnabled()) {
            Graphics2D tmpG2d = (Graphics2D) graphics2D.create();
            overlay.draw(tmpG2d);
            tmpG2d.dispose();
         }
      }

      for (Overlay overlay : overlays) {
         if (overlay.isEnabled()) {
            overlay.drawText(graphics2D);
         }
      }
   }

   private Overlay findOverlappingOverlay(@Nullable Point point) {
      if (point == null) {
         return NullOverlay.INSTANCE;
      }

      Rectangle2D rectangle = createRectangle(point);

      for (int i = overlays.size() - 1; i >= 0; i--) {
         Overlay overlay = overlays.get(i);
         if (overlay.isEnabled() && overlay.overlaps(rectangle)) {
            return overlay;
         }
      }

      return NullOverlay.INSTANCE;
   }

   private static Rectangle2D createRectangle(Point point) {
      int d = 5;
      int w = 2 * d + 1;
      return new Rectangle2D.Double(point.getX() - d, point.getY() - d, w, w);
   }

   private void updateActiveOverlay() {
      setActiveOverlay(findOverlappingOverlay(mousePosition));
   }

   private void setActiveOverlay(Overlay overlay) {
      activeOverlay = overlay;
      setCursor(activeOverlay.getCursor());
   }

   private void showPopupMenu(Point point) {
      JPopupMenu menu = getFirstPossiblePopupMenuAmongOverlays(point);
      if (menu != null) {
         menu.show(this, point.x, point.y);
      }
   }

   private @Nullable JPopupMenu getFirstPossiblePopupMenuAmongOverlays(Point point) {
      Rectangle2D rectangle = createRectangle(point);
      for (int i = overlays.size() - 1; i >= 0; i--) {
         O overlay = overlays.get(i);
         if (overlay.overlaps(rectangle)) {
            JPopupMenu menu = overlay.getPopupMenu(point);
            if (menu != null) {
               return menu;
            }
         }
      }
      return null;
   }

   /**
    * Listener for mouse and keyboard.
    */
   private final class InputListener implements MouseInputListener, MouseWheelListener, KeyListener {
      private InputListener() {
      }

      @Override
      public void mouseClicked(MouseEvent e) {
         activeOverlay.mouseClicked(e);
      }

      @Override
      public void mousePressed(MouseEvent e) {
         if (e.isPopupTrigger()) {
            showPopupMenu(e.getPoint());
         } else {
            activeOverlay.mousePressed(e);
         }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
         if (e.isPopupTrigger()) {
            showPopupMenu(e.getPoint());
         } else {
            activeOverlay.mouseReleased(e);
         }
      }

      @Override
      public void mouseEntered(MouseEvent e) {
         mousePosition = e.getPoint();
         setFocusable(true);
         requestFocusInWindow();

         activeOverlay.mouseEntered(e);
      }

      @Override
      public void mouseExited(MouseEvent e) {
         mousePosition = null;
         setFocusable(false);

         activeOverlay.mouseExited(e);
      }

      @Override
      public void mouseDragged(MouseEvent e) {
         mousePosition = e.getPoint();

         activeOverlay.mouseDragged(e);
      }

      @Override
      public void mouseMoved(MouseEvent e) {
         mousePosition = e.getPoint();
         updateActiveOverlay();

         activeOverlay.mouseMoved(e);
      }

      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
         activeOverlay.mouseWheelMoved(e);
      }

      @Override
      public void keyTyped(KeyEvent e) {
         activeOverlay.keyTyped(e);
      }

      @Override
      public void keyPressed(KeyEvent e) {
         activeOverlay.keyPressed(e);
      }

      @Override
      public void keyReleased(KeyEvent e) {
         activeOverlay.keyReleased(e);
      }
   }
}
