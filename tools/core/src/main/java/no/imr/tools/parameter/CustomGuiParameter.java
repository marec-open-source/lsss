package no.imr.tools.parameter;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.util.function.Supplier;

/**
 * A "parameter" with a custom gui.
 */
public final class CustomGuiParameter extends VoidParameter {
   private Supplier<JComponent> componentSupplier = JLabel::new;

   public CustomGuiParameter(Name name) {
      super(name, Unit.NONE, "");
   }

   public void setComponentSupplier(Supplier<JComponent> componentSupplier) {
      this.componentSupplier = componentSupplier;
   }

   public Supplier<JComponent> getComponentSupplier() {
      return componentSupplier;
   }
}
