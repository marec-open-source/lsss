package no.imr.tools.swing;

import javax.swing.Timer;
import java.awt.Component;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

public final class WhenShowingTimer implements HierarchyListener {
   private final Component component;
   private final Timer timer;

   private WhenShowingTimer(Component component, int delay, Runnable listener) {
      this.component = component;
      timer = new Timer(delay, e -> listener.run());
      timer.setInitialDelay(0);

      component.addHierarchyListener(this);

      if (component.isShowing()) {
         timer.start();
      }
   }

   public static void start(Component component, int delay, Runnable listener) {
      new WhenShowingTimer(component, delay, listener);
   }

   @Override
   public void hierarchyChanged(HierarchyEvent e) {
      if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
         update();
      }
   }

   private void update() {
      if (component.isShowing()) {
         timer.start();
      } else {
         timer.stop();
      }
   }
}
