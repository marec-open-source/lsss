package no.imr.korona.computation.display;

import javax.swing.JComponent;

public abstract class VisualizerModule extends PlayboxModule {
   protected VisualizerModule() {
   }

   public abstract JComponent getComponent();
}
