package no.imr.tools.swing;

import javax.swing.JComponent;
import java.util.function.BooleanSupplier;

public final class CurrentInputComponent {
   private static BooleanSupplier commitEdit = () -> true;

   private CurrentInputComponent() {
   }

   public static void set(JComponent component, BooleanSupplier inputCommitter) {
      commitEdit = () -> !component.isShowing() || inputCommitter.getAsBoolean();
   }

   public static boolean commitEdit() {
      return commitEdit.getAsBoolean();
   }
}
