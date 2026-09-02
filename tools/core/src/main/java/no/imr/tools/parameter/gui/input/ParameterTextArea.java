package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.ParameterException;
import no.imr.tools.parameter.TextParameter;
import no.imr.tools.swing.CurrentInputComponent;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.JDialog;
import javax.swing.JTextArea;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * A text field for editing a parameter.
 */
final class ParameterTextArea implements ParameterComponent {
   private final TextParameter parameter;
   private final JTextArea textArea = new JTextArea();
   private @Nullable JDialog errorDialog;

   ParameterTextArea(TextParameter parameter, GUIConfig guiConfig) {
      this.parameter = parameter;

      updateComponent();

      textArea.setRows(parameter.getProperty(BaseParameter.KEY_ROWS));
      if (parameter.getProperty(BaseParameter.KEY_TEXT_WRAP)) {
         textArea.setLineWrap(true);
         textArea.setWrapStyleWord(true);
      }
      textArea.addFocusListener(new FocusListener() {
         @Override
         public void focusGained(FocusEvent e) {
            textArea.getCaret().setVisible(guiConfig.isParameterEnabled(parameter));
            CurrentInputComponent.set(textArea, ParameterTextArea.this::updateParameter);
         }

         @Override
         public void focusLost(FocusEvent e) {
            textArea.getCaret().setVisible(false);
            updateParameter();
         }
      });
      textArea.addHierarchyListener(e -> {
         if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !textArea.isShowing()) {
            closeErrorDialog();
         }
      });
      textArea.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
               if (!parameter.getStringValue().equals(textArea.getText())) {
                  updateComponent();
                  e.consume();
               }
            }
         }
      });
      if (parameter.getProperty(BaseParameter.KEY_MONOSPACED)) {
         textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, textArea.getFont().getSize()));
      }
      GuiUtils.addUndoSupport(textArea);
   }

   @Override
   public JTextArea getComponent() {
      return textArea;
   }

   @Override
   public void updateComponent() {
      String stringValue = parameter.getStringValue();
      if (!stringValue.equals(textArea.getText())) {
         textArea.setText(stringValue);
         textArea.setCaretPosition(0);
      }
   }

   private boolean updateParameter() {
      if (errorDialog != null) {
         return false;
      }
      if (!textArea.isShowing()) {
         return false;
      }

      String newValue = textArea.getText();
      if (newValue.equals(parameter.getStringValue())) {
         return true;
      }

      try {
         parameter.setValue(newValue);
         updateComponent();
         return true;
      } catch (ParameterException e) {
         errorDialog = ParameterGuiUtils.createErrorDialog(e, textArea);
         errorDialog.setVisible(true);
         textArea.requestFocusInWindow();
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
}
