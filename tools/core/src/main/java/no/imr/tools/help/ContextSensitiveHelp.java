package no.imr.tools.help;

import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Displays context-sensitive help.
 */
public final class ContextSensitiveHelp {
   private static final Object HELP_ID_KEY = new Object();
   private static final Object HELP_ID_PROVIDER_KEY = new Object();

   private ContextSensitiveHelp() {
   }

   public static void setHelpID(JComponent component, HelpID helpID) {
      component.putClientProperty(HELP_ID_KEY, helpID);
   }

   public static void setHelpIdProvider(JComponent component, HelpIDProvider helpIDProvider) {
      component.putClientProperty(HELP_ID_PROVIDER_KEY, helpIDProvider);
   }

   public static void run() {
      Cursor helpCursor = GuiUtils.createCursor("no/imr/tools/resources/images/cursors", "ContextSensitiveHelp", new Point(2, 1));
      Map<Component, Cursor> originalCursors = new HashMap<>();
      Arrays.stream(Window.getWindows())
            .flatMap(GuiUtils::hierarchyStream)
            .filter(Component::isCursorSet)
            .forEach(component -> {
               originalCursors.put(component, component.getCursor());
               component.setCursor(helpCursor);
            });

      try {
         MouseEvent mouseEvent = new MouseEventGetter().getNextMouseEvent();
         if (mouseEvent != null) {
            if (mouseEvent.getSource() instanceof Component component) {
               Component deepestComponent = SwingUtilities.getDeepestComponentAt(component, mouseEvent.getX(), mouseEvent.getY());
               HelpID helpID = getHelpID(deepestComponent, mouseEvent);
               if (helpID != null) {
                  helpID.show();
               }
            }
         }
      } finally {
         originalCursors.forEach(Component::setCursor);
      }
   }

   private static @Nullable HelpID getHelpID(@Nullable Component deepestComponent, MouseEvent mouseEvent) {
      for (Component component = deepestComponent; component != null; component = component.getParent()) {
         if (component instanceof JComponent jComponent) {

            if (jComponent.getClientProperty(HELP_ID_PROVIDER_KEY) instanceof HelpIDProvider helpIDProvider) {
               return helpIDProvider.getHelpID(mouseEvent);
            }

            if (jComponent.getClientProperty(HELP_ID_KEY) instanceof HelpID helpID) {
               return helpID;
            }
         }
      }
      return null;
   }

   private static final class MouseEventGetter extends EventQueue {
      private MouseEventGetter() {
      }

      private @Nullable MouseEvent getNextMouseEvent() {
         try {
            EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();

            for (int eventNumber = 0; true; eventNumber++) {
               AWTEvent event = eventQueue.getNextEvent();
               switch (event) {
                  case KeyEvent keyEvent -> {
                     keyEvent.consume();
                     if (keyEvent.getKeyCode() == KeyEvent.VK_CANCEL || keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        return null;
                     }
                  }
                  case MouseEvent mouseEvent -> {
                     int id = mouseEvent.getID();
                     if ((id == MouseEvent.MOUSE_CLICKED ||
                           id == MouseEvent.MOUSE_PRESSED ||
                           id == MouseEvent.MOUSE_RELEASED) &&
                           SwingUtilities.isLeftMouseButton(mouseEvent)) {
                        if (id == MouseEvent.MOUSE_CLICKED && eventNumber == 0) {
                           // This happens right after the user selects menu item for context help.
                           dispatchEvent(event);
                           continue;
                        }
                        mouseEvent.consume();
                        return mouseEvent;
                     } else {
                        mouseEvent.consume();
                     }
                  }
                  default -> {
                     dispatchEvent(event);
                  }
               }
            }
         } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return null;
         }
      }
   }

   @FunctionalInterface
   public interface HelpIDProvider {
      HelpID getHelpID(MouseEvent mouseEvent);
   }
}
