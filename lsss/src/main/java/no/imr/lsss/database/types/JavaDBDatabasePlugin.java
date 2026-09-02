package no.imr.lsss.database.types;

import no.imr.lsss.LSSS;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.JavaDBUtils;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Plugin for connecting to a JavaDB/Derby database.
 */
public final class JavaDBDatabasePlugin extends AbstractDatabasePlugin {
   public final StringParameter databaseName = new StringParameter(
         new Name("DatabaseName", "Database name"),
         "lsss_DB",
         "Name of the database") {
      @Override
      public List<String> getSuggestedValues() {
         Path dir = directory.getFile();
         if (dir == null) {
            return List.of();
         }
         try {
            return FileUtils.listFilesWithAttributes(dir, FileInfo::isDirectory).stream()
                  .map(FileInfo::getFileName)
                  .sorted()
                  .toList();
         } catch (IOException _) {
            return List.of();
         }
      }
   };

   public final FileParameter directory = new FileParameter(
         new Name("Directory"),
         LSSS.getApplicationDataDir().resolve("database"), FileParameter.Mode.DIRECTORY,
         "Location of the database files");

   private final StringParameter userName = new StringParameter(
         new Name("UserName", "Username"));

   public JavaDBDatabasePlugin(LSSS lsss) {
      super(new Name("JavaDB"), lsss);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            databaseName,
            directory,
            userName,
            password,
            savePassword
      );
   }

   @Override
   public String getDescription() {
      return """
            This database does not require a database server.<br>
            The database tables are stored in files.
            <br><br>
            <p style='color:red;'><b>
            WARNING: The JavaDB database type is deprecated.
            Support for JavaDB will be dropped in a future version of LSSS.
            As an alternative, please use the HSQLDB database type.
            </b></p>
            """;
   }

   @Override
   public boolean isConfigurationValid() {
      return !databaseName.getValue().isEmpty()
            && directory.getFile() != null;
   }

   @Override
   public boolean canConnect() {
      return isConfigurationValid()
            && JavaDBUtils.isJavaDBDatabase(getDir(), databaseName.getValue());
   }

   @Override
   public Configuration getConfiguration(ConnectionType connectionType) {
      return JavaDBUtils.createConfiguration(getDir(), databaseName.getValue(), connectionType, userName.getValue(), password.getValue());
   }

   @Override
   public void prepareToConnect(ConnectionType connectionType) throws IOException {
      JavaDBUtils.prepareToConnect(connectionType, getDir(), databaseName.getValue());
   }

   @Override
   public void shutDown() {
      JavaDBUtils.shutDown(getDir(), databaseName.getValue());
   }

   public Path getDir() {
      return directory.getValue().orElseThrow(() -> new IllegalStateException("No directory"));
   }
}
