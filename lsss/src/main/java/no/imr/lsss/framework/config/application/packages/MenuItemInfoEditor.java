package no.imr.lsss.framework.config.application.packages;

import no.imr.lsss.framework.config.application.packages.pojo.MenuItemInfo;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import javax.swing.JOptionPane;
import java.util.List;

final class MenuItemInfoEditor implements ParameterContainer {
   private final StringParameter filter = new StringParameter(new Name("Filter"),
         "",
         "Filters list of actions");

   private final ActionParameter action = new ActionParameter();

   private final StringParameter text = new StringParameter(new Name("Text"),
         "",
         "Leave blank to use action label");

   private final StringParameter mnemonic = new StringParameter(new Name("Mnemonic"),
         "", ValueConstraints.maxLength(1));

   private final UserDefinedPackage userDefinedPackage;
   private final MenuItemInfo menuItemInfo;
   private final boolean isMenu;

   MenuItemInfoEditor(UserDefinedPackage userDefinedPackage, MenuItemInfo menuItemInfo, boolean isMenu) {
      this.userDefinedPackage = userDefinedPackage;
      this.menuItemInfo = menuItemInfo;
      this.isMenu = isMenu;

      text.setValue(menuItemInfo.text);
      mnemonic.setValue(menuItemInfo.mnemonic != null ? menuItemInfo.mnemonic.toString() : "");

      if (isMenu) {
         filter.setVisible(false);
         action.setVisible(false);
         text.setDescription("");
      }
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            filter,
            action,
            text,
            mnemonic
      );
   }

   void init(ParameterEditor parameterEditor) {
      action.init(parameterEditor, filter, userDefinedPackage, menuItemInfo);
   }

   boolean isOK(ParameterEditor parameterEditor) {
      if (!parameterEditor.commitEdits()) {
         return false;
      }
      if (isMenu) {
         if (text.getValue().isEmpty()) {
            JOptionPane.showMessageDialog(parameterEditor.getEditorComponent(), "Please enter a menu text", "Error", JOptionPane.ERROR_MESSAGE);
            parameterEditor.getInputComponent(text).requestFocusInWindow();
            return false;
         }
      } else {
         if (action.getValue().isEmpty()) {
            JOptionPane.showMessageDialog(parameterEditor.getEditorComponent(), "Please select an action", "Error", JOptionPane.ERROR_MESSAGE);
            parameterEditor.getInputComponent(action).requestFocusInWindow();
            return false;
         }
      }
      return true;
   }

   void apply() {
      action.getValue().ifPresent(lsssAction -> {
         String packageId = lsssAction.getLsssPackage().getId();
         menuItemInfo.packageId = packageId.equals(userDefinedPackage.id) ? "" : packageId;
         menuItemInfo.actionId = lsssAction.getId();
      });
      menuItemInfo.text = text.getValue();
      String mnemonicValue = mnemonic.getValue();
      menuItemInfo.mnemonic = mnemonicValue.isEmpty() ? null : mnemonicValue.charAt(0);
   }
}
