package no.imr.lsss.database;

import no.imr.lsss.database.types.TestDatabasePlugin;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.DatabaseConnection;
import org.junit.jupiter.api.Test;

final class DefaultDatabaseTest {
   @Test
   void test() {
      createTmpDatabase();
   }

   private static void createTmpDatabase() {
      TestDatabasePlugin databasePlugin = TestDatabasePlugin.newDatabasePlugin();
      DatabaseConnection databaseConnection = new DatabaseConnection(
            ConnectionType.INITIALIZE, databasePlugin.getConfiguration(ConnectionType.INITIALIZE), LsssDatabaseContent.DATABASE_CLASSES);
      new LsssDatabaseContent().copyDefaultDataIntoTables(databaseConnection);
      databaseConnection.disconnect();
      databasePlugin.shutDown();
   }
}
