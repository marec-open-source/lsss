package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.PasswordParameter;
import no.imr.tools.swing.GridBag;

import javax.swing.JComponent;
import javax.swing.JPasswordField;
import java.awt.Dimension;

public final class PasswordParameterGUI extends ParameterGUI<PasswordParameter> {
   private final ParameterTextField parameterComponent;

   PasswordParameterGUI(PasswordParameter parameter, GUIConfig guiConfig) {
      super(parameter, guiConfig);

      parameterComponent = new ParameterTextField(parameter, guiConfig, new Workaround4238932PasswordField());
   }

   @Override
   public void installGUI(GridBag gridBag) {
      addName(gridBag);

      gridBag.getConstraints().anchor = getGUIConfig().getInputFieldAlignment().getGridBagConstraintsAnchor();

      addInputAndDescription(gridBag, parameterComponent.getComponent());
   }

   @Override
   public void updateInput() {
      parameterComponent.updateComponent();
      updateEnabledState(parameterComponent.getComponent());
   }

   @Override
   public JComponent getInputComponent() {
      return parameterComponent.getComponent();
   }

   /**
    * Workaround for <a href="https://bugs.openjdk.org/browse/JDK-4238932">JDK-4238932</a>.
    */
   private static final class Workaround4238932PasswordField extends JPasswordField {
      private Workaround4238932PasswordField() {
      }

      @Override
      public Dimension getMinimumSize() {
         return getPreferredSize();
      }
   }
}
