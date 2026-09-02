package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.ParameterException;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.swing.ComboBoxListModel;
import no.imr.tools.swing.CurrentInputComponent;
import no.imr.tools.swing.PopupMenuAdapter;
import org.jspecify.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.Timer;
import javax.swing.event.PopupMenuEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * A combo box for editing a parameter.
 */
final class ParameterComboBox<V> implements ParameterComponent {
   private final ValueParameter<V> parameter;
   private final JComboBox<String> comboBox = new JComboBox<>();
   private @Nullable JDialog errorDialog;

   ParameterComboBox(ValueParameter<V> parameter) {
      this.parameter = parameter;

      updateComponent();

      Timer timer = new Timer(250, _ -> updateParameter());
      timer.setRepeats(false);
      comboBox.addItemListener(e -> {
         if (e.getStateChange() == ItemEvent.SELECTED) {
            timer.restart();
         }
      });
      comboBox.addPopupMenuListener(new PopupMenuAdapter() {
         @Override
         public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            updateParameter();
         }
      });
      comboBox.addFocusListener(new FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            CurrentInputComponent.set(comboBox, ParameterComboBox.this::updateParameter);
         }
      });
      comboBox.getEditor().getEditorComponent().addFocusListener(new FocusListener() {
         @Override
         public void focusGained(FocusEvent e) {
            CurrentInputComponent.set(comboBox, ParameterComboBox.this::updateParameter);
         }

         @Override
         public void focusLost(FocusEvent e) {
            updateParameter();
         }
      });
      comboBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
               case KeyEvent.VK_ESCAPE -> {
                  if (!parameter.getStringValue().equals(comboBox.getEditor().getItem())) {
                     updateComponent();
                     e.consume();
                  }
               }
               case KeyEvent.VK_ENTER -> {
                  if (e.getModifiersEx() == 0 && !parameter.getStringValue().equals(comboBox.getEditor().getItem())) {
                     updateParameter();
                     e.consume();
                  }
               }
               default -> {
               }
            }
         }
      });
      comboBox.addHierarchyListener(e -> {
         if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !comboBox.isShowing()) {
            closeErrorDialog();
         }
      });
   }

   @Override
   public JComponent getComponent() {
      return comboBox;
   }

   @Override
   public void updateComponent() {
      List<V> values = parameter.getAllowedValues();
      comboBox.setEditable(values == null);
      if (values == null) {
         values = parameter.getSuggestedValues();
      }
      List<String> stringValues = values.stream()
            .map(parameter::toValueString)
            .toList();
      comboBox.setModel(new ComboBoxListModel<>(parameter.getStringValue(), stringValues, false));
      comboBox.setRenderer(new ParameterListCellRenderer<>(parameter, values, stringValues));
   }

   private boolean updateParameter() {
      if (errorDialog != null) {
         return false;
      }
      if (!comboBox.isShowing()) {
         return false;
      }

      Object newValue;
      if (comboBox.isEditable()) {
         newValue = comboBox.getEditor().getItem();
      } else {
         newValue = comboBox.getSelectedItem();
      }
      if (newValue == null) {
         return true;
      }

      try {
         parameter.setStringValue((String) newValue);
         updateComponent();
         return true;
      } catch (ParameterException e) {
         errorDialog = ParameterGuiUtils.createErrorDialog(e, comboBox);
         errorDialog.setVisible(true);
         updateComponent();
         comboBox.requestFocusInWindow();
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
