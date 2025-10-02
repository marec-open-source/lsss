package no.imr.lsss.server.pojo;

import no.imr.lsss.plugins.FeaturePlugin;

public final class PluginInfo {
   public String id;

   public PluginInfo(FeaturePlugin plugin) {
      id = plugin.getPersistentName();
   }

   @Override
   public String toString() {
      return "PluginInfo{" +
            "id='" + id + '\'' +
            '}';
   }
}
