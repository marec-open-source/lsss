package no.imr.lsss.viewer;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.packages.ActionArgument;
import no.imr.lsss.framework.packages.LsssPackage;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.echogram.EchogramModule;

import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;

final class KeyStrokeDispatcher implements KeyEventPostProcessor {
   private final KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

   KeyStrokeDispatcher() {
   }

   void start() {
      keyboardFocusManager.addKeyEventPostProcessor(this);
   }

   void stop() {
      keyboardFocusManager.removeKeyEventPostProcessor(this);
   }

   @Override
   public boolean postProcessKeyEvent(KeyEvent e) {
      if (e.getID() != KeyEvent.KEY_PRESSED) {
         return false;
      }
      if (e.isConsumed()) {
         return false;
      }
      switch (e.getKeyCode()) {
         case KeyEvent.VK_ALT,
              KeyEvent.VK_ALT_GRAPH,
              KeyEvent.VK_CONTROL,
              KeyEvent.VK_META,
              KeyEvent.VK_SHIFT -> {
            return false;
         }
         default -> {
         }
      }
      KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
      Window focusedWindow = keyboardFocusManager.getFocusedWindow(); // Get focused window before dispatching any keystrokes.
      BaseViewModule module = BaseViewModule.moduleForComponent(keyboardFocusManager.getFocusOwner());
      if (module != null) {
         module.getLSSS().getPackageManager().dispatchKeyStroke(module.getPersistentName(), keyStroke, new ActionArgument(e));
         if (module instanceof EchogramModule) {
            module.getLSSS().getPackageManager().dispatchKeyStroke(LsssPackage.KEY_STROKE_CONTEXT_ANY_ECHOGRAM_MODULE, keyStroke, new ActionArgument(e));
         }
      }
      if (focusedWindow instanceof RootPaneContainer rootPaneContainer) {
         Object lsssProperty = rootPaneContainer.getRootPane().getClientProperty(MainDisplay.LSSS_KEY);
         if (lsssProperty instanceof LSSS lsss) {
            if (focusedWindow == lsss.getFrame()) {
               lsss.getPackageManager().dispatchKeyStroke(LsssPackage.KEY_STROKE_CONTEXT_MAIN_WINDOW, keyStroke, new ActionArgument(e));
            }
            lsss.getPackageManager().dispatchKeyStroke(LsssPackage.KEY_STROKE_CONTEXT_ANYWHERE, keyStroke, new ActionArgument(e));
         }
      }
      return false;
   }
}
