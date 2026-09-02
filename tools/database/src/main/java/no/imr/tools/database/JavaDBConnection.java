package no.imr.tools.database;

import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class JavaDBConnection implements AutoCloseable {
   private final Path directory;
   private final String databaseName;
   private final DatabaseConnection databaseConnection;

   public JavaDBConnection(ConnectionType connectionType, Path directory, String databaseName, List<Class<? extends BaseDatabaseObject>> databaseClasses) throws IOException {
      this.directory = directory;
      this.databaseName = databaseName;

      JavaDBUtils.prepareToConnect(connectionType, directory, databaseName);
      Configuration configuration = JavaDBUtils.createConfiguration(directory, databaseName, connectionType);
      databaseConnection = new DatabaseConnection(connectionType, configuration, databaseClasses);
   }

   public DatabaseConnection getDatabaseConnection() {
      return databaseConnection;
   }

   @Override
   public void close() {
      databaseConnection.disconnect();
      JavaDBUtils.shutDown(directory, databaseName);
   }
}
