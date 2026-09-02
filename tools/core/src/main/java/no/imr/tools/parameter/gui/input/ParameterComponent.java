package no.imr.tools.parameter.gui.input;

import javax.swing.JComponent;

/**
 * Interface for components for editing parameters.
 */
interface ParameterComponent {
   JComponent getComponent();

   void updateComponent();
}
