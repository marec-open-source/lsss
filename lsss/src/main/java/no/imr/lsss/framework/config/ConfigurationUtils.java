package no.imr.lsss.framework.config;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.FlowLayout;

public final class ConfigurationUtils {
   private ConfigurationUtils() {
   }

   public static JPanel createTitledButtonPanel(String title) {
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
      panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(title),
            BorderFactory.createEmptyBorder(1, 0, 3, 0)));
      return panel;
   }
}
