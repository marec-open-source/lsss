package no.imr.korona.computation.plankton.editor;

import no.imr.korona.Korona;
import no.imr.korona.computation.plankton.PlanktonFileService;
import no.imr.korona.resources.KoronaResource;
import no.imr.korona.viewer.KoronaHelpSystem;
import no.imr.tools.Utils;

import javax.swing.SwingUtilities;
import java.nio.file.Path;

final class PlanktonGUIMain {
   private PlanktonGUIMain() {
   }

   public static void main(String[] args) {
      Utils.init(args, KoronaResource.KORONA_64);
      SwingUtilities.invokeLater(PlanktonGUIMain::start);
   }

   private static void start() {
      Korona korona = new Korona();
      KoronaHelpSystem.createHelpSystem(korona);
      PlanktonFileService configFileService = new PlanktonFileService();
      Path file = configFileService.getInstallationLocation();
      PlanktonGUI.showDialog(null, file, true);
   }
}
