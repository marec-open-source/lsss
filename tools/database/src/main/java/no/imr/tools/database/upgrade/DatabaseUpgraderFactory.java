package no.imr.tools.database.upgrade;

import com.google.common.base.Splitter;
import no.imr.tools.Utils;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.queries.StatelessDatabaseQuery;
import no.imr.tools.io.FileUtils;
import no.imr.tools.upgrade.UpgradeException;
import no.imr.tools.upgrade.Upgrader;
import no.imr.tools.upgrade.UpgraderFactory;
import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CancellationException;

public final class DatabaseUpgraderFactory implements UpgraderFactory<DatabaseConnection> {
   private final @Nullable Component referenceComponent;
   private final String resourceDirectory;

   public DatabaseUpgraderFactory(@Nullable Component referenceComponent, String resourceDirectory) {
      this.referenceComponent = referenceComponent;
      this.resourceDirectory = resourceDirectory;
   }

   @Override
   public Upgrader<DatabaseConnection> createUpgrader(String fromVersion) {
      return databaseConnection -> upgrade(fromVersion, databaseConnection);
   }

   private DatabaseConnection upgrade(String fromVersion, DatabaseConnection databaseConnection) throws UpgradeException {
      String connectionUrl = databaseConnection.executeStatelessValuedQuery(session -> {
         return session.<@Nullable String>doReturningWork(connection -> connection.getMetaData().getURL());
      });
      if (connectionUrl == null) {
         throw new UpgradeException("Unable to determine database connection URL");
      }
      String[] connectionUrlParts = connectionUrl.split(":", 3);
      if (connectionUrlParts.length < 3) {
         throw new UpgradeException("Unable to parse database connection URL: " + connectionUrl);
      }
      String databaseType = connectionUrlParts[1];

      String upgradeScript = getUpgradeScript(fromVersion, databaseType);

      if (!List.of("hsqldb", "derby", "postgresql").contains(databaseType)) {
         DatabaseUpgradeDialog databaseUpgradeDialog = new DatabaseUpgradeDialog(upgradeScript);
         databaseUpgradeDialog.show(referenceComponent);
         if (!databaseUpgradeDialog.isOK()) {
            throw new CancellationException();
         }
         upgradeScript = databaseUpgradeDialog.getSQL();
      }

      for (String line : Splitter.on('\n').trimResults().omitEmptyStrings().split(upgradeScript)) {
         if (line.startsWith("--")) {
            continue;
         }
         try {
            databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.nativeSql(line));
         } catch (Exception e) {
            throw new UpgradeException("Error executing SQL upgrading from version: " + fromVersion + ", line: " + line, e);
         }
      }
      return databaseConnection;
   }

   private String getUpgradeScript(String fromVersion, String databaseType) throws UpgradeException {
      URL url = getUpgradeScriptURL(fromVersion + "-" + databaseType);
      if (url == null) {
         url = getUpgradeScriptURL(fromVersion);
      }
      if (url == null) {
         throw new UpgradeException("Cannot find SQL upgrade script from version " + fromVersion);
      }
      try {
         return FileUtils.readAsString(url, Utils.UTF_8);
      } catch (Exception e) {
         throw new UpgradeException("Error getting SQL script for upgrading from version: " + fromVersion, e);
      }
   }

   private @Nullable URL getUpgradeScriptURL(String fromVersion) {
      return DatabaseUpgraderFactory.class.getClassLoader().getResource(resourceDirectory + "/FromVersion" + fromVersion + ".sql");
   }
}
