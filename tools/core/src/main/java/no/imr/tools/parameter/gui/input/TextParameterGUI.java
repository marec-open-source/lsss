package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.TextParameter;
import no.imr.tools.swing.GridBag;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Dimension;

/**
 * GUI for a {@link TextParameter}.
 */
public final class TextParameterGUI extends ParameterGUI<TextParameter> {
   private final ParameterTextArea parameterComponent;

   TextParameterGUI(TextParameter parameter, GUIConfig guiConfig) {
      super(parameter, guiConfig);

      parameterComponent = new ParameterTextArea(parameter, guiConfig);
   }

   @Override
   public void installGUI(GridBag gridBag) {
      addName(gridBag);

      gridBag.getConstraints().anchor = getGUIConfig().getInputFieldAlignment().getGridBagConstraintsAnchor();
      if (getGUIConfig().getHorizontalFill(getParameter())) {
         gridBag.activateHorizontalFill();
      }
      if (getParameter().getProperty(BaseParameter.KEY_VERTICAL_FILL)) {
         gridBag.activateVerticalFill();
      }
      if (getParameter().getProperty(BaseParameter.KEY_TEXT_SCROLL_PANE)) {
         JScrollPane scrollPane = new Workaround4238932ScrollPane(parameterComponent.getComponent());
         addInputAndDescription(gridBag, scrollPane);
      } else {
         parameterComponent.getComponent().setBorder(new JTextField().getBorder());
         addInputAndDescription(gridBag, parameterComponent.getComponent());
      }
   }

   @Override
   public void updateInput() {
      parameterComponent.updateComponent();
      updateEnabledState(parameterComponent.getComponent());
   }

   @Override
   public JComponent getInputComponent() {
      return parameterComponent.getComponent();
   }

   /**
    * Workaround for <a href="https://bugs.openjdk.org/browse/JDK-4238932">JDK-4238932</a>.
    */
   private static final class Workaround4238932ScrollPane extends JScrollPane {
      private Workaround4238932ScrollPane(Component view) {
         super(view);
      }

      @Override
      public Dimension getMinimumSize() {
         return getPreferredSize();
      }
   }
}
