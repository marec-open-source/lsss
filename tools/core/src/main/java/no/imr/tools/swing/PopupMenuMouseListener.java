package no.imr.tools.swing;

import org.jspecify.annotations.Nullable;

import javax.swing.JPopupMenu;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Function;

/**
 * Mouse listener that displays a popup.
 */
public final class PopupMenuMouseListener extends MouseAdapter {
   private final Function<MouseEvent, @Nullable JPopupMenu> popupMenuSupplier;

   public PopupMenuMouseListener(Function<MouseEvent, @Nullable JPopupMenu> popupMenuSupplier) {
      this.popupMenuSupplier = popupMenuSupplier;
   }

   @Override
   public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
   }

   private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
         JPopupMenu popupMenu = popupMenuSupplier.apply(e);
         if (popupMenu != null) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
         }
      }
   }
}
