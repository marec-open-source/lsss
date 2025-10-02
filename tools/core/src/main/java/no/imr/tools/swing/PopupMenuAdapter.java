package no.imr.tools.swing;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * Abstract adapter class for PopupMenuListener.
 */
public abstract class PopupMenuAdapter implements PopupMenuListener {
   protected PopupMenuAdapter() {
   }

   @Override
   public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
   }

   @Override
   public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
   }

   @Override
   public void popupMenuCanceled(PopupMenuEvent e) {
   }
}
