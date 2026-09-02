package no.imr.tools.parameter.gui;

import no.imr.tools.Utils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.MultiParameter;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.parameter.gui.input.ParameterGUI;
import no.imr.tools.parameter.gui.input.ParameterGUIFactory;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.WhenShowingListening;

import javax.swing.JComponent;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ParameterEditorData {
   private final Map<BaseParameter<?>, ParameterGUI<?>> parameterGUIs = new HashMap<>();
   private final ChangeManager parameterChangeManager = new ChangeManager();

   public ParameterEditorData(JComponent visibilityComponent, GUIConfig guiConfig, List<? extends BaseParameter<?>> parameters) {
      for (BaseParameter<?> parameter : parameters) {
         ParameterGUI<?> parameterGUI = ParameterGUIFactory.createParameterGUI(parameter, guiConfig, parameters);
         parameterGUIs.put(parameter, parameterGUI);
      }

      List<BaseParameter<?>> allParameters = parameters.stream()
            .flatMap(parameter -> Utils.recursiveStream(parameter, ParameterEditorData::subParameters))
            .toList();
      WhenShowingListening.connect(visibilityComponent, allParameters, GuiListeners.coalescingLater(() -> {
         update();
         parameterChangeManager.notifyListeners();
      }));

      update();
   }

   private static Collection<? extends BaseParameter<?>> subParameters(BaseParameter<?> parameter) {
      return parameter instanceof MultiParameter<?> multiParameter
            ? multiParameter.getParameters()
            : List.of();
   }

   public ChangeManager getParameterChangeManager() {
      return parameterChangeManager;
   }

   public Map<BaseParameter<?>, ParameterGUI<?>> getParameterGUIs() {
      return parameterGUIs;
   }

   public void update() {
      parameterGUIs.values().forEach(ParameterGUI::updateInput);
   }

   public JComponent getInputComponent(BaseParameter<?> parameter) {
      ParameterGUI<?> parameterGUI = parameterGUIs.get(parameter);
      JComponent inputComponent = parameterGUI != null ? parameterGUI.getInputComponent() : null;
      if (inputComponent == null) {
         throw new IllegalArgumentException("No GUI for parameter: " + parameter.getPersistentName());
      }
      return inputComponent;
   }
}
