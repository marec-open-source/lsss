package no.imr.lsss.framework.config.application.packages;

import no.imr.lsss.framework.config.application.packages.pojo.CallbackInfo;
import no.imr.lsss.framework.packages.LsssAction;
import no.imr.lsss.framework.packages.LsssCallbackEvent;
import no.imr.tools.Utils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.gui.ParameterEditor;

import javax.swing.JOptionPane;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

final class CallbackInfoEditor implements ParameterContainer {
   private final StringParameter filter = new StringParameter(new Name("Filter"),
         "",
         "Filters list of actions");

   private final ActionParameter action;

   private final ObjectParameter<Optional<LsssCallbackEvent>> event = new ObjectParameter<>(new Name("Event"),
         Optional.empty()) {
      @Override
      public String toString(Optional<LsssCallbackEvent> value) {
         return value.map(LsssCallbackEvent::name).orElse("");
      }
   };

   private final UserDefinedPackage userDefinedPackage;
   private final CallbackInfo callbackInfo;

   CallbackInfoEditor(UserDefinedPackage userDefinedPackage, CallbackInfo callbackInfo) {
      this.userDefinedPackage = userDefinedPackage;
      this.callbackInfo = callbackInfo;

      action = new ActionParameter(userDefinedPackage, callbackInfo);

      List<Optional<LsssCallbackEvent>> events = Utils.toOptionals(Arrays.asList(LsssCallbackEvent.values()));
      try {
         event.setAllowedValuesAndValue(events, Optional.of(LsssCallbackEvent.valueOf(callbackInfo.event)));
      } catch (IllegalArgumentException _) {
         events = Utils.toList(List.of(Optional.empty()), events);
         event.setAllowedValuesAndValue(events, Optional.empty());
      }
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            filter,
            action,
            event
      );
   }

   void init(ParameterEditor parameterEditor) {
      action.init(parameterEditor, filter);
   }

   boolean isOK(ParameterEditor parameterEditor) {
      if (action.getValue().isEmpty()) {
         JOptionPane.showMessageDialog(parameterEditor.getEditorComponent(), "Please select an action", "Error", JOptionPane.ERROR_MESSAGE);
         parameterEditor.getInputComponent(action).requestFocusInWindow();
         return false;
      }
      if (event.getValue().isEmpty()) {
         JOptionPane.showMessageDialog(parameterEditor.getEditorComponent(), "Please select an event", "Error", JOptionPane.ERROR_MESSAGE);
         parameterEditor.getInputComponent(event).requestFocusInWindow();
         return false;
      }
      return true;
   }

   void apply() {
      LsssAction lsssAction = action.getValue().orElseThrow();
      String packageId = lsssAction.getLsssPackage().getId();
      callbackInfo.packageId = packageId.equals(userDefinedPackage.id) ? "" : packageId;
      callbackInfo.actionId = lsssAction.getId();
      callbackInfo.event = event.getValue().orElseThrow().name();
   }
}
