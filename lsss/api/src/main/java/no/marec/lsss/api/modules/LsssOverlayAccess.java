package no.marec.lsss.api.modules;

import no.marec.lsss.api.DoNotImplement;
import no.marec.lsss.api.util.observing.ObservableValue;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Optional;

/**
 * Access to the actual overlay.
 */
@DoNotImplement
public interface LsssOverlayAccess extends LsssModuleAccess {
   /**
    * Triggers a repaint of the overlaid module.
    */
   void repaint();

   /**
    * Triggers a new computation of this overlay's {@link LsssOverlayDisplayData}.
    */
   void recompute();

   /**
    * {@return the width in pixels of the overlaid module}
    */
   int getWidth();

   /**
    * {@return the height in pixels of the overlaid module}
    */
   int getHeight();

   /**
    * {@return the bounds in pixels of the overlaid module}
    */
   Rectangle getBounds();

   /**
    * {@return an observable for the mouse position in the overlaid module}
    */
   ObservableValue<Optional<Point>> getMousePosition();

   /**
    * Sets the cursor to be displayed.
    *
    * @param cursor the cursor
    */
   void setCursor(Cursor cursor);
}
