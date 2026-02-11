package no.imr.korona;

import no.imr.korona.apps.fiskview.FiskView;
import no.imr.korona.computation.ModuleManager;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.ConfigFileSettingsContext;
import no.imr.korona.config.KoronaSettings;
import no.imr.korona.data.DataFormatManager;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.resources.KoronaResource;
import no.imr.tools.Utils;
import no.imr.tools.adm.ApplicationInfo;
import no.imr.tools.logging.LoggingManager;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * The main KORONA class.
 */
public final class Korona {
   public static final String VERSION = Utils.IS_BUILT_VERSION
         ? "@LSSS_VERSION@" // Substituted during build.
         : Utils.readBuildVersion("lsss");
   public static final ApplicationInfo APPLICATION_INFO = new ApplicationInfo(
         "LSSS", "Large Scale Survey System", "KORONA", VERSION,
         KoronaResource.KORONA_32, KoronaResource.KORONA_64,
         "korona", "lsss");
   private static final LoggingManager LOGGING_MANAGER = new LoggingManager(APPLICATION_INFO);

   private final ModuleManager moduleManager;
   private final DataFormatManager dataFormatManager;
   private @Nullable KoronaSettings koronaSettings;

   public Korona() {
      this(new ModuleManager());
   }

   public Korona(ModuleManager moduleManager) {
      this.moduleManager = moduleManager;
      dataFormatManager = new DataFormatManager();
   }

   static void main(String[] args) {
      FiskView.main(args);
   }

   public DataFormatManager getDataFormatManager() {
      return dataFormatManager;
   }

   public DatagramTypeManager getDatagramTypeManager() {
      return dataFormatManager.getDatagramTypeManager();
   }

   public ModuleManager getModuleManager() {
      return moduleManager;
   }

   public KoronaSettings getKoronaSettings() {
      if (koronaSettings == null) {
         koronaSettings = new KoronaSettings(getApplicationDataDir().resolve("KoronaSettings.xml"));
      }
      return koronaSettings;
   }

   public ConfigFileSettings createConfigFileSettings() {
      ConfigFileSettings configFileSettings = new ConfigFileSettings();
      configFileSettings.getReferenceDirectoryManager().add(getKoronaSettings().getReferenceDirectoryCollection());
      return configFileSettings;
   }

   public ConfigFileSettings createConfigFileSettings(ConfigFileSettingsContext context) {
      ConfigFileSettings configFileSettings = createConfigFileSettings();
      configFileSettings.setContext(context);
      return configFileSettings;
   }

   public static Path getInstallationDir() {
      return LOGGING_MANAGER.getInstallationDir();
   }

   public static Path getInstallationConfigDir() {
      return getInstallationDir().resolve("config");
   }

   public static Path getInstallationDataDir() {
      return getInstallationDir().resolve("data");
   }

   public static Path getApplicationDataDir() {
      return LOGGING_MANAGER.getApplicationDataDir();
   }
}
