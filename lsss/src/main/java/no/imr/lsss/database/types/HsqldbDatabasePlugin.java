package no.imr.lsss.database.types;

import no.imr.lsss.LSSS;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.HsqldbUtils;
import no.imr.tools.io.FilePredicates;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Plugin for connecting to a HSQLDB database.
 */
public final class HsqldbDatabasePlugin extends AbstractDatabasePlugin {
   private final StringParameter databaseName = new StringParameter(
         new Name("DatabaseName", "Database name"),
         "lsss",
         "Name of the database") {
      @Override
      public List<String> getSuggestedValues() {
         Path dir = directory.getFile();
         if (dir == null) {
            return List.of();
         }
         try {
            return FileUtils.listFiles(dir, new AsyncHandle(), FilePredicates.endsWith(HsqldbUtils.SCRIPT_FILE_SUFFIX)).stream()
                  .map(file -> {
                     String fileName = file.getFileName().toString();
                     return fileName.substring(0, fileName.length() - HsqldbUtils.SCRIPT_FILE_SUFFIX.length());
                  })
                  .sorted()
                  .toList();
         } catch (IOException e) {
            return List.of();
         }
      }
   };

   private final FileParameter directory = new FileParameter(
         new Name("Directory"),
         LSSS.getApplicationDataDir().resolve("database"), FileParameter.Mode.DIRECTORY,
         "Location of the database files");

   private final StringParameter userName = new StringParameter(
         new Name("UserName", "Username"),
         "sa");

   public HsqldbDatabasePlugin(LSSS lsss) {
      super(new Name("HSQLDB"), lsss);
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
            This database does not require a database server.
            The database tables are stored in files as sql.
            This might be useful for debugging purposes.
            <div style='color:red;'>This database may not be not suitable for large databases and should not be used in production.</div>
            <div style='color:red;'>LSSS 1.9.2 contains a version of the HSQLDB library that may not be compatible with earlier releases.</div>
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
            && Files.isRegularFile(getDir().resolve(databaseName.getValue() + HsqldbUtils.SCRIPT_FILE_SUFFIX));
   }

   @Override
   public Configuration getConfiguration(ConnectionType connectionType) {
      return HsqldbUtils.createConfiguration(getDir(), databaseName.getValue(), userName.getValue(), password.getValue());
   }

   @Override
   public void shutDown() {
      HsqldbUtils.shutDown(getDir(), databaseName.getValue());
   }

   public Path getDir() {
      return directory.getValue().orElseThrow(() -> new IllegalStateException("No directory"));
   }
}
