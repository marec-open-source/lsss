package no.imr.tools.parameter.gui.input;

import com.google.common.html.HtmlEscapers;
import no.imr.tools.parameter.MultiParameter;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.WrappingFlowLayout;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MultiParameterGUI extends ParameterGUI<MultiParameter<?>> {
   private final Map<ValueParameter<?>, ParameterGUI<?>> parameterGUIs = new LinkedHashMap<>();
   private final JPanel subParameterPanel = new JPanel(new BorderLayout());

   MultiParameterGUI(MultiParameter<?> parameter, GUIConfig guiConfig) {
      super(parameter, guiConfig);

      // Nest in extra panel since wrapping uses the width of the parent component.
      JPanel wrappingPanel = new JPanel(new WrappingFlowLayout(FlowLayout.LEFT, 0, 0));
      subParameterPanel.add(wrappingPanel);

      for (ValueParameter<?> subParameter : getParameter().getParameters()) {
         ParameterGUI<?> subParameterGUI = ParameterGUIFactory.createParameterGUI(subParameter, getGUIConfig());
         parameterGUIs.put(subParameter, subParameterGUI);
         subParameterGUI.getNameLabel().setParentNameLabel(getNameLabel());
         subParameterGUI.setParentParameterGui(this);

         String displayName = subParameter.getDisplayName();
         if (!displayName.isEmpty()) {
            displayName = displayName.startsWith("<html>") ? displayName.substring(6) : HtmlEscapers.htmlEscaper().escape(displayName);
            displayName = "<html><span style='white-space: nowrap;'>" + displayName + ":</span>";
         }
         JLabel subName = subParameterGUI.getNameLabel();
         subName.setText(displayName);

         Box box = Box.createHorizontalBox();
         box.add(subName);
         box.add(Box.createHorizontalStrut(5));
         box.add(subParameterGUI.getInputComponent());
         box.add(Box.createHorizontalStrut(6));
         box.add(subParameterGUI.getUnitLabel());
         box.add(Box.createHorizontalStrut(15));
         wrappingPanel.add(box);
      }
   }

   @Override
   public void installGUI(GridBag gridBag) {
      addName(gridBag);

      gridBag.getConstraints().gridwidth = GridBagConstraints.REMAINDER;
      gridBag.getConstraints().anchor = GridBagConstraints.WEST;
      gridBag.activateHorizontalFill();
      gridBag.add(subParameterPanel);
   }

   public Map<ValueParameter<?>, ParameterGUI<?>> getParameterGUIs() {
      return parameterGUIs;
   }

   @Override
   public void updateInput() {
      parameterGUIs.values().forEach(ParameterGUI::updateInput);
      updateEnabledState(List.of());
      subParameterPanel.setVisible(getParameter().isVisible());
   }

   @Override
   public JComponent getInputComponent() {
      return ParameterGuiUtils.noInputComponent();
   }

   @Override
   public boolean commitEdit() {
      return parameterGUIs.values().stream().allMatch(ParameterGUI::commitEdit);
   }
}
