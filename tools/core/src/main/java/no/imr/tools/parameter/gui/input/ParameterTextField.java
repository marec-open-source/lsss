package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.ParameterException;
import no.imr.tools.parameter.ValueParameter;

import javax.swing.JFormattedTextField;
import javax.swing.text.DefaultFormatter;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Objects;

/**
 * A text field for editing a parameter.
 */
final class ParameterTextField extends ParameterComponent {
   private final ValueParameter<?> parameter;
   private final JFormattedTextField textField = new Workaround4238932FormattedTextField("");
   private boolean dialogShowing;

   ParameterTextField(ValueParameter<?> parameter, GUIConfig guiConfig) {
      this.parameter = parameter;

      updateComponent();

      ((DefaultFormatter) textField.getFormatter()).setOverwriteMode(false);

      textField.setColumns(guiConfig.getTextInputColumns());
      textField.setHorizontalAlignment(guiConfig.getTextAlignment().apply(parameter).getTextFieldHorizontalAlignment());
      textField.addActionListener(_ -> updateParameter());
      textField.addFocusListener(new FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            textField.getCaret().setVisible(guiConfig.isParameterEnabled(parameter));
         }

         @Override
         public void focusLost(FocusEvent e) {
            textField.getCaret().setVisible(false);
            updateParameter();
         }
      });
   }

   @Override
   JFormattedTextField getComponent() {
      return textField;
   }

   @Override
   void updateComponent() {
      String stringValue = parameter.getStringValue();
      if (!Objects.equals(textField.getText(), stringValue)) {
         textField.setValue(stringValue);
      }
   }

   private boolean updateParameter() {
      if (dialogShowing) {
         return false;
      }

      String newValue = textField.getText();
      if (newValue.equals(parameter.getStringValue())) {
         return true;
      }

      dialogShowing = true;
      try {
         parameter.setStringValue(newValue);
         updateComponent();
         return true;
      } catch (ParameterException e) {
         ParameterGuiUtils.showErrorDialog(e, textField);
         updateComponent();
         textField.requestFocusInWindow();
         return false;
      } finally {
         dialogShowing = false;
      }
   }

   @Override
   boolean commitEdit() {
      return updateParameter();
   }

   /**
    * Workaround for <a href="https://bugs.openjdk.org/browse/JDK-4238932">JDK-4238932</a>.
    */
   private static final class Workaround4238932FormattedTextField extends JFormattedTextField {
      private Workaround4238932FormattedTextField(String value) {
         super(value);
      }

      @Override
      public Dimension getMinimumSize() {
         return getPreferredSize();
      }
   }
}
