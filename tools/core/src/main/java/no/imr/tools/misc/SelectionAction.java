package no.imr.tools.misc;

import java.awt.event.InputEvent;

public enum SelectionAction {
   ADD, REPLACE, TOGGLE;

   public static SelectionAction fromModifiersEx(int modifiersEx) {
      if ((modifiersEx & InputEvent.CTRL_DOWN_MASK) != 0) {
         return TOGGLE;
      }
      if ((modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0) {
         return ADD;
      }
      return REPLACE;
   }
}
