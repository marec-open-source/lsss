package no.imr.lsss.modules.interpretation;

import no.imr.lsss.plugins.FeaturePlugin;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;

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
}
