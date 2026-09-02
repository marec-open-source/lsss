package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.swing.CurrentInputComponent;
import no.imr.tools.swing.GridBag;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

/**
 * GUI for a {@link BooleanParameter}.
 */
public final class BooleanParameterGUI extends ParameterGUI<BooleanParameter> {
   private final JCheckBox checkBox = new JCheckBox();

   BooleanParameterGUI(BooleanParameter parameter, GUIConfig guiConfig) {
      super(parameter, guiConfig);

      checkBox.addItemListener(_ -> {
         if (CurrentInputComponent.commitEdit()) {
            updateParameter();
         } else {
            updateInput();
         }
      });
      addMouseClickListener(() -> {
         if (CurrentInputComponent.commitEdit()) {
            checkBox.requestFocusInWindow();
            checkBox.doClick();
         }
      });
   }

   @Override
   public void installGUI(GridBag gridBag) {
      addName(gridBag);
      gridBag.getConstraints().anchor = getGUIConfig().getInputFieldAlignment().getGridBagConstraintsAnchor();
      addInputAndDescription(gridBag, checkBox);
   }

   private void updateParameter() {
      getParameter().setBooleanValue(checkBox.isSelected());
   }

   @Override
   public void updateInput() {
      checkBox.setSelected(getParameter().getBooleanValue());
      updateEnabledState(checkBox);
   }

   @Override
   public JComponent getInputComponent() {
      return checkBox;
   }
}
