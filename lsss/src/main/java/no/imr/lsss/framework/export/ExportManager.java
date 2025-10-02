package no.imr.lsss.framework.export;

import no.imr.lsss.framework.PluginManager;
import no.imr.tools.Utils;

import java.util.List;

public final class ExportManager {
   private final ExportSettings exportSettings = new ExportSettings();
   private final List<Exporter> exporters;

   public ExportManager(PluginManager pluginManager) {
      exporters = pluginManager.getFeaturePlugins().stream()
            .flatMap(plugin -> plugin.createExporters().stream())
            .sorted(Utils.comparingIgnoringCase(exporter -> exporter.name.displayName()))
            .toList();
   }

   public ExportSettings getExportSettings() {
      return exportSettings;
   }

   public List<Exporter> getExporters() {
      return exporters;
   }
}
