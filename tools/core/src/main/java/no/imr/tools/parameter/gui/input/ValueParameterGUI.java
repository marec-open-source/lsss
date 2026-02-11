package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.OptionalStringParameter;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.swing.GridBag;

import javax.swing.JComponent;

/**
 * Default GUI for a {@link ValueParameter}.
 */
public final class ValueParameterGUI<V, P extends ValueParameter<V>> extends ParameterGUI<P> {
   private final ParameterComponent parameterComponent;

   ValueParameterGUI(P parameter, GUIConfig guiConfig) {
      super(parameter, guiConfig);

      if (parameter.getAllowedValues() != null || !parameter.getSuggestedValues().isEmpty()) {
         parameterComponent = new ParameterComboBox<>(parameter);
      } else {
         parameterComponent = new ParameterTextField(parameter, guiConfig);
      }
   }

   @Override
   public void installGUI(GridBag gridBag) {
      addName(gridBag);

      gridBag.getConstraints().anchor = getGUIConfig().getInputFieldAlignment().getGridBagConstraintsAnchor();
      if (getGUIConfig().getHorizontalFill(getParameter())
            && (getParameter() instanceof StringParameter || getParameter() instanceof OptionalStringParameter)
            && (parameterComponent instanceof ParameterTextField || !getParameter().getSuggestedValues().isEmpty())) {
         gridBag.activateHorizontalFill();
      }

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

   @Override
   public boolean commitEdit() {
      return parameterComponent.commitEdit();
   }
}
