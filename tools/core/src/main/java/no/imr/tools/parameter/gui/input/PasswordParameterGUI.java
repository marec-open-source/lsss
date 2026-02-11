package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.PasswordParameter;
import no.imr.tools.swing.GridBag;

import javax.swing.JComponent;
import javax.swing.JPasswordField;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public final class PasswordParameterGUI extends ParameterGUI<PasswordParameter> {
   private final JPasswordField passwordField = new Workaround4238932PasswordField();

   PasswordParameterGUI(PasswordParameter parameter, GUIConfig guiConfig) {
      super(parameter, guiConfig);

      passwordField.setColumns(guiConfig.getTextInputColumns());
      passwordField.setHorizontalAlignment(guiConfig.getTextAlignment().apply(parameter).getTextFieldHorizontalAlignment());
      passwordField.addFocusListener(new FocusAdapter() {
         @Override
         public void focusLost(FocusEvent e) {
            updateParameter();
         }
      });
      passwordField.addActionListener(_ -> updateParameter());
   }

   private void updateParameter() {
      getParameter().setValue(new String(passwordField.getPassword()));
   }

   @Override
   public void installGUI(GridBag gridBag) {
      addName(gridBag);

      addInputAndDescription(gridBag, passwordField);
   }

   @Override
   public void updateInput() {
      passwordField.setText(getParameter().getStringValue());
      updateEnabledState(passwordField);
   }

   @Override
   public JComponent getInputComponent() {
      return passwordField;
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
