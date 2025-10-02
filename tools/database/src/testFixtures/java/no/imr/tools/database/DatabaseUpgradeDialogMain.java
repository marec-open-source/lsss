package no.imr.tools.database;

import no.imr.tools.ResourceUtils;
import no.imr.tools.database.upgrade.DatabaseUpgradeDialog;

import javax.swing.SwingUtilities;

@SuppressWarnings("PMD.SystemPrintln")
final class DatabaseUpgradeDialogMain {
   private DatabaseUpgradeDialogMain() {
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> {
         String resource = args.length > 0 ? args[0] : "no/imr/lsss/resources/databaseUpgrade/FromVersion3.sql";
         String sql = ResourceUtils.getString(resource);
         DatabaseUpgradeDialog databaseUpgradeDialog = new DatabaseUpgradeDialog(sql);
         databaseUpgradeDialog.show(null);
         System.out.println("OK: " + databaseUpgradeDialog.isOK());
         System.out.println(databaseUpgradeDialog.getSQL());
      });
   }
}
