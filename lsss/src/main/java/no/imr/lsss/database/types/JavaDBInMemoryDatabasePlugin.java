package no.imr.lsss.database.types;

import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.JavaDBUtils;
import no.imr.tools.parameter.Name;
import org.hibernate.cfg.Configuration;

/**
 * In-memory database for testing.
 */
final class JavaDBInMemoryDatabasePlugin extends NoGuiDatabasePlugin {
   private final String databaseName;

   JavaDBInMemoryDatabasePlugin(String databaseName) {
      super(new Name("JavaDBInMemoryDatabasePlugin"));

      this.databaseName = databaseName;
   }

   @Override
   public Configuration getConfiguration(ConnectionType connectionType) {
      return JavaDBUtils.createInMemoryConfiguration(databaseName);
   }

   @Override
   public void shutDown() {
      JavaDBUtils.dropInMemoryDatabase(databaseName);
   }
}
