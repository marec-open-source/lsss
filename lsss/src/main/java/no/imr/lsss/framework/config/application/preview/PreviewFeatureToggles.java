package no.imr.lsss.framework.config.application.preview;

import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BooleanParameter;

import java.util.List;
import java.util.logging.Level;
import java.util.prefs.Preferences;

public final class PreviewFeatureToggles {

   private final ChangeManager changeManager = new ChangeManager();
   private boolean needSave;

   PreviewFeatureToggles() {
      load();
      Listener.of(changeManager).addTo(getParameters());
      changeManager.addListener(() -> needSave = true);
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public List<BooleanParameter> getParameters() {
      return List.of(
      );
   }

   private static Preferences getPreferences() {
      return Preferences.userRoot().node("/no/marec/lsss/preview");
   }

   void load() {
      Preferences preferences = getPreferences();
      getParameters().forEach(parameter -> {
         boolean value = preferences.getBoolean(parameter.getPersistentName(), false);
         parameter.setBooleanValue(value);
      });
   }

   void save() {
      if (!needSave) {
         return;
      }
      Preferences preferences = getPreferences();
      try {
         preferences.clear();
         getParameters().forEach(parameter -> {
            preferences.putBoolean(parameter.getPersistentName(), parameter.getBooleanValue());
         });
         preferences.flush();
         needSave = false;
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error saving preferences " + preferences.name(), e);
      }
   }
}
