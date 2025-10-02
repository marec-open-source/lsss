package no.imr.lsss.database;

import no.imr.lsss.database.types.NoGuiDatabasePlugin;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.HsqldbUtils;
import no.imr.tools.parameter.Name;
import org.hibernate.cfg.Configuration;

import java.nio.file.Path;

public final class HsqldbTestDatabasePlugin extends NoGuiDatabasePlugin {
   private final Path dir;
   private final String databaseName;

   public HsqldbTestDatabasePlugin(Path dir, String databaseName) {
      super(new Name("HsqldbTestDatabasePlugin"));

      this.dir = dir;
      this.databaseName = databaseName;
   }

   @Override
   public Configuration getConfiguration(ConnectionType connectionType) {
      return HsqldbUtils.createConfiguration(dir, databaseName);
   }

   @Override
   public void shutDown() {
      HsqldbUtils.shutDown(dir, databaseName);
   }
}
