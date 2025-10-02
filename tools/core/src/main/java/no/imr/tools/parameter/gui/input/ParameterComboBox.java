package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.ParameterException;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.swing.ComboBoxListModel;
import no.imr.tools.swing.PopupMenuAdapter;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.event.PopupMenuEvent;
import java.awt.event.ItemEvent;
import java.util.List;

/**
 * A combo box for editing a parameter.
 */
final class ParameterComboBox<V> extends ParameterComponent {
   private final ValueParameter<V> parameter;
   private final JComboBox<String> comboBox = new JComboBox<>();
   private boolean dialogShowing;

   ParameterComboBox(ValueParameter<V> parameter) {
      this.parameter = parameter;

      updateComponent();

      Timer timer = new Timer(250, e -> updateParameter());
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
   }

   @Override
   JComponent getComponent() {
      return comboBox;
   }

   @Override
   void updateComponent() {
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
      if (dialogShowing) {
         return false;
      }

      if (comboBox.getModel().getSize() == 0 && !comboBox.isEditable()) {
         return true;
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

      dialogShowing = true;
      try {
         parameter.setStringValue((String) newValue);
         updateComponent();
         return true;
      } catch (ParameterException e) {
         ParameterGuiUtils.showErrorDialog(e, comboBox);
         updateComponent();
         comboBox.requestFocusInWindow();
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
