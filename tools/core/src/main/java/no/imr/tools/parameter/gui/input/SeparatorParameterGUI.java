package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.swing.GridBag;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JSeparator;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;

/**
 * GUI for a {@link SeparatorParameter}.
 */
public final class SeparatorParameterGUI extends ParameterGUI<SeparatorParameter> {
   private final Component separator;

   SeparatorParameterGUI(SeparatorParameter parameter, GUIConfig guiConfig) {
      super(parameter, guiConfig);

      separator = switch (parameter.getSeparatorType()) {
         case LINE -> createLineComponent();
         case SPACE -> Box.createVerticalStrut(10);
      };
   }

   @Override
   public void installGUI(GridBag gridBag) {
      gridBag.getConstraints().gridwidth = GridBagConstraints.REMAINDER;
      gridBag.activateHorizontalFill();
      gridBag.add(separator);
   }

   @Override
   public void updateInput() {
      // Do not disable gui for separator parameters.
   }

   @Override
   public JComponent getInputComponent() {
      return ParameterGuiUtils.noInputComponent();
   }

   private static Box createLineComponent() {
      JSeparator separator = new JSeparator();
      separator.setForeground(Color.GRAY);
      Box box = Box.createVerticalBox();
      box.add(Box.createVerticalStrut(5));
      box.add(separator);
      box.add(Box.createVerticalStrut(5));
      return box;
   }
}
