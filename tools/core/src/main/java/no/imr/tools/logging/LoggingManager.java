package no.imr.tools.logging;

import no.imr.tools.ToolsPreferences;
import no.imr.tools.Utils;
import no.imr.tools.adm.AdmService;
import no.imr.tools.adm.AppEventHandler;
import no.imr.tools.adm.ApplicationInfo;
import no.imr.tools.adm.LicenseInfo;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.test.UniqueTmpDir;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A logging manager with handlers for console and file.
 */
public final class LoggingManager {
   public static final String TOP_INSTALLATION_DIR = "TOP_INSTALLATION_DIR";
   public static final String APP_DATA_HOME = "APP_DATA_HOME";
   private static final Level FILE_LEVEL = Level.FINE;
   private static final Level CRITICAL_LEVEL = Level.WARNING;
   private static final String DATE_FORMAT_PATTERN = "yyyy_MM_dd-HH_mm_ss";
   private static final String PREFERENCE_SHUT_DOWN_OK = "shutDownOk";

   private static final Path TOP_DIR = findTopInstallationDir();

   private final ApplicationInfo applicationInfo;
   private boolean useWindowHandler = true;
   private @Nullable FileHandler fileHandler;
   private @Nullable Path logFile;
   private final Path installationDir;
   private @Nullable Path applicationDataDir;
   private final OutOfMemoryHandler outOfMemoryHandler = new OutOfMemoryHandler(this);
   private AppEventHandler appEventHandler = AppEventHandler.ignore();

   /**
    * Creates a new LoggingManager.
    *
    * @param applicationInfo application info
    */
   public LoggingManager(ApplicationInfo applicationInfo) {
      this.applicationInfo = applicationInfo;
      installationDir = getTopInstallationDir().resolve(applicationInfo.installationSubDir());
   }

   public @Nullable Path getLogFile() {
      return logFile;
   }

   public ApplicationInfo getApplicationInfo() {
      return applicationInfo;
   }

   public Path getStartupScript() {
      return installationDir.resolve(applicationInfo.getStartupScriptName() + FileUtils.SCRIPT_SUFFIX);
   }

   public void setUseWindowHandler(boolean useWindowHandler) {
      this.useWindowHandler = useWindowHandler;
   }

   public AppEventHandler getAppEventHandler() {
      return appEventHandler;
   }

   public void setAppEventHandler(AppEventHandler appEventHandler) {
      this.appEventHandler = appEventHandler;
   }

   public void startLogging() {
      startFileLogging();
      if (useWindowHandler) {
         startWindowHandler();
         startOutOfMemoryErrorLogging();
      }
      logBasicInfo();
   }

   private void logBasicInfo() {
      Log.global.info(applicationInfo.appName() + " version " + applicationInfo.version() + " built on " + Utils.BUILD_TIME);
      LicenseInfo licenseInfo = AdmService.INSTANCE.getLicenseInfo();
      if (licenseInfo != null) {
         Log.global.config("License: " + licenseInfo.id() + "; " + licenseInfo.licensedTo()
               + "; " + licenseInfo.expiration() + "; " + licenseInfo.features());
      }
      Log.global.config("Application home: " + installationDir);
      Log.global.config(Utils.getSystemProperties());
   }

   void stopLogging() {
      // Only file logging needs to be stopped.
      closeFileHandler();
   }

   public void shutDown() {
      shutDown(0);
   }

   public void shutDown(int exitCode) {
      Log.global.info("Shutting down " + applicationInfo.appName());
      stopLogging();
      getPreferences(applicationInfo).putBoolean(PREFERENCE_SHUT_DOWN_OK, true);
      System.exit(exitCode);
   }

   public static boolean isRestartDueToError(String[] args, ApplicationInfo applicationInfo) {
      Preferences preferences = getPreferences(applicationInfo);
      boolean shutDownOk = preferences.getBoolean(PREFERENCE_SHUT_DOWN_OK, false);
      preferences.remove(PREFERENCE_SHUT_DOWN_OK);
      if (!shutDownOk) {
         String restartDueToErrorValue = getRestartDueToErrorValue(args);
         if (restartDueToErrorValue != null) {
            Log.global.info("Restart due to error: " + restartDueToErrorValue);
            return true;
         }
      }
      return false;
   }

   public static @Nullable String getRestartDueToErrorValue(String[] args) {
      for (int i = 1; i < args.length; i++) {
         if (args[i - 1].equals("--restart-due-to-error")) {
            return args[i];
         }
      }
      return null;
   }

   private static Preferences getPreferences(ApplicationInfo applicationInfo) {
      return ToolsPreferences.node("application", applicationInfo);
   }

   public void activateExitOnOutOfMemory() {
      // If not dist version then keep application running to allow debugging.
      if (Utils.IS_DIST_VERSION) {
         outOfMemoryHandler.doExitOnOutOfMemory();
      }
   }

   private void startOutOfMemoryErrorLogging() {
      Log.addHandler(outOfMemoryHandler);
   }

   public void activateAppEventErrorHandler() {
      Log.addHandler(new AppEventErrorHandler(this));
   }

   private void startWindowHandler() {
      WindowHandler windowHandler = new WindowHandler(this);
      windowHandler.setLevel(CRITICAL_LEVEL);
      Log.addHandler(windowHandler);
   }

   public Path getLogDir() {
      return getApplicationDataDir().resolve("log");
   }

   private void startFileLogging() {
      Path logDir = getLogDir();

      DateTimeFormatter dateFormat = Utils.createLocalDateTimeFormatter(DATE_FORMAT_PATTERN);
      String prefix = applicationInfo.appName() + "-";
      String suffix = ".log";
      logFile = logDir.resolve(prefix + dateFormat.format(Instant.now()) + suffix);
      try {
         FileUtils.createDirectories(logDir);
         fileHandler = new FileHandler(logFile.toString());
         fileHandler.setEncoding("UTF-8");
         fileHandler.setFormatter(new OneLineFormatter());
         fileHandler.setLevel(FILE_LEVEL);
         Log.addHandler(fileHandler);
         Log.global.info("Logging to " + logFile);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Could not start logging to " + logFile, e);
      }

      Exec.schedule(() -> {
         try {
            deleteOldLogFiles(logDir);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error deleting old log files", e);
         }
      }, 15, TimeUnit.SECONDS);
   }

   private void closeFileHandler() {
      if (fileHandler == null || logFile == null) {
         return;
      }
      Log.removeHandler(fileHandler);
      fileHandler.close();

      if (Log.getMaxLevel().intValue() < CRITICAL_LEVEL.intValue()) {
         try {
            Files.deleteIfExists(logFile);
         } catch (IOException e) {
            Log.global.warning("Could not delete log file " + logFile);
         }
      }
   }

   private static void deleteOldLogFiles(Path logDir) throws IOException {
      Instant keepTime = Instant.now().minus(30, ChronoUnit.DAYS);
      Pattern logFilePattern = Pattern.compile(".*-(\\d{4}([_\\-]\\d\\d){5})\\.log.*");

      FileUtils.listFilesWithAttributes(logDir).forEach(fileInfo -> {
         Path file = fileInfo.file();
         String name = file.getFileName().toString();
         Matcher matcher = logFilePattern.matcher(name);
         if (matcher.matches()) {
            Instant modificationTime = fileInfo.lastModifiedTime().toInstant();
            if (modificationTime.isBefore(keepTime)) {
               Log.global.info("Deleting old log file " + file);
               try {
                  Files.deleteIfExists(file);
               } catch (IOException e) {
                  Log.global.log(Level.WARNING, "Error deleting log file " + file, e);
               }
            }
         }
      });
   }

   public Path getInstallationDir() {
      return installationDir;
   }

   public static Path getTopInstallationDir() {
      return TOP_DIR;
   }

   private static Path findTopInstallationDir() {
      String topInstallationDir = Utils.getSystemPropertyOrEnv(TOP_INSTALLATION_DIR);
      if (topInstallationDir != null) {
         return Path.of(topInstallationDir).toAbsolutePath().normalize();
      }
      Path dir = Path.of(".").toAbsolutePath().normalize();
      while (true) {
         if (Files.isDirectory(dir.resolve("lib"))) {
            return dir;
         }
         dir = dir.getParent();
         if (dir == null) {
            throw new AssertionError("Cannot find top dir");
         }
      }
   }

   public Path getApplicationDataDir() {
      if (applicationDataDir == null) {
         applicationDataDir = getApplicationDataRoot().resolve(applicationInfo.applicationDataSubDir());
      }
      return applicationDataDir;
   }

   public static Path getApplicationDataRoot() {
      String appDataHome = Utils.getSystemPropertyOrEnv(APP_DATA_HOME);
      if (appDataHome != null) {
         return Path.of(appDataHome).toAbsolutePath().normalize();
      }
      return Utils.getUserHome().resolve(".ApplicationData");
   }

   public static void initTmpApplicationDataDir() {
      System.setProperty(APP_DATA_HOME, UniqueTmpDir.newSubDir(".ApplicationData").toString());
   }
}
