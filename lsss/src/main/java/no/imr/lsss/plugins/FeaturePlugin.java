package no.imr.lsss.plugins;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseReportManager;
import no.imr.lsss.database.reports.ReportEngine;
import no.imr.lsss.framework.backup.BackupExclusionOption;
import no.imr.lsss.framework.config.LsssConfiguration;
import no.imr.lsss.framework.config.application.SubDir;
import no.imr.lsss.framework.export.Exporter;
import no.imr.lsss.modules.ModuleCollection;
import no.imr.tools.database.DatabaseContent;
import no.imr.tools.help.HelpSystemHelpSet;
import no.imr.tools.plugins.BasePlugin;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.svg.SvgIcon;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * The API for feature plugins.
 */
public abstract class FeaturePlugin extends BasePlugin {
   private final FeatureService service;
   private final LSSS lsss;

   protected FeaturePlugin(FeatureService service, LSSS lsss) {
      super(service.getName());

      this.service = service;
      this.lsss = lsss;
   }

   public LSSS getLSSS() {
      return lsss;
   }

   public @Nullable SvgIcon getIcon() {
      return service.getIcon();
   }

   public SvgIcon getIconOrEmpty() {
      SvgIcon icon = getIcon();
      return icon != null ? icon : MiscIcons.EMPTY;
   }

   public String getMainPluginId() {
      return getPersistentName();
   }

   public HelpSystemHelpSet getHelpSet() {
      return HelpSystemHelpSet.EMPTY;
   }

   public ModuleCollection<?> getModules() {
      return new ModuleCollection<>(this);
   }

   public List<Exporter> createExporters() {
      return List.of();
   }

   public List<SubDir> getSubDirs() {
      return List.of();
   }

   public void addConfiguration(LsssConfiguration lsssConfiguration) {
   }

   public @Nullable DatabaseReportManager createDatabaseReportManager(ReportEngine reportEngine) {
      return null;
   }

   public @Nullable WorkFileManager getWorkFileManager() {
      return null;
   }

   public List<Path> getExcludedDirsForCopying() {
      return List.of();
   }

   public List<BackupExclusionOption> getBackupExclusionOptions() {
      return List.of();
   }

   public @Nullable DatabaseContent getDatabaseContent() {
      return null;
   }

   public void setup() {
   }

   public void close() {
   }
}
