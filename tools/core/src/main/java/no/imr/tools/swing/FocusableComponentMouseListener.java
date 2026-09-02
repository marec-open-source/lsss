package no.imr.tools.swing;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A mouse listener setting a {@link Component} focusable and requesting focus
 * when the mouse is inside the component.
 */
public final class FocusableComponentMouseListener extends MouseAdapter {
   private final Component component;

   public FocusableComponentMouseListener(Component component) {
      this.component = component;
   }

   @Override
   public void mouseEntered(MouseEvent e) {
      component.setFocusable(true);
      component.requestFocusInWindow();
   }

   @Override
   public void mouseExited(MouseEvent e) {
      component.setFocusable(false);
   }
}
