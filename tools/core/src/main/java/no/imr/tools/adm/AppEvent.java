package no.imr.tools.adm;

import no.imr.tools.logging.LoggingManager;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AppEvent {
   public static final String APP = "app";
   public static final String VERSION = "version";
   public static final String LICENSE_ID = "licenceId";
   public static final String TYPE = "type";
   public static final String TIME = "time";
   public static final String LOG_FILE = "logFile";
   public static final String TEXT = "text";

   private AppEvent() {
   }

   public static Map<String, Object> create(LoggingManager loggingManager, Type type, String text) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put(APP, loggingManager.getApplicationInfo().appName());
      map.put(VERSION, loggingManager.getApplicationInfo().version());
      LicenseInfo licenseInfo = AdmService.INSTANCE.getLicenseInfo();
      if (licenseInfo != null) {
         map.put(LICENSE_ID, licenseInfo.id());
      }
      map.put(TYPE, type.name());
      map.put(TIME, System.currentTimeMillis());
      Path logFile = loggingManager.getLogFile();
      map.put(LOG_FILE, logFile != null ? logFile.toString() : "No log file");
      map.put(TEXT, text);
      return map;
   }

   public static Map<String, Object> restartDueToError(String[] args, LoggingManager loggingManager) {
      return create(loggingManager, Type.error, "Restart due to error: " + LoggingManager.getRestartDueToErrorValue(args));
   }

   public enum Type {
      error, info
   }
}
