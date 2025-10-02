package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BaseValueParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.parameter.CustomGuiParameter;
import no.imr.tools.parameter.DynamicListParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.MultiParameter;
import no.imr.tools.parameter.PasswordParameter;
import no.imr.tools.parameter.RangeParameter;
import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.parameter.TextParameter;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.parameter.VoidParameter;

import java.util.List;

/**
 * Creates new instances of {@link ParameterGUI}.
 */
public final class ParameterGUIFactory {
   private ParameterGUIFactory() {
   }

   public static ParameterGUI<?> createParameterGUI(BaseParameter<?> parameter, GUIConfig guiConfig) {
      return createParameterGUI(parameter, guiConfig, List.of(parameter));
   }

   public static ParameterGUI<?> createParameterGUI(BaseParameter<?> parameter, GUIConfig guiConfig, List<? extends BaseParameter<?>> parameters) {
      ParameterGUI<?> parameterGUI = switch (parameter) {
         case BaseValueParameter<?> baseValueParameter -> switch (baseValueParameter) {
            case DynamicListParameter<?> dynamicListParameter -> new DynamicListParameterGUI<>(dynamicListParameter, guiConfig);
            case RangeParameter rangeParameter -> new RangeParameterGUI(rangeParameter, guiConfig);
            case ValueParameter<?> valueParameter -> switch (valueParameter) {
               case BooleanParameter booleanParameter    -> new BooleanParameterGUI(booleanParameter, guiConfig);
               case FileParameter fileParameter          -> new FileParameterGUI(fileParameter, guiConfig);
               case PasswordParameter passwordParameter  -> new PasswordParameterGUI(passwordParameter, guiConfig);
               case TextParameter textParameter          -> new TextParameterGUI(textParameter, guiConfig);
               default                                   -> new ValueParameterGUI<>(valueParameter, guiConfig);
            };
         };
         case MultiParameter<?> multiParameter -> new MultiParameterGUI(multiParameter, guiConfig);
         case VoidParameter voidParameter -> switch (voidParameter) {
            case ButtonParameter buttonParameter       -> new ButtonParameterGUI(buttonParameter, guiConfig);
            case CustomGuiParameter customGuiParameter -> new CustomGuiParameterGUI(customGuiParameter, guiConfig);
            case HeaderParameter headerParameter       -> new HeaderParameterGUI(headerParameter, guiConfig);
            case SeparatorParameter separatorParameter -> new SeparatorParameterGUI(separatorParameter, guiConfig);
         };
      };
      parameterGUI.init(parameters);
      return parameterGUI;
   }
}
