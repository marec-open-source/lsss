package no.imr.tools.swing;

import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * Abstract adapter class for receiving menu events.
 */
public abstract class MenuAdapter implements MenuListener {
   protected MenuAdapter() {
   }

   @Override
   public void menuSelected(MenuEvent e) {
   }

   @Override
   public void menuDeselected(MenuEvent e) {
   }

   @Override
   public void menuCanceled(MenuEvent e) {
   }
}
