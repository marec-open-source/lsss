package no.imr.tools.swing;

import javax.swing.SwingUtilities;

final class ContrastingBlackOrWhiteMain {
   private ContrastingBlackOrWhiteMain() {
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> {
         new ContrastingColorDialog("Contrasting black or white", ColorUtils::contrastingBlackOrWhite);
      });
   }
}
