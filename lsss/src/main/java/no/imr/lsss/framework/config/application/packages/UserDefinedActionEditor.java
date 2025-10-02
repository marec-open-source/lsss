package no.imr.lsss.framework.config.application.packages;

import no.imr.lsss.framework.config.application.packages.pojo.UserDefinedInputParameter;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.CustomGuiParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.TextParameter;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.gui.ParameterTableGUI;
import no.imr.tools.parameter.gui.ParameterTableModel;
import org.jspecify.annotations.Nullable;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

final class UserDefinedActionEditor implements ParameterContainer {
   final StringParameter id = new StringParameter(new Name("Id"),
         "", this::validateId);

   private final StringParameter label = new StringParameter(new Name("Label"));

   private final TextParameter tooltip = new TextParameter(new Name("Tooltip"));

   private final CustomGuiParameter icon = new CustomGuiParameter(new Name("Icon"));

   private final CustomGuiParameter inputs = new CustomGuiParameter(new Name("Inputs"));

   final BooleanParameter showDialog = new BooleanParameter(
         new Name("ShowDialog", "<html>Run in the<br>foreground"),
         true,
         "If checked, then running the action waits until completion and shows a dialog if it takes a long time");

   final TextParameter code = new TextParameter(new Name("main.py"));

   private final UserDefinedAction userDefinedAction;
   private final IconSelector iconSelector;
   private final ParameterTableGUI<InputEditor> inputTableGUI;

   UserDefinedActionEditor(UserDefinedAction userDefinedAction) {
      this.userDefinedAction = userDefinedAction;

      tooltip.setProperty(BaseParameter.KEY_ROWS, 3);

      iconSelector = new IconSelector(userDefinedAction.getDir(), userDefinedAction.info.icon);

      icon.setComponentSupplier(iconSelector::getComponent);

      List<InputEditor> inputRows = userDefinedAction.info.inputParameters.stream()
            .map(InputEditor::new)
            .collect(Collectors.toCollection(ArrayList::new));
      inputTableGUI = new ParameterTableGUI<>(new ParameterTableModel<>(InputEditor::new, inputRows));
      inputs.setComponentSupplier(inputTableGUI::createPanel);

      code.setProperty(BaseParameter.KEY_VERTICAL_FILL, true);
      code.setProperty(BaseParameter.KEY_MONOSPACED, true);

      id.setValue(userDefinedAction.id);
      label.setValue(userDefinedAction.info.label);
      tooltip.setValue(userDefinedAction.info.tooltip);
      showDialog.setValue(userDefinedAction.info.showDialog);
      if (userDefinedAction.id.isEmpty()) {
         code.setValue(PackagesConf.generatedHeader());
      } else {
         Path file = userDefinedAction.getMainFile();
         try {
            code.setValue(Files.readString(file, Utils.UTF_8));
         } catch (IOException e) {
            if (Files.exists(file)) {
               Log.global.log(Level.WARNING, "Error reading " + file, e);
            }
         }
      }
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            id,
            label,
            tooltip,
            icon,
            inputs,
            showDialog,
            code
      );
   }

   void init(ParameterEditor parameterEditor) {
      JTextArea codeComponent = (JTextArea) parameterEditor.getInputComponent(code);
      codeComponent.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_TAB && e.getModifiersEx() == 0) {
               e.consume();
               codeComponent.insert("    ", codeComponent.getCaretPosition());
            }
         }
      });
   }

   private @Nullable String validateId(String newId) {
      if (newId.isEmpty()) {
         // Empty id must be allowed when creating an action.
         // Will be tested on ok in edit dialog.
         return null;
      }
      if (newId.equals(userDefinedAction.id)) {
         return null;
      }
      if (userDefinedAction.getUserDefinedPackage().getActions().stream().anyMatch(action -> action.id.equals(newId))) {
         return "An action already exists with that ID";
      }
      return null;
   }

   List<UserDefinedInputParameter> getInputParameters() {
      inputTableGUI.stopEditing();
      return inputTableGUI.getModel().getRows().stream()
            .map(InputEditor::toInputParameter)
            .toList();
   }

   boolean isOK(ParameterEditor parameterEditor) {
      if (!parameterEditor.commitEdits()) {
         return false;
      }
      if (id.getValue().isEmpty()) {
         JOptionPane.showMessageDialog(parameterEditor.getEditorComponent(), "ID cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
         parameterEditor.getInputComponent(id).requestFocusInWindow();
         return false;
      }
      return inputTableGUI.stopEditing();
   }

   void apply() {
      Path actionsDir = userDefinedAction.getUserDefinedPackage().getActionsDir();
      if (!userDefinedAction.id.equals(id.getValue()) && !userDefinedAction.id.isEmpty()) {
         try {
            Files.move(actionsDir.resolve(userDefinedAction.id), actionsDir.resolve(id.getValue()));
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error renaming action directory", e);
         }
      }

      userDefinedAction.id = id.getValue();
      userDefinedAction.info.label = label.getValue();
      userDefinedAction.info.tooltip = tooltip.getValue();
      userDefinedAction.info.icon = iconSelector.getPath();
      userDefinedAction.info.showDialog = showDialog.getBooleanValue();
      userDefinedAction.info.inputParameters = getInputParameters();

      Path mainPy = userDefinedAction.getDir().resolve(UserDefinedAction.MAIN_PY);
      try {
         FileUtils.replaceFileSafely(mainPy, code.getValue(), Utils.UTF_8);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error saving " + mainPy, e);
      }

      userDefinedAction.saveActionInfo();
   }

   public static final class InputEditor implements ParameterContainer {
      public final StringParameter name = new StringParameter(
            new Name("ParameterName", "Parameter name"));

      public final ObjectParameter<String> type = new ObjectParameter<>(new Name("Type"),
            UserDefinedUtils.INPUT_FLOAT, UserDefinedUtils.INPUT_TYPES);

      public final StringParameter unit = new StringParameter(new Name("Unit"));

      public final StringParameter description = new StringParameter(new Name("Description"));

      private InputEditor() {
      }

      private InputEditor(UserDefinedInputParameter inputParameter) {
         name.setValue(inputParameter.name);
         type.setValue(inputParameter.type);
         unit.setValue(inputParameter.unit);
         description.setValue(inputParameter.description);
      }

      @Override
      public List<? extends BaseParameter<?>> getParameters() {
         return List.of(
               name,
               type,
               unit,
               description
         );
      }

      private UserDefinedInputParameter toInputParameter() {
         UserDefinedInputParameter inputParameter = new UserDefinedInputParameter();
         inputParameter.name = name.getValue();
         inputParameter.type = type.getValue();
         inputParameter.unit = unit.getValue();
         inputParameter.description = description.getValue();
         return inputParameter;
      }
   }
}
