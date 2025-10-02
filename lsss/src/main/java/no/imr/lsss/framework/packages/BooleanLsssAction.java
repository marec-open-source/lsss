package no.imr.lsss.framework.packages;

import no.imr.tools.swing.icons.MiscIcons;

public final class BooleanLsssAction extends LsssAction {
   private boolean value;

   public BooleanLsssAction(String id, String label, boolean initialValue) {
      super(id, label);

      value = initialValue;
      setIcon(MiscIcons.checkBox(value));
   }

   @Override
   public String toString() {
      return getId() + " = " + value;
   }

   public boolean get() {
      return value;
   }

   public void set(boolean value) {
      if (this.value == value) {
         return;
      }
      this.value = value;
      setIcon(MiscIcons.checkBox(value));
      getChangeManager().notifyListeners();
   }

   public void toggle() {
      set(!value);
   }

   @Override
   protected void doRun(ActionArgument argument) {
      toggle();
   }
}
