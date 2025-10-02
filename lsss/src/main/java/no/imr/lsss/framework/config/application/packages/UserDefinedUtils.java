package no.imr.lsss.framework.config.application.packages;

import no.imr.lsss.framework.config.application.packages.pojo.UserDefinedInputParameter;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatCsvListParameter;
import no.imr.tools.parameter.IntCsvListParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.OptionalIntParameter;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

final class UserDefinedUtils {
   static final String INPUT_BOOLEAN = "boolean";
   static final String INPUT_FLOAT = "float";
   static final String INPUT_FLOAT_ARRAY = "floatArray";
   static final String INPUT_INT = "int";
   static final String INPUT_INT_ARRAY = "intArray";
   static final String INPUT_STRING = "string";
   static final List<String> INPUT_TYPES = List.of(INPUT_BOOLEAN, INPUT_FLOAT, INPUT_FLOAT_ARRAY, INPUT_INT, INPUT_INT_ARRAY, INPUT_STRING);

   private UserDefinedUtils() {
   }

   static String combinedText(String firstLine, String text) {
      if (text.isEmpty()) {
         return firstLine;
      }
      HtmlStringBuilder builder = new HtmlStringBuilder()
            .html("<p>")
            .text(firstLine)
            .html("</p>")
            .html("<p>");
      if (text.startsWith("<html>")) {
         builder.html(text.substring(6));
      } else {
         text.lines().forEach(line -> {
            builder.text(line).html("<br>");
         });
      }
      return builder
            .html("</p>")
            .build();
   }

   static Optional<Map<String, Object>> showInputDialog(UserDefinedAction action, List<UserDefinedInputParameter> inputParameters, @Nullable Component referenceComponent) {
      if (inputParameters.isEmpty()) {
         return Optional.of(Map.of());
      }

      List<? extends ValueParameter<?>> parameters = inputParameters.stream()
            .map(UserDefinedUtils::toParameter)
            .toList();
      ParameterEditor parameterEditor = new ParameterEditor(parameters);

      ConfigurableGUIDialog configurableGUIDialog = new ConfigurableGUIDialog(referenceComponent, action.getEffectiveLabel(), new ParameterCollection(parameters))
            .accessOKButton(okButton -> {
               MiscIcons.PLAY.on(okButton).setText("Run");
               okButton.setToolTipText(null);
            })
            .accessCancelButton(cancelButton -> cancelButton.setToolTipText(null))
            .setCloseOnOk(() -> {
               if (!parameterEditor.commitEdits()) {
                  return false;
               }
               for (ValueParameter<?> parameter : parameters) {
                  Object value = parameter.getValue();
                  if (value instanceof Optional<?> optional && optional.isEmpty()) {
                     JOptionPane.showMessageDialog(referenceComponent, "Please enter a value for " + parameter.getDisplayName(), "Error", JOptionPane.ERROR_MESSAGE);
                     parameterEditor.getInputComponent(parameter).requestFocusInWindow();
                     return false;
                  }
               }
               return true;
            })
            .setGUI(parameterEditor.getEditorComponent());

      KeyAdapter keyListener = new KeyAdapter() {
         @Override
         public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
               SwingUtilities.invokeLater(() -> {
                  for (ValueParameter<?> parameter : parameters) {
                     if (parameter.getStringValue().isEmpty()) {
                        parameterEditor.getInputComponent(parameter).requestFocusInWindow();
                        return;
                     }
                  }
                  configurableGUIDialog.ok();
               });
            }
         }
      };
      parameters.stream()
            .map(parameterEditor::getInputComponent)
            .forEach(component -> component.addKeyListener(keyListener));

      boolean ok = configurableGUIDialog.show();
      if (!ok) {
         return Optional.empty();
      }

      Map<String, Object> input = parameters.stream()
            .collect(Collectors.toMap(BaseParameter::getPersistentName, UserDefinedUtils::toValue));
      return Optional.of(input);
   }

   private static Object toValue(ValueParameter<?> parameter) {
      Object value = parameter.getValue();
      if (value instanceof Optional<?> optional) {
         return optional.orElseThrow();
      }
      return value;
   }

   private static ValueParameter<?> toParameter(UserDefinedInputParameter inputParameter) {
      return switch (inputParameter.type) {
         case INPUT_BOOLEAN -> {
            yield new BooleanParameter(new Name(inputParameter.name),
                  false,
                  inputParameter.description);
         }
         case INPUT_FLOAT -> {
            yield new OptionalFloatParameter(new Name(inputParameter.name),
                  Optional.empty(), new Unit(inputParameter.unit),
                  inputParameter.description);
         }
         case INPUT_FLOAT_ARRAY -> {
            yield new FloatCsvListParameter(new Name(inputParameter.name),
                  List.of(), new Unit(inputParameter.unit),
                  inputParameter.description);
         }
         case INPUT_INT -> {
            yield new OptionalIntParameter(new Name(inputParameter.name),
                  Optional.empty(), new Unit(inputParameter.unit),
                  inputParameter.description);
         }
         case INPUT_INT_ARRAY -> {
            yield new IntCsvListParameter(new Name(inputParameter.name),
                  List.of(), new Unit(inputParameter.unit),
                  inputParameter.description);
         }
         default -> {
            yield new StringParameter(new Name(inputParameter.name),
                  "",
                  inputParameter.description);
         }
      };
   }
}
