package no.imr.tools.database.upgrade;

import no.imr.tools.Version;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.DatabaseContent;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.upgrade.UpgradeEngine;
import no.imr.tools.upgrade.UpgradeException;
import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.util.concurrent.CancellationException;

/**
 * DatabaseContent with logic for database upgrades.
 */
public abstract class UpgradableDatabaseContent extends DatabaseContent {
   private final String name;

   protected UpgradableDatabaseContent(String name) {
      this.name = name;
   }

   public abstract String getVersionOf(DatabaseConnection databaseConnection);

   public abstract String getTargetVersion();

   public abstract UpgradeEngine<DatabaseConnection> getUpgradeEngine(@Nullable Component referenceComponent);

   @Override
   public UpgradeResult doUpgradeIfNecessary(DatabaseConnection databaseConnection, @Nullable Component referenceComponent, boolean interactiveMode) throws UpgradeException {
      String currentVersion = getVersionOf(databaseConnection);

      if (currentVersion.equals(getTargetVersion())) {
         return UpgradeResult.OK;
      }

      if (new Version(currentVersion).isNewerThan(new Version(getTargetVersion()))) {
         throw new UpgradeException("Your current " + name + " database version " + currentVersion + " is too new.\n" +
               "The database version compatible with this release is " + getTargetVersion() + " and older.");
      }

      Log.global.info(name + " database version is too old: " + currentVersion);
      if (interactiveMode) {
         if (currentVersion.equals("0")) {
            int answer = GuiUtils.showOptionDialog(referenceComponent, "Database upgrade",
                  "The " + name + " database component does not exist.\n\n" +
                        "Do you want to create the database component?\n\n" +
                        "It is recommended that you create a backup of the existing database before upgrading.\n\n",
                  new String[]{"Create", "Cancel"});
            if (answer != 0) {
               return UpgradeResult.CANCELLED;
            }
         } else {
            int answer = GuiUtils.showOptionDialog(referenceComponent, "Database upgrade",
                  name + " database version " + currentVersion + " is too old. " +
                        "Required version is " + getTargetVersion() + ".\n\n" +
                        "Do you want to upgrade the database now?\n\n" +
                        "It is recommended that you create a backup of the existing database before upgrading.\n\n",
                  new String[]{"Upgrade", "Cancel"});
            if (answer != 0) {
               return UpgradeResult.CANCELLED;
            }
         }
      }

      try {
         getUpgradeEngine(referenceComponent).upgrade(databaseConnection);
         String upgradedVersion = getVersionOf(databaseConnection);
         Log.global.info("Upgraded " + name + " database from " + currentVersion + " to " + upgradedVersion);
      } catch (CancellationException _) {
         return UpgradeResult.CANCELLED;
      }
      return UpgradeResult.OK;
   }
}
