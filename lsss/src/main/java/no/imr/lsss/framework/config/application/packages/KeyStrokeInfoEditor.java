package no.imr.lsss.framework.config.application.packages;

import no.imr.lsss.framework.config.application.packages.pojo.KeyStrokeInfo;
import no.imr.lsss.framework.packages.LsssAction;
import no.imr.lsss.framework.packages.LsssPackage;
import no.imr.lsss.modules.BaseLsssModule;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.gui.ParameterEditor;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

final class KeyStrokeInfoEditor implements ParameterContainer {
   private final StringParameter filter = new StringParameter(new Name("Filter"),
         "",
         "Filters list of actions");

   private final ActionParameter action = new ActionParameter();

   private final StringParameter keyStroke = new StringParameter(new Name("Keystroke"),
         "",
         "Type the keystroke");

   private final StringParameter context = new StringParameter(new Name("Context"));

   private final UserDefinedPackage userDefinedPackage;
   private final KeyStrokeInfo keyStrokeInfo;

   KeyStrokeInfoEditor(UserDefinedPackage userDefinedPackage, KeyStrokeInfo keyStrokeInfo) {
      this.userDefinedPackage = userDefinedPackage;
      this.keyStrokeInfo = keyStrokeInfo;

      keyStroke.setValue(keyStrokeInfo.keyStroke);
      List<String> contexts = userDefinedPackage.getLSSS().getModuleManager().getViewModules().values().stream()
            .flatMap(Collection::stream)
            .map(BaseLsssModule::getPersistentName)
            .collect(Collectors.toCollection(ArrayList::new));
      contexts.add(LsssPackage.KEY_STROKE_CONTEXT_ANYWHERE);
      contexts.add(LsssPackage.KEY_STROKE_CONTEXT_MAIN_WINDOW);
      contexts.add(LsssPackage.KEY_STROKE_CONTEXT_ANY_ECHOGRAM_MODULE);
      if (!contexts.contains(keyStrokeInfo.context)) {
         contexts.add(keyStrokeInfo.context);
      }
      contexts.sort(String::compareToIgnoreCase);
      context.setAllowedValuesAndValue(contexts, keyStrokeInfo.context);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            filter,
            action,
            keyStroke,
            context
      );
   }

   void init(ParameterEditor parameterEditor) {
      action.init(parameterEditor, filter, userDefinedPackage, keyStrokeInfo);

      JComponent inputComponent = parameterEditor.getInputComponent(keyStroke);
      inputComponent.addKeyListener(new KeyListener() {
         @Override
         public void keyTyped(KeyEvent e) {
            e.consume();
         }

         @Override
         public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
               case KeyEvent.VK_ALT, KeyEvent.VK_ALT_GRAPH, KeyEvent.VK_CONTROL, KeyEvent.VK_META, KeyEvent.VK_SHIFT -> {
               }
               default -> {
                  keyStroke.setValue(KeyStroke.getKeyStrokeForEvent(e).toString().replace("pressed ", ""));
               }
            }
            e.consume();
         }

         @Override
         public void keyReleased(KeyEvent e) {
            e.consume();
         }
      });
   }

   boolean isOK(ParameterEditor parameterEditor) {
      if (!parameterEditor.commitEdits()) {
         return false;
      }
      if (action.getValue().isEmpty()) {
         JOptionPane.showMessageDialog(parameterEditor.getEditorComponent(), "Please select an action", "Error", JOptionPane.ERROR_MESSAGE);
         parameterEditor.getInputComponent(action).requestFocusInWindow();
         return false;
      }
      if (keyStroke.getValue().isEmpty()) {
         JOptionPane.showMessageDialog(parameterEditor.getEditorComponent(), "Please type a keystroke", "Error", JOptionPane.ERROR_MESSAGE);
         parameterEditor.getInputComponent(keyStroke).requestFocusInWindow();
         return false;
      }
      return true;
   }

   void apply() {
      LsssAction lsssAction = action.getValue().orElseThrow();
      String packageId = lsssAction.getLsssPackage().getId();
      keyStrokeInfo.packageId = packageId.equals(userDefinedPackage.id) ? "" : packageId;
      keyStrokeInfo.actionId = lsssAction.getId();
      keyStrokeInfo.keyStroke = keyStroke.getValue();
      keyStrokeInfo.context = context.getValue();
   }
}
