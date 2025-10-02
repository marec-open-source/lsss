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
   private static final String DEFAULT_NAME = "lsss_DB";
   private static final Path DEFAULT_DIRECTORY = LSSS.getApplicationDataDir().resolve("database");

   public final StringParameter databaseName = new StringParameter(
         new Name("DatabaseName", "Database name"),
         DEFAULT_NAME,
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
         } catch (IOException e) {
            return List.of();
         }
      }
   };

   public final FileParameter directory = new FileParameter(
         new Name("Directory"),
         DEFAULT_DIRECTORY, FileParameter.Mode.DIRECTORY,
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

   public void resetInvalidSettings() {
      if (databaseName.getValue().isBlank()) {
         databaseName.setValue(DEFAULT_NAME);
      }
      if (directory.getFile() == null) {
         directory.setFile(DEFAULT_DIRECTORY);
      }
   }

   @Override
   public String getDescription() {
      return """
            This database does not require a database server.<br>
            The database tables are stored in files.
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
            && JavaDBUtils.isJavaDBDirectory(getDir().resolve(databaseName.getValue()));
   }

   @Override
   public Configuration getConfiguration(ConnectionType connectionType) {
      return JavaDBUtils.createConfiguration(getDir(), databaseName.getValue(), connectionType, userName.getValue(), password.getValue());
   }

   @Override
   public void shutDown() {
      JavaDBUtils.shutDown(getDir(), databaseName.getValue());
   }

   public Path getDir() {
      return directory.getValue().orElseThrow(() -> new IllegalStateException("No directory"));
   }
}
