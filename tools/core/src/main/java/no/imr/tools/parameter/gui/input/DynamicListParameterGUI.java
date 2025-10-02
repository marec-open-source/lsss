package no.imr.tools.parameter.gui.input;

import no.imr.tools.ImmutableUtils;
import no.imr.tools.parameter.DynamicListParameter;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.swing.GridBag;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public final class DynamicListParameterGUI<V, P extends DynamicListParameter<V>> extends ParameterGUI<P> {
   private final List<ValueParameter<Optional<V>>> subParameters;
   private final List<ParameterGUI<?>> parameterGUIs;

   DynamicListParameterGUI(P parameter, GUIConfig guiConfig) {
      super(parameter, guiConfig);

      List<V> list = getParameter().getValue();
      subParameters = IntStream.rangeClosed(0, list.size())
            .mapToObj(i -> {
               String parameterName = getParameter().getName().persistentName();
               String subParameterName = i == 0 ? parameterName : parameterName + "_" + i;
               ValueParameter<Optional<V>> subParameter = getParameter().createNewParameter(i, subParameterName);
               if (i < list.size()) {
                  subParameter.setValue(Optional.of(list.get(i)));
               }
               subParameter.setEnabled(getParameter().isEnabled());
               subParameter.subscribe(value -> valueChanged(i, value.orElse(null)));
               return subParameter;
            })
            .toList();
      parameterGUIs = subParameters.stream()
            .<ParameterGUI<?>>map(subParameter -> ParameterGUIFactory.createParameterGUI(subParameter, getGUIConfig()))
            .toList();
   }

   @Override
   public void installGUI(GridBag gridBag) {
      for (ParameterGUI<?> parameterGUI : parameterGUIs) {
         parameterGUI.installGUI(gridBag);
      }
   }

   private void valueChanged(int index, @Nullable V value) {
      List<V> list = getParameter().getValue();
      if (value != null) {
         if (index < list.size()) {
            getParameter().setValue(ImmutableUtils.set(list, index, value));
         } else {
            getParameter().setValue(ImmutableUtils.add(list, value));
         }
      } else {
         if (index < list.size()) {
            getParameter().setValue(ImmutableUtils.remove(list, index));
         }
      }
   }

   @Override
   public void updateInput() {
      List<V> list = getParameter().getValue();
      if (list.size() + 1 == subParameters.size()) {
         for (int i = 0; i < subParameters.size(); i++) {
            ValueParameter<Optional<V>> subParameter = subParameters.get(i);
            subParameter.setEnabled(getParameter().isEnabled());
            subParameter.setValue(i < list.size() ? Optional.of(list.get(i)) : Optional.empty());
            parameterGUIs.get(i).updateInput();
         }
      } else {
         getGUIConfig().getDoRelayout().run();
      }
   }

   @Override
   public JComponent getInputComponent() {
      return ParameterGuiUtils.noInputComponent();
   }

   @Override
   public boolean commitEdit() {
      return parameterGUIs.stream().allMatch(ParameterGUI::commitEdit);
   }

   @Override
   public void setHighlight(@Nullable Color color) {
      parameterGUIs.forEach(parameterGUI -> parameterGUI.setHighlight(color));
   }
}
