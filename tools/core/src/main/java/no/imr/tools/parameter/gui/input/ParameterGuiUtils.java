package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.ParameterException;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public final class ParameterGuiUtils {
   private ParameterGuiUtils() {
   }

   public static void showErrorDialog(ParameterException parameterException, JComponent referenceComponent) {
      BaseParameter<?> parameter = parameterException.getParameter();
      boolean monospaced = parameter.getProperty(BaseParameter.KEY_MONOSPACED);
      String text = "<html>"
            + (monospaced ? "<pre>" : "")
            + "Illegal value for parameter <code>" + parameter.getDisplayName() + "</code>:<br>"
            + parameterException.getMessage();
      String allowedValuesDescription = parameter.getAllowedValuesDescription();
      if (allowedValuesDescription != null) {
         text += "<br>Accepted values:<br>" + allowedValuesDescription;
      }
      JOptionPane.showMessageDialog(referenceComponent, text, "Illegal parameter value", JOptionPane.ERROR_MESSAGE);
   }

   public static @Nullable String getNameToolTip(BaseParameter<?> parameter) {
      String description = parameter.getDescription();
      String allowedValuesDescription = parameter.getAllowedValuesDescription();

      if (description.isEmpty() && allowedValuesDescription == null) {
         return null;
      }

      StringBuilder sb = new StringBuilder("<html>")
            .append(description);
      if (allowedValuesDescription != null) {
         if (!description.isEmpty()) {
            sb.append("<br>");
         }
         sb.append("Allowed values: ").append(allowedValuesDescription);
      }
      return sb.toString();
   }

   public static @Nullable String getInputToolTip(BaseParameter<?> parameter) {
      String allowedValuesDescription = parameter.getAllowedValuesDescription();
      if (allowedValuesDescription != null) {
         return "<html>" + allowedValuesDescription;
      } else {
         return null;
      }
   }

   static JComponent noInputComponent() {
      return new JLabel("No input");
   }
}
