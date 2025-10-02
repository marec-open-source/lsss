package no.imr.lsss.viewer;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

/**
 * Keyboard shortcuts.
 */
public final class Shortcuts {
   public static final KeyStroke ACTION_DIALOG = KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);

   public static final KeyStroke NEW = KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK);
   public static final KeyStroke OPEN = KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK);
   public static final KeyStroke EDIT = KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK);
   public static final KeyStroke SAVE = KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK);

   public static final KeyStroke PREVIOUS_SEGMENT = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK);
   public static final KeyStroke NEXT_SEGMENT = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK);

   public static final KeyStroke PREVIOUS_RANGE = KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.SHIFT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK);
   public static final KeyStroke NEXT_RANGE = KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK);

   public static final KeyStroke GO_BACK = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK);
   public static final KeyStroke GO_FORWARD = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK);

   public static final KeyStroke UNDO = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK);
   public static final KeyStroke REDO = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);

   public static final KeyStroke TOOLTIP = KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK);

   public static final KeyStroke STORED_MASKING = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
   public static final KeyStroke CATEGORIZATION = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
   public static final KeyStroke PLANKTON = KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0);
   public static final KeyStroke CONDITIONAL_MASKING = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
   public static final KeyStroke ONLY_ECHOGRAM = KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0);
   public static final KeyStroke HELP = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
   public static final KeyStroke ESCAPE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

   private Shortcuts() {
   }
}
