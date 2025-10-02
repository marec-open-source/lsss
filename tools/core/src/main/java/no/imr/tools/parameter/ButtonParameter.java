package no.imr.tools.parameter;

import no.imr.tools.listening.Listener;

/**
 * A "parameter" for performing an action.
 * A ButtonParameter will be represented as a clickable button in the gui.
 */
public final class ButtonParameter extends VoidParameter {
   public ButtonParameter(Name name, String description) {
      super(name, Unit.NONE, description);
   }

   public ButtonParameter(Name name, String description, Listener listener) {
      this(name, description);

      subscribe(listener);
   }
}
