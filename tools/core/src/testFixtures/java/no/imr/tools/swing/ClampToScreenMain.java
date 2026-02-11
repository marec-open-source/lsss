package no.imr.tools.swing;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

@SuppressWarnings("PMD.SystemPrintln")
final class ClampToScreenMain {
   private ClampToScreenMain() {
   }

   private static void display() {
      JFrame frame = new JFrame(ClampToScreenMain.class.getName());
      JButton button = new JButton("Clamp to screen");
      button.addActionListener(_ -> {
         GuiUtils.clampToScreen(frame);
         System.out.println("-----------------------------------------------");
         System.out.println("Frame bounds:  " + frame.getBounds());
         System.out.println("Frame insets:  " + frame.getInsets());
         System.out.println("RootPane size: " + frame.getRootPane().getSize());
      });
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.getContentPane().add(button);
      frame.setSize(900, 600);
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
   }

   static void main() {
      SwingUtilities.invokeLater(ClampToScreenMain::display);
   }
}
