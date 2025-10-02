package no.imr.tools.swing;

import javax.swing.SwingUtilities;

final class ContrastingColorWithSameHueMain {
   private ContrastingColorWithSameHueMain() {
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> {
         new ContrastingColorDialog("Contrasting color with same hue", color -> ColorUtils.contrastingColorWithSameHue(color, 3));
      });
   }
}
