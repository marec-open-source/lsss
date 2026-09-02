package no.imr.tools.jogl;

import no.imr.tools.jogl.node.JoglPerspectiveModule;
import no.imr.tools.math.MathUtils;
import no.imr.tools.math.linalg.Matrix3;
import no.imr.tools.math.linalg.TRS;
import no.imr.tools.math.linalg.Vec3;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Mouse and keyboard input.
 */
public final class Jogl3DInputListener implements MouseListener, MouseMotionListener, MouseWheelListener {
   private long lastTime;
   private int lastX;
   private int lastY;
   private long lastDt;
   private int lastDx;
   private int lastDy;
   private @Nullable Timer rotationTimer;
   private final JoglPerspectiveModule world;
   private final ModelProvider modelProvider;

   public Jogl3DInputListener(JoglPerspectiveModule world, ModelProvider modelProvider) {
      this.world = world;
      this.modelProvider = modelProvider;
   }

   private void startRotationTimer(MouseEvent event, int dt, int dx, int dy) {
      int timerDx = Math.clamp(dx, -10, 10);
      int timerDy = Math.clamp(dy, -10, 10);
      int timerDt = Math.max(dt, 50); // Do not allow too fast movement.
      rotationTimer = new Timer(timerDt, _ -> {
         doRotation(event, timerDx, timerDy);
         modelProvider.repaint();
      });
      rotationTimer.start();
   }

   public void stopRotationTimer() {
      if (rotationTimer != null) {
         rotationTimer.stop();
      }
   }

   public boolean isAnimating() {
      return rotationTimer != null && rotationTimer.isRunning();
   }

   @Override
   public void mouseClicked(MouseEvent e) {
   }

   @Override
   public void mousePressed(MouseEvent e) {
      lastTime = System.currentTimeMillis();
      lastDt = 0;
      lastX = e.getX();
      lastY = e.getY();
      lastDx = 0;
      lastDy = 0;
      stopRotationTimer();
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      long deltaTime = System.currentTimeMillis() - lastTime;
      boolean interactiveMode = modelProvider.isInteractiveMode();
      stopRotationTimer();
      if (deltaTime < 200 && (Math.abs((float) lastDx / deltaTime) >= 0.01 || Math.abs((float) lastDy / deltaTime) >= 0.01)) {
         modelProvider.setInteractiveMode(true);
         startRotationTimer(e, (int) lastDt, lastDx, lastDy);
      } else if (interactiveMode) {
         modelProvider.setInteractiveMode(false);
         modelProvider.repaint();
      }
   }

   @Override
   public void mouseEntered(MouseEvent e) {
      world.getComponent().setFocusable(true);
      world.getComponent().requestFocusInWindow();
   }

   @Override
   public void mouseExited(MouseEvent e) {
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      if (SwingUtilities.isLeftMouseButton(e)) {
         modelProvider.setInteractiveMode(true);
         int x = e.getX();
         int y = e.getY();
         lastDx = x - lastX;
         lastDy = lastY - y;
         long currentTime = System.currentTimeMillis();
         lastDt = currentTime - lastTime;
         lastTime = currentTime;
         if (!doRotation(e, lastDx, lastDy)) {
            return;
         }
         lastX = x;
         lastY = y;
         modelProvider.repaint();
      }
   }

   private boolean doRotation(MouseEvent e, int dx, int dy) {
      if (dx == 0 && dy == 0) {
         return false;
      }

      TRS trs = modelProvider.getTRS();
      Vec3 center = trs.transformPoint(modelProvider.getCenter());

      if ((e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0) {
         float startX = (float) lastX / world.getJava2dWidth() - 0.5f;
         float startY = (float) lastY / world.getJava2dHeight() - 0.5f;
         float endX = startX + (float) dx / world.getJava2dWidth();
         float endY = startY - (float) dy / world.getJava2dHeight();
         Matrix3 dR = createRotationAroundZ(startX, startY, endX, endY);
         trs = trs.rotate(dR, center);
         modelProvider.setTRS(trs);
      } else if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
         Vec3 dir = getDir(lastX, lastY);
         float f = center.z() / dir.z();
         trs = trs.translate(new Vec3(dx, dy, 0).times(f));
         modelProvider.setTRS(trs);
      } else if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
         zoom(dy / 4, new Vec3(0, 0, 1));
      } else {
         Vec3 rotAxis = new Vec3(-dy, dx, 0).unit();
         double angle = MathUtils.hypot(dx, dy);
         Matrix3 dR = Matrix3.createRotation(angle, rotAxis);
         trs = trs.rotate(dR, center);
         modelProvider.setTRS(trs);
      }
      return true;
   }

   @Override
   public void mouseMoved(MouseEvent e) {
   }

   @Override
   public void mouseWheelMoved(MouseWheelEvent e) {
      int wheelRotation = e.getWheelRotation();
      int x = e.getX();
      int y = e.getY();
      zoom(wheelRotation, getDir(x, y).unit());
   }

   private void zoom(int wheelRotation, Vec3 dir) {
      TRS trs = modelProvider.getTRS();
      float z = trs.transformPointGetZ(modelProvider.getCenter());
      float dz = (float) (z * (Math.pow(1.05, wheelRotation) - 1));
      trs = trs.translate(dir.times(dz));
      modelProvider.setTRS(trs);
      modelProvider.repaint();
   }

   private Vec3 getDir(int pixelX, int pixelY) {
      float h = world.getJava2dHeight();
      float w = world.getJava2dWidth();
      float y = h / 2 - pixelY;
      float x = pixelX - w / 2;
      float z = -(float) ((h / 2) / Math.tan(Math.toRadians(world.getFovy() / 2)));
      return new Vec3(x, y, z);
   }

   private static Matrix3 createRotationAroundZ(float startX, float startY, float endX, float endY) {
      Vec3 startVec = new Vec3(startX, startY, 0);
      Vec3 endVec = new Vec3(endX, endY, 0);
      if (startVec.equals(endVec) || startVec.length() == 0 || endVec.length() == 0) {
         return Matrix3.IDENTITY;
      } else {
         float cross = startVec.crossGetZ(endVec);

         float rotation = (float) MathUtils.acosClamped(startVec.unit().dot(endVec.unit()));
         if (cross > 0) {
            rotation = -rotation;
         }
         return Matrix3.createRotation(Math.toDegrees(rotation), new Vec3(0, 0, 1));
      }
   }

   public interface ModelProvider {
      TRS getTRS();

      void setTRS(TRS trs);

      Vec3 getCenter();

      boolean isInteractiveMode();

      void setInteractiveMode(boolean mode);

      void repaint();
   }
}
