package no.imr.lsss.modules.interpretation;

import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.swing.ColorUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public abstract class StoreTask {
   private final FeaturePlugin plugin;
   private final String shortLabel;
   private final String longLabel;
   private boolean active = true;

   protected StoreTask(FeaturePlugin plugin, String shortLabel, String longLabel) {
      this.plugin = plugin;
      this.shortLabel = shortLabel;
      this.longLabel = longLabel;
   }

   FeaturePlugin getPlugin() {
      return plugin;
   }

   String getShortLabel() {
      return shortLabel;
   }

   String getLongLabel() {
      return longLabel;
   }

   boolean isActive() {
      return active;
   }

   void setActive(boolean active) {
      this.active = active;
   }

   public abstract @Nullable JComponent getStoreComponent();

   public abstract @Nullable JComponent getDeleteComponent();

   public abstract boolean store();

   public abstract void delete();

   protected static JComponent createWarningLabel(String text) {
      JPanel panel = new JPanel(new BorderLayout());
      panel.setBackground(ColorUtils.TOMATO);
      panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
      panel.add(new JLabel(text));
      return panel;
   }
}
