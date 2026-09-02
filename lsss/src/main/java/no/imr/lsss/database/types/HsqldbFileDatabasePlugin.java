package no.imr.lsss.database.types;

import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.HsqldbUtils;
import no.imr.tools.parameter.Name;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.nio.file.Path;

public final class HsqldbFileDatabasePlugin extends NoGuiDatabasePlugin {
   private final Path dir;
   private final String databaseName;

   public HsqldbFileDatabasePlugin(Name name, Path dir, String databaseName) {
      super(name);

      this.dir = dir;
      this.databaseName = databaseName;
   }

   @Override
   public Configuration getConfiguration(ConnectionType connectionType) {
      return HsqldbUtils.createConfiguration(dir, databaseName, "sa", "",
            HsqldbUtils.BINARY_CONNECTION_PROPERTIES);
   }

   @Override
   public void prepareToConnect(ConnectionType connectionType) throws IOException {
      HsqldbUtils.prepareToConnect(connectionType, dir, databaseName);
   }

   @Override
   public void shutDown() {
      HsqldbUtils.shutDown(dir, databaseName);
   }
}
