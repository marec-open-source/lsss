package no.imr.tools.swing;

import javax.swing.ToolTipManager;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * Represents a ToolTipManager state.
 */
public record ToolTipManagerState(
      boolean enabled,
      int initialDelay,
      int dismissDelay
) {
   public static final ToolTipManagerState DEFAULT = new ToolTipManagerState(true, 750, 4000);
   public static final ToolTipManagerState ALWAYS_ON = new ToolTipManagerState(true, 0, Integer.MAX_VALUE);
   public static final ToolTipManagerState ALWAYS_OFF = new ToolTipManagerState(false, Integer.MAX_VALUE, 0);

   /**
    * Applies this state to the shared ToolTipManager instance.
    *
    * @see ToolTipManager#sharedInstance()
    */
   public void apply() {
      ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
      toolTipManager.setEnabled(enabled);
      toolTipManager.setInitialDelay(initialDelay);
      toolTipManager.setDismissDelay(dismissDelay);
   }

   public static void updateToolTip(Component component, Point mousePosition) {
      ToolTipManager.sharedInstance().mouseMoved(new MouseEvent(component, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, mousePosition.x, mousePosition.y, 0, false));
   }
}
