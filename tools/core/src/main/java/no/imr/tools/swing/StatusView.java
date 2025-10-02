package no.imr.tools.swing;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public final class StatusView {
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final JLabel mainLabel;
   private final JLabel secondaryLabel = new JLabel("");

   public StatusView(String mainText) {
      mainLabel = new JLabel(mainText);
      mainPanel.add(mainLabel, BorderLayout.NORTH);
      mainPanel.add(secondaryLabel);
   }

   public JComponent getComponent() {
      return mainPanel;
   }

   public void setMainText(String text) {
      SwingDelayer.invokeLater(mainLabel, () -> mainLabel.setText(text));
   }

   public void setSecondaryText(String text) {
      SwingDelayer.invokeLater(secondaryLabel, () -> secondaryLabel.setText(text));
   }
}
