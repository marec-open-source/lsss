package no.marec.lsss.api.modules;

import org.jspecify.annotations.Nullable;

import javax.swing.JPopupMenu;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * An overlay on the echogram or the map.
 */
public interface LsssOverlay extends LsssModule {
   /**
    * Returns the data to be displayed by this overlay.
    *
    * @return the data to be displayed, or {@code null} if nothing to display
    */
   @Nullable LsssOverlayDisplayData computeDisplayData();

   /**
    * An optional tooltip specific for this overlay.
    *
    * @param point the point for the tooltip
    * @return a tooltip, or {@code null}
    */
   default @Nullable String getToolTipText(Point point) {
      return null;
   }

   /**
    * An optional popup menu specific for this overlay.
    *
    * @param point the point where the popup was triggered
    * @return the popup menu, or {@code null}
    */
   default @Nullable JPopupMenu getPopupMenu(Point point) {
      return null;
   }

   /**
    * Called when this overlay becomes the active overlay.
    * <p>
    * The active overlay receives mouse and key events.
    */
   default void onActivate() {
   }

   /**
    * Called when this overlay no longer is the active overlay.
    */
   default void onDeactivate() {
   }

   /**
    * Called when this overlay is active and a mouse button has been pressed.
    *
    * @param mouseEvent the event to be processed
    */
   default void mousePressed(MouseEvent mouseEvent) {
   }

   /**
    * Called when this overlay is active and a mouse button has been released.
    *
    * @param mouseEvent the event to be processed
    */
   default void mouseReleased(MouseEvent mouseEvent) {
   }

   /**
    * Called when this overlay is active and a mouse button has been clicked.
    *
    * @param mouseEvent the event to be processed
    */
   default void mouseClicked(MouseEvent mouseEvent) {
   }

   /**
    * Called when this overlay is active and the mouse enters the overlaid component.
    *
    * @param mouseEvent the event to be processed
    */

   default void mouseEntered(MouseEvent mouseEvent) {
   }

   /**
    * Called when this overlay is active and the mouse exits the overlaid component.
    *
    * @param mouseEvent the event to be processed
    */
   default void mouseExited(MouseEvent mouseEvent) {
   }

   /**
    * Called when this overlay is active and the mouse button has been dragged.
    *
    * @param mouseEvent the event to be processed
    */
   default void mouseDragged(MouseEvent mouseEvent) {
   }

   /**
    * Called when this overlay is active and the mouse button has been moved.
    *
    * @param mouseEvent the event to be processed
    */
   default void mouseMoved(MouseEvent mouseEvent) {
   }

   /**
    * Called when this overlay is active and a key has been typed.
    *
    * @param keyEvent the event to be processed
    * @return {@code true} is this overlay used the event
    */
   default boolean keyTyped(KeyEvent keyEvent) {
      return false;
   }

   /**
    * Called when this overlay is active and a key has been pressed.
    *
    * @param keyEvent the event to be processed
    * @return {@code true} is this overlay used the event
    */
   default boolean keyPressed(KeyEvent keyEvent) {
      return false;
   }

   /**
    * Called when this overlay is active and a key has been released.
    *
    * @param keyEvent the event to be processed
    * @return {@code true} is this overlay used the event
    */
   default boolean keyReleased(KeyEvent keyEvent) {
      return false;
   }
}
