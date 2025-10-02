package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.CustomGuiParameter;
import no.imr.tools.swing.GridBag;

import javax.swing.JComponent;

/**
 * GUI for a {@link CustomGuiParameter}.
 */
public final class CustomGuiParameterGUI extends ParameterGUI<CustomGuiParameter> {
   private final JComponent component;

   CustomGuiParameterGUI(CustomGuiParameter parameter, GUIConfig guiConfig) {
      super(parameter, guiConfig);

      component = parameter.getComponentSupplier().get();
   }

   @Override
   public void installGUI(GridBag gridBag) {
      addName(gridBag);
      gridBag.activateHorizontalFill();
      addInputAndDescription(gridBag, component);
   }

   @Override
   public void updateInput() {
      updateEnabledState(component);
   }

   @Override
   public JComponent getInputComponent() {
      return component;
   }
}
