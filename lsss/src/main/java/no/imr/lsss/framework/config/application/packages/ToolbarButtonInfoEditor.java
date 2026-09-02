package no.imr.lsss.framework.config.application.packages;

import no.imr.lsss.framework.config.application.packages.pojo.ToolbarButtonInfo;
import no.imr.lsss.framework.packages.LsssAction;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.gui.ParameterEditor;

import javax.swing.JOptionPane;
import java.util.List;

final class ToolbarButtonInfoEditor implements ParameterContainer {
   private final StringParameter filter = new StringParameter(new Name("Filter"),
         "",
         "Filters list of actions");

   private final ActionParameter action;

   private final StringParameter text = new StringParameter(new Name("Text"),
         "",
         "Leave blank to use action label");

   private final UserDefinedPackage userDefinedPackage;
   private final ToolbarButtonInfo toolbarButtonInfo;

   ToolbarButtonInfoEditor(UserDefinedPackage userDefinedPackage, ToolbarButtonInfo toolbarButtonInfo) {
      this.userDefinedPackage = userDefinedPackage;
      this.toolbarButtonInfo = toolbarButtonInfo;

      action = new ActionParameter(userDefinedPackage, toolbarButtonInfo);

      text.setValue(toolbarButtonInfo.text);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            filter,
            action,
            text
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
      return true;
   }

   void apply() {
      LsssAction lsssAction = action.getValue().orElseThrow();
      String packageId = lsssAction.getLsssPackage().getId();
      toolbarButtonInfo.packageId = packageId.equals(userDefinedPackage.id) ? "" : packageId;
      toolbarButtonInfo.actionId = lsssAction.getId();
      toolbarButtonInfo.text = text.getValue();
   }
}
