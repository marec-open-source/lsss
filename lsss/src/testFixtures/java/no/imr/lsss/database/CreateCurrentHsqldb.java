package no.imr.lsss.database;

import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.HsqldbUtils;
import no.imr.tools.logging.Log;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class CreateCurrentHsqldb {
   private CreateCurrentHsqldb() {
   }

   public static void main(String[] args) throws IOException {
      Log.init();
      Path dir = DatabaseTestUtils.upgradeTestDataDir().resolve(LsssDatabaseContent.VERSION_VALUE);
      if (Files.exists(dir)) {
         throw new IllegalStateException("Directory already exists: " + dir);
      }
      Configuration configuration = HsqldbUtils.createConfiguration(dir, "lsss", "sa", "");
      DatabaseConnection databaseConnection = new DatabaseConnection(
            ConnectionType.INITIALIZE, configuration, LsssDatabaseContent.DATABASE_CLASSES);
      LsssDatabaseContent.loadDefaultContent().save(databaseConnection, LsssDatabaseUtils::isSystemClass);
      databaseConnection.disconnect();
      HsqldbUtils.shutDown(dir, "lsss");
   }
}
