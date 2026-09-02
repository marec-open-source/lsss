package no.imr.tools.swing;

import no.imr.tools.Utils;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Color;

final class WrappingFlowLayoutMain {
   private WrappingFlowLayoutMain() {
   }

   static void main(String[] args) {
      Utils.init(args, MiscIcons.SETTINGS.getImage());
      SwingUtilities.invokeLater(WrappingFlowLayoutMain::run);
   }

   private static void run() {
      JPanel panel = new VerticalScrollablePanel(new WrappingFlowLayout());
      panel.setBackground(Color.WHITE);
      for (int i = 0; i < 50; i++) {
         panel.add(new JCheckBox("<html>" + (i + 1) + " " + "a".repeat(i % 6) + ((i / 10) % 2 == 0 ? "" : "<br>bbb<br>ccc<br>ddd")));
      }
      JScrollPane scrollPane = new JScrollPane(panel);

      JFrame frame = new JFrame();
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.add(scrollPane);
      frame.setSize(300, 500);
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
   }
}
