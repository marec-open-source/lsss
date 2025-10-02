package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.swing.GridBag;

import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.GridBagConstraints;

/**
 * GUI for a {@link ButtonParameter}.
 */
public final class ButtonParameterGUI extends ParameterGUI<ButtonParameter> {
   private final JButton button;

   ButtonParameterGUI(ButtonParameter parameter, GUIConfig guiConfig) {
      super(parameter, guiConfig);

      button = new JButton(parameter.getDisplayName());
      button.addActionListener(e -> {
         getParameter().notifyListeners();
      });
   }

   @Override
   public void installGUI(GridBag gridBag) {
      addName(gridBag);
      gridBag.getConstraints().anchor = GridBagConstraints.EAST;
      addInputAndDescription(gridBag, button);
   }

   @Override
   public void updateInput() {
      updateEnabledState(button);
   }

   @Override
   public JComponent getInputComponent() {
      return button;
   }
}
