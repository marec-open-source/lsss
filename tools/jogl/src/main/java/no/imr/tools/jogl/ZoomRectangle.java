package no.imr.tools.jogl;

import com.jogamp.opengl.GL2;
import no.imr.tools.jogl.node.ClippingPlanes;
import no.imr.tools.jogl.node.JoglDisplayNode;
import no.imr.tools.jogl.node.JoglOrthographicModule;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Function;

import static com.jogamp.opengl.GL2.*;

public final class ZoomRectangle extends JoglDisplayNode {
   private final Function<Point, Vec3> mousePosToIntersectionPos;
   private @Nullable Drag drag;
   private final ChangeManager dragChangeManager = new ChangeManager();

   public ZoomRectangle(Function<Point, Vec3> mousePosToIntersectionPos) {
      this.mousePosToIntersectionPos = mousePosToIntersectionPos;
   }

   public @Nullable Drag getDrag() {
      return drag;
   }

   private void setDrag(@Nullable Drag drag) {
      this.drag = drag;
      dragChangeManager.notifyListeners();
      repaint();
   }

   public ChangeManager getDragChangeManager() {
      return dragChangeManager;
   }

   @Override
   protected void draw(GL2 gl) {
      if (drag != null) {
         gl.glPushAttrib(GL_ALL_ATTRIB_BITS);
         gl.glEnable(GL_LINE_STIPPLE);
         gl.glLineWidth(1);
         gl.glLineStipple(2, (short) 0xaaaa);
         gl.glDisable(GL_DEPTH_TEST);
         gl.glColor3f(0, 0, 0);
         Vec3 p1 = mousePosToIntersectionPos.apply(drag.startPixPos);
         Vec3 p2 = mousePosToIntersectionPos.apply(drag.endPixPos);
         gl.glBegin(GL_LINE_LOOP);
         gl.glVertex3f(p1.x(), p1.y(), p1.z());
         gl.glVertex3f(p1.x(), p2.y(), p1.z());
         gl.glVertex3f(p1.x(), p2.y(), p2.z());
         gl.glVertex3f(p2.x(), p2.y(), p2.z());
         gl.glVertex3f(p2.x(), p1.y(), p2.z());
         gl.glVertex3f(p2.x(), p1.y(), p1.z());
         gl.glEnd(); // GL_LINE_STRIP
         gl.glPopAttrib(); //GL_ALL_ATTRIB_BITS
      }
   }

   public record Drag(Point startPixPos, Point endPixPos) {

      private Drag shiftStartPoint(int dx, int dy) {
         return new Drag(new Point(startPixPos.x + dx, startPixPos.y + dy), endPixPos);
      }

      private Drag withEnd(Point end) {
         return new Drag(startPixPos, end);
      }
   }

   public static final class ZoomInputListener extends MouseAdapter {
      private static final double SMALLEST_ZOOM_BOX = 2;

      private final ZoomRectangle zoomRectangle;
      private final JoglOrthographicModule world;
      private @Nullable Vec3 panGrabPos;

      public ZoomInputListener(ZoomRectangle zoomRectangle, JoglOrthographicModule world) {
         this.zoomRectangle = zoomRectangle;
         this.world = world;
      }

      @Override
      public void mousePressed(MouseEvent e) {
         if (e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK) {
            Point p = e.getPoint();
            zoomRectangle.setDrag(new Drag(p, p));
         } else if (e.getModifiersEx() == (MouseEvent.BUTTON1_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)) {
            panGrabPos = world.pixPosToViewRay(e.getPoint()).origin();
         }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
         Drag drag = zoomRectangle.drag;
         if (e.getButton() == MouseEvent.BUTTON1 && drag != null) {
            zoomRectangle.setDrag(null);
            if (drag.endPixPos.distance(drag.startPixPos) < SMALLEST_ZOOM_BOX) {
               return;
            }
            Vec3 p1 = world.pixPosToViewRay(drag.startPixPos).origin();
            Vec3 p2 = world.pixPosToViewRay(drag.endPixPos).origin();
            FloatRange x = FloatRange.ofUnsorted(p1.x(), p2.x());
            FloatRange y = FloatRange.ofUnsorted(p1.y(), p2.y());
            world.setClippingPlanes(x, y);
         }
         panGrabPos = null;
      }

      @Override
      public void mouseDragged(MouseEvent e) {
         Drag drag = zoomRectangle.drag;
         if (drag != null) {
            zoomRectangle.setDrag(drag.withEnd(e.getPoint()));
         }
         if (panGrabPos != null) {
            Vec3 pos = world.pixPosToViewRay(e.getPoint()).origin();
            float dx = panGrabPos.x() - pos.x();
            float dy = panGrabPos.y() - pos.y();
            ClippingPlanes clip = world.getClippingPlanes();
            world.setClippingPlanes(clip.x().add(dx), clip.y().add(dy));
         }
      }

      public boolean keyPressed(KeyEvent keyEvent) {
         Drag drag = zoomRectangle.drag;
         if (drag == null) {
            return false;
         }

         Drag newDrag;
         switch (keyEvent.getKeyCode()) {
            case KeyEvent.VK_ESCAPE -> newDrag = null;
            case KeyEvent.VK_LEFT -> newDrag = drag.shiftStartPoint(-1, 0);
            case KeyEvent.VK_RIGHT -> newDrag = drag.shiftStartPoint(1, 0);
            case KeyEvent.VK_UP -> newDrag = drag.shiftStartPoint(0, -1);
            case KeyEvent.VK_DOWN -> newDrag = drag.shiftStartPoint(0, 1);
            default -> {
               return false;
            }
         }
         zoomRectangle.setDrag(newDrag);

         return true;
      }
   }
}
