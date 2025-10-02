package no.imr.korona.computation;

import no.imr.korona.Korona;
import no.imr.korona.resources.KoronaResource;
import no.imr.korona.util.KoronaUtils;
import no.imr.korona.viewer.KoronaHelpSystem;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;

final class ModuleEditorMain {
   private ModuleEditorMain() {
   }

   /**
    * For testing the ModuleEditor.
    *
    * @param args specifies the configuration file (optional)
    */
   public static void main(String[] args) {
      Utils.init(args, KoronaResource.KORONA_64);
      SwingUtilities.invokeLater(() -> start(args));
   }

   private static void start(String[] args) {
      Path cdsFile = Utils.getTmpDir().resolve("ModuleEditorMain" + KoronaUtils.CDS_FILE_SUFFIX);
      Log.global.info("Using " + cdsFile);

      Korona korona = new Korona();
      KoronaHelpSystem.createHelpSystem(korona);
      ModuleContainer moduleContainer = new ModuleContainer(korona);
      moduleContainer.getConfigFileSettings().restoreInstallationLocations();
      try {
         Document document = XmlUtils.readDocumentIfExists(cdsFile);
         if (document != null) {
            moduleContainer.fromXml(document.getRootElement());
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error reading " + cdsFile, e);
      }
      if (args.length > 0) {
         Path file = Path.of(args[0]);
         try {
            moduleContainer.readConfiguration(file);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error reading " + file, e);
         }
      }
      boolean ok = new ModuleEditor(moduleContainer, true, null)
            .show()
            .getOK();
      if (ok) {
         try {
            XmlUtils.writeDocument(moduleContainer.toXml(), cdsFile);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error writing " + cdsFile, e);
         }
      }
   }
}
