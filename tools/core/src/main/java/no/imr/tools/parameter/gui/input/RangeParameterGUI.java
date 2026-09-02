package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.RangeParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GridBag;
import org.jspecify.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JComponent;
import java.util.List;

public final class RangeParameterGUI extends ParameterGUI<RangeParameter> {
   private final ValueParameter<Float> minParameter;
   private final ValueParameter<Float> maxParameter;
   private final NameLabel minLabel = new NameLabel("Min:");
   private final NameLabel maxLabel = new NameLabel("Max:");
   private final ParameterTextField minTextField;
   private final ParameterTextField maxTextField;

   RangeParameterGUI(RangeParameter parameter, GUIConfig guiConfig) {
      super(parameter, guiConfig);

      minParameter = new ValueParameter<>(new Name(""),
            parameter.getValue().min(), Unit.NONE, parameter.getMinMaxConstraint(), RangeParameter.MIN_CONVERTER);

      maxParameter = new ValueParameter<>(new Name(""),
            parameter.getValue().max(), Unit.NONE, parameter.getMinMaxConstraint(), RangeParameter.MAX_CONVERTER);

      minParameter.subscribe(parameter::setMin);
      maxParameter.subscribe(parameter::setMax);

      minTextField = new ParameterTextField(minParameter, guiConfig);
      maxTextField = new ParameterTextField(maxParameter, guiConfig);

      minTextField.getComponent().setColumns(6);
      maxTextField.getComponent().setColumns(6);

      minLabel.setParentNameLabel(getNameLabel());
      maxLabel.setParentNameLabel(getNameLabel());

      minLabel.addFocusListenerTo(minTextField.getComponent());
      maxLabel.addFocusListenerTo(maxTextField.getComponent());
   }

   @Override
   public void installGUI(GridBag gridBag) {
      addName(gridBag);

      GridBag box = new GridBag();
      box.add(minLabel);
      box.add(Box.createHorizontalStrut(5));
      box.add(minTextField.getComponent());
      box.add(Box.createHorizontalStrut(10));
      box.add(maxLabel);
      box.add(Box.createHorizontalStrut(5));
      box.add(maxTextField.getComponent());

      gridBag.getConstraints().anchor = getGUIConfig().getInputFieldAlignment().getGridBagConstraintsAnchor();
      addInputAndDescription(gridBag, box.getPanel());
   }

   @Override
   public void updateInput() {
      FloatRange range = getParameter().getValue();
      minParameter.setValue(range.min());
      maxParameter.setValue(range.max());
      minTextField.updateComponent();
      maxTextField.updateComponent();
      updateEnabledState(List.of(
            minLabel, minTextField.getComponent(),
            maxLabel, maxTextField.getComponent())
      );
   }

   @Override
   public @Nullable JComponent getInputComponent() {
      return null;
   }
}
