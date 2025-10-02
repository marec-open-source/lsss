package no.imr.tools.visualizer;

import javax.swing.JComponent;

abstract class ItemView<T> {
   ItemView() {
   }

   abstract JComponent getComponent();

   abstract void selectFeatures(ItemFeature<T> x, ItemFeature<T> y);

   abstract void update();
}
