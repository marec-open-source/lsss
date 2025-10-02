package no.imr.tools;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Factory for preferences kept in memory only.
 */
public final class InMemoryPreferencesFactory implements PreferencesFactory {
   private final InMemoryPreferences systemRoot = new InMemoryPreferences(null, "");
   private final InMemoryPreferences userRoot = new InMemoryPreferences(null, "");

   public static void install() {
      System.setProperty("java.util.prefs.PreferencesFactory", InMemoryPreferencesFactory.class.getName());
      checkInstallation();
   }

   @SuppressWarnings("PMD.SystemPrintln")
   public static void checkInstallation() {
      if (!(Preferences.userRoot() instanceof InMemoryPreferences)) {
         System.err.println("Not using InMemoryPreferences. Run with -Djava.util.prefs.PreferencesFactory=" + InMemoryPreferencesFactory.class.getName());
         System.exit(1);
      }
   }

   public InMemoryPreferencesFactory() {
   }

   @Override
   public Preferences systemRoot() {
      return systemRoot;
   }

   @Override
   public Preferences userRoot() {
      return userRoot;
   }

   /**
    * Preferences kept in memory only.
    */
   private static final class InMemoryPreferences extends AbstractPreferences {
      private final Map<String, String> map = new HashMap<>();
      private final Map<String, InMemoryPreferences> nodes = new HashMap<>();

      private InMemoryPreferences(@Nullable InMemoryPreferences parent, String name) {
         super(parent, name);
      }

      @Override
      protected void putSpi(String key, String value) {
         map.put(key, value);
      }

      @Override
      protected @Nullable String getSpi(String key) {
         return map.get(key);
      }

      @Override
      protected void removeSpi(String key) {
         map.remove(key);
      }

      @Override
      protected void removeNodeSpi() {
         map.clear();
         nodes.clear();
         ((InMemoryPreferences) parent()).nodes.remove(name());
      }

      @Override
      protected String[] keysSpi() {
         return map.keySet().toArray(new String[0]);
      }

      @Override
      protected String[] childrenNamesSpi() {
         return nodes.keySet().toArray(new String[0]);
      }

      @Override
      protected AbstractPreferences childSpi(String name) {
         return nodes.computeIfAbsent(name, k -> new InMemoryPreferences(this, k));
      }

      @Override
      protected void syncSpi() {
      }

      @Override
      protected void flushSpi() {
      }
   }
}
