package no.imr.tools.swing;

import javax.swing.SwingUtilities;

final class ContrastingColorWithSameHueMain {
   private ContrastingColorWithSameHueMain() {
   }

   static void main() {
      SwingUtilities.invokeLater(() -> {
         new ContrastingColorDialog("Contrasting color with same hue", color -> ColorUtils.contrastingColorWithSameHue(color, 3));
      });
   }
}
