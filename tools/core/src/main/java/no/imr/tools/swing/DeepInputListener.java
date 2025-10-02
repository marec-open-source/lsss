package no.imr.tools.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

/**
 * A key and mouse listener that attaches itself to all components in a hierarchy.
 */
public final class DeepInputListener {
   private final Component topComponent;
   private final MouseAndKeyAdapter listener;
   private int installedCount;
   private final ContainerListener containerListener = new ContainerListener() {
      @Override
      public void componentAdded(ContainerEvent e) {
         install(e.getChild());
      }

      @Override
      public void componentRemoved(ContainerEvent e) {
         remove(e.getChild());
      }
   };

   public DeepInputListener(Component topComponent, MouseAndKeyAdapter listener) {
      this.topComponent = topComponent;
      this.listener = listener;
      install(topComponent);
   }

   public void stop() {
      if (installedCount == 0) {
         return;
      }
      remove(topComponent);
      assert installedCount == 0 : installedCount;
   }

   private void install(Component component) {
      installedCount++;

      component.addFocusListener(listener);
      component.addKeyListener(listener);
      component.addMouseListener(listener);
      component.addMouseMotionListener(listener);
      // Adding a MouseWheelListener can prevent scrolling by JScrollPane
      // component.addMouseWheelListener(listener);

      if (component instanceof Container container) {
         container.addContainerListener(containerListener);
         for (int i = 0; i < container.getComponentCount(); i++) {
            install(container.getComponent(i));
         }
      }
   }

   private void remove(Component component) {
      installedCount--;

      component.removeFocusListener(listener);
      component.removeKeyListener(listener);
      component.removeMouseListener(listener);
      component.removeMouseMotionListener(listener);
      // component.removeMouseWheelListener(listener);

      if (component instanceof Container container) {
         container.removeContainerListener(containerListener);
         for (int i = 0; i < container.getComponentCount(); i++) {
            remove(container.getComponent(i));
         }
      }
   }
}
