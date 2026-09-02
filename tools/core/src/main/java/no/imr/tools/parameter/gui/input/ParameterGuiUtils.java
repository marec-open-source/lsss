package no.imr.tools.parameter.gui.input;

import com.google.common.html.HtmlEscapers;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.ParameterException;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

public final class ParameterGuiUtils {
   private ParameterGuiUtils() {
   }

   public static JDialog createErrorDialog(ParameterException parameterException, JComponent referenceComponent) {
      BaseParameter<?> parameter = parameterException.getParameter();
      boolean monospaced = parameter.getProperty(BaseParameter.KEY_MONOSPACED);
      String text = "<html>"
            + (monospaced ? "<pre>" : "")
            + "Illegal value for parameter <code>" + parameter.getDisplayName() + "</code>:<br>"
            + HtmlEscapers.htmlEscaper().escape(String.valueOf(parameterException.getMessage()));
      String allowedValuesDescription = parameter.getAllowedValuesDescription();
      if (allowedValuesDescription != null) {
         text += "<br>Accepted values:<br>" + allowedValuesDescription;
      }
      JOptionPane optionPane = new JOptionPane(text, JOptionPane.ERROR_MESSAGE);
      JDialog dialog = optionPane.createDialog(referenceComponent, "Illegal parameter value");
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      return dialog;
   }

   public static void showErrorDialog(ParameterException parameterException, JComponent referenceComponent) {
      JDialog dialog = createErrorDialog(parameterException, referenceComponent);
      dialog.setVisible(true);
   }

   static String descriptionAsHtml(BaseParameter<?> parameter) {
      String description = parameter.getDescription();
      if (description.startsWith("<html>")) {
         return description.substring(6);
      } else {
         return HtmlEscapers.htmlEscaper().escape(description);
      }
   }

   public static @Nullable String getNameToolTip(BaseParameter<?> parameter) {
      String description = descriptionAsHtml(parameter);
      String allowedValuesDescription = parameter.getAllowedValuesDescription();

      if (description.isEmpty() && allowedValuesDescription == null) {
         return null;
      }

      HtmlStringBuilder sb = new HtmlStringBuilder()
            .html(description);
      if (allowedValuesDescription != null) {
         if (!description.isEmpty()) {
            sb.html("<br>");
         }
         sb.html("Allowed values: ").html(allowedValuesDescription);
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
}
