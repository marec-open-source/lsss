package no.imr.tools.swing;

import javax.swing.SwingUtilities;

final class ContrastingBlackOrWhiteMain {
   private ContrastingBlackOrWhiteMain() {
   }

   static void main() {
      SwingUtilities.invokeLater(() -> {
         new ContrastingColorDialog("Contrasting black or white", ColorUtils::contrastingBlackOrWhite);
      });
   }
}
