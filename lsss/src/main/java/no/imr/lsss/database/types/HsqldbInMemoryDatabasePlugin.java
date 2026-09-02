package no.imr.lsss.database.types;

import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.HsqldbUtils;
import no.imr.tools.parameter.Name;
import org.hibernate.cfg.Configuration;

/**
 * In-memory database for testing.
 */
final class HsqldbInMemoryDatabasePlugin extends NoGuiDatabasePlugin {
   private final String databaseName;

   HsqldbInMemoryDatabasePlugin(String databaseName) {
      super(new Name("HsqldbInMemoryDatabasePlugin"));

      this.databaseName = databaseName;
   }

   @Override
   public Configuration getConfiguration(ConnectionType connectionType) {
      return HsqldbUtils.createInMemoryConfiguration(databaseName);
   }

   @Override
   public void shutDown() {
      HsqldbUtils.shutDown(HsqldbUtils.inMemoryConnectionUrl(databaseName));
   }
}
