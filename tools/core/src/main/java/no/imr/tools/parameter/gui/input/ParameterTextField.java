package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.ParameterException;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.swing.CurrentInputComponent;
import org.jspecify.annotations.Nullable;

import javax.swing.JDialog;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * A text field for editing a parameter.
 */
final class ParameterTextField implements ParameterComponent {
   private final ValueParameter<?> parameter;
   private final JTextField textField;
   private @Nullable JDialog errorDialog;

   ParameterTextField(ValueParameter<?> parameter, GUIConfig guiConfig) {
      this(parameter, guiConfig, new Workaround4238932TextField());
   }

   ParameterTextField(ValueParameter<?> parameter, GUIConfig guiConfig, JTextField textField) {
      this.parameter = parameter;
      this.textField = textField;

      updateComponent();

      textField.setColumns(guiConfig.getTextInputColumns());
      textField.setHorizontalAlignment(guiConfig.getTextAlignment().apply(parameter).getTextFieldHorizontalAlignment());
      textField.addActionListener(_ -> updateParameter());
      textField.addFocusListener(new FocusListener() {
         @Override
         public void focusGained(FocusEvent e) {
            textField.getCaret().setVisible(guiConfig.isParameterEnabled(parameter));
            CurrentInputComponent.set(textField, ParameterTextField.this::updateParameter);
         }

         @Override
         public void focusLost(FocusEvent e) {
            textField.getCaret().setVisible(false);
            updateParameter();
         }
      });
      textField.addHierarchyListener(e -> {
         if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !textField.isShowing()) {
            closeErrorDialog();
         }
      });
      textField.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
               if (!parameter.getStringValue().equals(textField.getText())) {
                  updateComponent();
                  e.consume();
               }
            }
         }
      });
   }

   @Override
   public JTextField getComponent() {
      return textField;
   }

   @Override
   public void updateComponent() {
      String stringValue = parameter.getStringValue();
      if (!stringValue.equals(textField.getText())) {
         textField.setText(stringValue);
      }
   }

   private boolean updateParameter() {
      if (errorDialog != null) {
         return false;
      }
      if (!textField.isShowing()) {
         return false;
      }

      String newValue = textField.getText();
      if (newValue.equals(parameter.getStringValue())) {
         return true;
      }

      try {
         parameter.setStringValue(newValue);
         updateComponent();
         return true;
      } catch (ParameterException e) {
         errorDialog = ParameterGuiUtils.createErrorDialog(e, textField);
         errorDialog.setVisible(true);
         updateComponent();
         textField.requestFocusInWindow();
         return false;
      } finally {
         closeErrorDialog();
      }
   }

   private void closeErrorDialog() {
      if (errorDialog != null) {
         errorDialog.dispose();
         errorDialog = null;
      }
   }

   /**
    * Workaround for <a href="https://bugs.openjdk.org/browse/JDK-4238932">JDK-4238932</a>.
    */
   private static final class Workaround4238932TextField extends JTextField {
      private Workaround4238932TextField() {
      }

      @Override
      public Dimension getMinimumSize() {
         return getPreferredSize();
      }
   }
}
