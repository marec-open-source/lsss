package no.imr.lsss.database.types;

import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.JavaDBUtils;
import no.imr.tools.parameter.Name;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.nio.file.Path;

public final class JavaDBFileDatabasePlugin extends NoGuiDatabasePlugin {
   private final Path dir;
   private final String databaseName;

   public JavaDBFileDatabasePlugin(Name name, Path dir, String databaseName) {
      super(name);

      this.dir = dir;
      this.databaseName = databaseName;
   }

   @Override
   public Configuration getConfiguration(ConnectionType connectionType) {
      return JavaDBUtils.createConfiguration(dir, databaseName, connectionType);
   }

   @Override
   public void prepareToConnect(ConnectionType connectionType) throws IOException {
      JavaDBUtils.prepareToConnect(connectionType, dir, databaseName);
   }

   @Override
   public void shutDown() {
      JavaDBUtils.shutDown(dir, databaseName);
   }
}
