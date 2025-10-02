package no.imr.lsss.framework.config.application;

import no.imr.korona.Korona;
import no.imr.lsss.resources.LsssResource;
import no.imr.tools.Utils;

import javax.swing.SwingUtilities;
import java.nio.file.Path;

@SuppressWarnings("PMD.SystemPrintln")
final class MoveConfigFilesDialogMain {
   private MoveConfigFilesDialogMain() {
   }

   public static void main(String[] args) {
      Utils.init(args, LsssResource.LSSS_64);
      Korona korona = new Korona();
      SwingUtilities.invokeLater(() -> {
         Path dir = korona.getKoronaSettings().getKoronaConfigDir().getFile();
         if (dir == null) {
            System.out.println("No KoronaConfigDir");
            return;
         }
         MoveConfigFilesDialog dialog = new MoveConfigFilesDialog(null, korona.createConfigFileSettings(), dir, false);
         System.out.println("UpdateLsssConfigSelected = " + dialog.isUpdateLsssConfigSelected());
      });
   }
}
