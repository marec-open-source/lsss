package no.imr.lsss.test;

import no.imr.lsss.LSSS;
import no.imr.lsss.resources.LsssResource;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.swing.GuiUtils;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.file.Path;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Removes all preferences.
 */
@SuppressWarnings("PMD.SystemPrintln")
final class LsssResetMain {
   private LsssResetMain() {
   }

   static void main(String[] args) throws BackingStoreException, IOException {
      Utils.init(args, LsssResource.LSSS_64);
      Path applicationDataDir = LSSS.getApplicationDataDir();
      String preferencesPath = "no/imr";
      int answer = GuiUtils.getNowOrWait(() -> {
         return JOptionPane.showConfirmDialog(null, "Delete?" +
                     "\nPreferences: " + preferencesPath +
                     "\nDirectory: " + applicationDataDir,
               "Reset LSSS", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
      });
      if (answer == JOptionPane.YES_OPTION) {
         removePreferences(Preferences.userRoot().node(preferencesPath));
         FileUtils.deleteRecursively(applicationDataDir);
      }
   }

   private static void removePreferences(Preferences preferences) throws BackingStoreException {
      for (String childName : preferences.childrenNames()) {
         removePreferences(preferences.node(childName));
      }
      for (String key : preferences.keys()) {
         System.out.println("Removing " + preferences.absolutePath() + " " + key);
         preferences.remove(key);
      }
      System.out.println("Removing " + preferences.absolutePath());
      preferences.removeNode();
   }
}
