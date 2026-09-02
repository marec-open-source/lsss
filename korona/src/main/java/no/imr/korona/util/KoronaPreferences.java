package no.imr.korona.util;

import java.util.prefs.Preferences;

public final class KoronaPreferences {
   private KoronaPreferences() {
   }

   public static Preferences node(String name) {
      return Preferences.userRoot().node("no/marec/korona").node(name);
   }
}
