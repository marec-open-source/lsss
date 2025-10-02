package no.imr.korona.config;

import no.imr.korona.util.KoronaPreferences;
import no.imr.tools.ProgressHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;

import java.util.prefs.Preferences;

public final class ConfigFileCopier {
   private static final String PREFERENCE_CONFIG_FILE_LAST_MODIFIED = "configFileLastModified";
   private static final String PREFERENCE_LAST_COPY_TIME = "lastCopyTime";

   private ConfigFileCopier() {
   }

   static void copy(KoronaFilesToCopy filesToCopy, ProgressHandler progressHandler, AsyncHandle asyncHandle) {
      updateLastModified(filesToCopy);
      if (filesToCopy.getFilesToCopy().isEmpty()) {
         return;
      }
      getPreferences().putLong(PREFERENCE_LAST_COPY_TIME, System.currentTimeMillis());
      FileUtils.copyAllRecursively(filesToCopy.getFilesToCopy(), progressHandler, asyncHandle);
   }

   static void updateLastModified(KoronaFilesToCopy filesToCopy) {
      getPreferences().putLong(PREFERENCE_CONFIG_FILE_LAST_MODIFIED, filesToCopy.getNextLastModifiedSource());
   }

   public static long getLastModified() {
      return getPreferences().getLong(PREFERENCE_CONFIG_FILE_LAST_MODIFIED, 0);
   }

   public static long getLastCopyTime() {
      return getPreferences().getLong(PREFERENCE_LAST_COPY_TIME, 0);
   }

   public static Preferences getPreferences() {
      return KoronaPreferences.node("config");
   }
}
