package no.imr.tools.swing;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

final class MultiColumnLayoutMain {
   private MultiColumnLayoutMain() {
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(MultiColumnLayoutMain::run);
   }

   private static void run() {
      JPanel panel = new VerticalScrollablePanel(new MultiColumnLayout());
      for (int i = 0; i < 50; i++) {
         panel.add(new JCheckBox((i + 1) + " " + "a".repeat(i % 6)));
      }
      JScrollPane scrollPane = new JScrollPane(panel);
      MultiColumnLayout.addRelayoutListener(scrollPane, panel);

      JFrame frame = new JFrame();
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.add(scrollPane);
      frame.setSize(300, 500);
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
   }
}
