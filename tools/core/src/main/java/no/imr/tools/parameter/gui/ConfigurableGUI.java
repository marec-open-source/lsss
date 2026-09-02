package no.imr.tools.parameter.gui;

import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.VerticalScrollablePanel;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public final class ConfigurableGUI {
   private Consumer<GUIConfig> parameterGUIConfigAdaptor = _ -> {
   };

   public ConfigurableGUI() {
   }

   public ConfigurableGUI setParameterGUIConfigAdaptor(Consumer<GUIConfig> parameterGUIConfigAdaptor) {
      this.parameterGUIConfigAdaptor = parameterGUIConfigAdaptor;
      return this;
   }

   private JComponent createGUI(Collection<? extends Configurable> configurables) {
      GridBag gridBag = new GridBag()
            .configureVerticalBox();

      List<BaseParameter<?>> parameters = new ArrayList<>();
      for (Configurable configurable : configurables) {
         if (configurable instanceof BaseParameter<?> parameter) {
            parameters.add(parameter);
         }
      }
      addParameterGUI(gridBag, parameters);
      for (Configurable configurable : configurables) {
         switch (configurable) {
            case ParameterCollection parameterCollection -> {
               addParameterGUI(gridBag, parameterCollection.getParameters());
            }
            case BaseParameter<?> _ -> {
               // Already taken care of.
            }
            default -> {
               JComponent component = createGUI(configurable.getSubConfigurables());
               if (component.getComponentCount() > 0) {
                  component.setBorder(BorderFactory.createTitledBorder(configurable.getName().displayName()));
                  gridBag.add(component);
               }
            }
         }
      }

      if (gridBag.getPanel().getComponentCount() > 0) {
         gridBag.addVerticalFiller();
      }

      return gridBag.getPanel();
   }

   private void addParameterGUI(GridBag gridBag, List<? extends BaseParameter<?>> parameters) {
      if (!parameters.isEmpty()) {
         gridBag.add(createParameterGUI(parameters));
      }
   }

   private JComponent createParameterGUI(List<? extends BaseParameter<?>> parameters) {
      GUIConfig guiConfig = new GUIConfig();
      parameterGUIConfigAdaptor.accept(guiConfig);
      ParameterEditor parameterEditor = new ParameterEditor(parameters, guiConfig);
      return parameterEditor.getEditorComponent();
   }

   public JComponent createComponent(Configurable configurable) {
      return createComponent(List.of(configurable));
   }

   public JComponent createComponent(Collection<? extends Configurable> configurables) {
      return VerticalScrollablePanel.wrap(createGUI(configurables));
   }
}
