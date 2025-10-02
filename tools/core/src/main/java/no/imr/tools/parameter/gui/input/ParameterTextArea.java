package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.ParameterException;
import no.imr.tools.parameter.TextParameter;
import no.imr.tools.swing.GuiUtils;

import javax.swing.JTextArea;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Objects;

/**
 * A text field for editing a parameter.
 */
final class ParameterTextArea extends ParameterComponent {
   private final TextParameter parameter;
   private final JTextArea textArea = new JTextArea();
   private boolean dialogShowing;

   ParameterTextArea(TextParameter parameter, GUIConfig guiConfig) {
      this.parameter = parameter;

      updateComponent();

      textArea.setRows(parameter.getProperty(BaseParameter.KEY_ROWS));
      textArea.addFocusListener(new FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            textArea.getCaret().setVisible(guiConfig.isParameterEnabled(parameter));
         }

         @Override
         public void focusLost(FocusEvent e) {
            textArea.getCaret().setVisible(false);
            updateParameter();
         }
      });
      if (parameter.getProperty(BaseParameter.KEY_MONOSPACED)) {
         textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
      }
      GuiUtils.addUndoSupport(textArea);
   }

   @Override
   JTextArea getComponent() {
      return textArea;
   }

   @Override
   void updateComponent() {
      String stringValue = parameter.getStringValue();
      if (!Objects.equals(textArea.getText(), stringValue)) {
         textArea.setText(stringValue);
         textArea.setCaretPosition(0);
      }
   }

   private boolean updateParameter() {
      if (dialogShowing) {
         return false;
      }

      String newValue = textArea.getText();
      if (newValue.equals(parameter.getStringValue())) {
         return true;
      }

      dialogShowing = true;
      try {
         parameter.setValue(newValue);
         return true;
      } catch (ParameterException e) {
         ParameterGuiUtils.showErrorDialog(e, textArea);
         textArea.requestFocusInWindow();
         return false;
      } finally {
         dialogShowing = false;
      }
   }

   @Override
   boolean commitEdit() {
      return updateParameter();
   }
}
