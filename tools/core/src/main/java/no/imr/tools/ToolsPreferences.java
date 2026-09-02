package no.imr.tools;

import no.imr.tools.adm.ApplicationInfo;

import java.util.prefs.Preferences;

public final class ToolsPreferences {
   private ToolsPreferences() {
   }

   public static Preferences node(String name) {
      return Preferences.userRoot().node("no/marec/tools").node(name);
   }

   public static Preferences node(String name, ApplicationInfo applicationInfo) {
      return node(name).node(applicationInfo.applicationDataSubDir());
   }
}
