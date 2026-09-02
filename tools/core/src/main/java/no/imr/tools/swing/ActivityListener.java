package no.imr.tools.swing;

import no.imr.tools.listening.ArgChangeManager;

import javax.swing.Timer;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.time.Duration;
import java.time.Instant;

/**
 * Registers if a component hierarchy receives input from user.
 */
public final class ActivityListener extends MouseAndKeyAdapter {
   private final ArgChangeManager<Boolean> changeManager = new ArgChangeManager<>();
   private final Timer timer = new Timer(1000, _ -> tick());

   private final DeepInputListener deepInputListener;
   private Duration threshold;
   private Instant lastActionTime = Instant.now();
   private boolean active = true;

   public ActivityListener(Component component, Duration threshold) {
      deepInputListener = new DeepInputListener(component, this);
      this.threshold = threshold;
      timer.start();
   }

   public void stop() {
      deepInputListener.stop();
      timer.stop();
   }

   public ArgChangeManager<Boolean> getChangeManager() {
      return changeManager;
   }

   public void setThreshold(Duration threshold) {
      this.threshold = threshold;
   }

   public boolean isActive() {
      return active;
   }

   private void tick() {
      if (Instant.now().isAfter(lastActionTime.plus(threshold))) {
         setActive(false);
      }
   }

   private void action() {
      lastActionTime = Instant.now();
      setActive(true);
   }

   private void setActive(boolean active) {
      if (this.active != active) {
         this.active = active;
         changeManager.notifyListeners(active);
      }
   }

   @Override
   public void keyTyped(KeyEvent e) {
      action();
   }

   @Override
   public void keyPressed(KeyEvent e) {
      action();
   }

   @Override
   public void keyReleased(KeyEvent e) {
      action();
   }

   @Override
   public void mouseClicked(MouseEvent e) {
      action();
   }

   @Override
   public void mousePressed(MouseEvent e) {
      action();
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      action();
   }

   @Override
   public void mouseEntered(MouseEvent e) {
      action();
   }

   @Override
   public void mouseExited(MouseEvent e) {
      // Do nothing on mouse exit
   }

   @Override
   public void mouseWheelMoved(MouseWheelEvent e) {
      action();
   }

   @Override
   public void mouseMoved(MouseEvent e) {
      action();
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      action();
   }
}
