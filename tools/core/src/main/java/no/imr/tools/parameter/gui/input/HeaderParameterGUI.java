package no.imr.tools.parameter.gui.input;

import com.google.common.html.HtmlEscapers;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.svg.SvgIcon;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;

/**
 * GUI for a {@link HeaderParameter}.
 */
public final class HeaderParameterGUI extends ParameterGUI<HeaderParameter> {
   private final Box box = Box.createVerticalBox();
   private final JLabel headerLabel = new JLabel();
   private final JLabel contentLabel = new JLabel();

   HeaderParameterGUI(HeaderParameter parameter, GUIConfig guiConfig) {
      super(parameter, guiConfig);

      box.add(headerLabel);
      box.add(contentLabel);
      contentLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
   }

   @Override
   public void installGUI(GridBag gridBag) {
      gridBag.getConstraints().gridwidth = GridBagConstraints.REMAINDER;
      gridBag.activateHorizontalFill();
      gridBag.add(box);
   }

   @Override
   public void updateInput() {
      String text = "<html><h3>" + HtmlEscapers.htmlEscaper().escape(getParameter().getDisplayName()) + "</h3>";
      headerLabel.setText(text);
      SvgIcon icon = getParameter().getProperty(BaseParameter.KEY_ICON).orElse(null);
      if (icon != null) {
         icon.on(headerLabel);
      } else {
         SvgIcon.remove(headerLabel);
      }
      String htmlContent = getParameter().getHtmlContent();
      contentLabel.setText("<html>" + htmlContent);
      contentLabel.setVisible(!htmlContent.isEmpty());
      // Do not disable gui for header parameters.
   }

   @Override
   public @Nullable JComponent getInputComponent() {
      return null;
   }
}
