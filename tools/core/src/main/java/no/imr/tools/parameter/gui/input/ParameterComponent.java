package no.imr.tools.parameter.gui.input;

import javax.swing.JComponent;

/**
 * Base class for components for editing parameters.
 */
abstract class ParameterComponent {
   ParameterComponent() {
   }

   abstract JComponent getComponent();

   abstract void updateComponent();

   abstract boolean commitEdit();
}
