package no.imr.lsss.database.types;

import com.google.common.html.HtmlEscapers;
import no.imr.lsss.LSSS;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.HsqldbUtils;
import no.imr.tools.io.FilePredicates;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.StringParameter;
import no.marec.lsss.api.util.parameters.ObjectParameterValue;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Plugin for connecting to a HSQLDB database.
 */
public final class HsqldbDatabasePlugin extends AbstractDatabasePlugin {
   private static final String DEFAULT_NAME = "lsss";
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
            return FileUtils.listFiles(dir, new AsyncHandle(), FilePredicates.endsWith(HsqldbUtils.SCRIPT_FILE_SUFFIX)).stream()
                  .map(file -> {
                     String fileName = file.getFileName().toString();
                     return fileName.substring(0, fileName.length() - HsqldbUtils.SCRIPT_FILE_SUFFIX.length());
                  })
                  .sorted()
                  .toList();
         } catch (IOException _) {
            return List.of();
         }
      }
   };

   public final ObjectParameter<HsqldbStorageFormat> storageFormat = new ObjectParameter<>(
         new Name("StorageFormat", "Storage format"),
         HsqldbStorageFormat.BINARY, HsqldbStorageFormat.values(),
         "Storage format when creating tables");

   public final FileParameter directory = new FileParameter(
         new Name("Directory"),
         DEFAULT_DIRECTORY, FileParameter.Mode.DIRECTORY,
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
            storageFormat,
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
            The database tables can be stored either as binary files or as text files containing SQL.<br>
            Storing database tables as SQL might be useful for debugging purposes,
            but may not be suitable for large databases used in production.
            <h4>The connected database</h4>
            """
            + getConnectionInfo();
   }

   private String getConnectionInfo() {
      DatabaseConnection databaseConnection = getLSSS().getDatabaseManager().getGlobalDatabaseConnectionManager().getDatabaseConnection();
      if (!databaseConnection.isConnected()) {
         return "Not connected.";
      }
      Set<String> hsqldbTypes;
      try {
         hsqldbTypes = HsqldbUtils.getHsqldbTypes(databaseConnection);
      } catch (Exception e) {
         return toWarning("Error examining the connected database:<br>" + HtmlEscapers.htmlEscaper().escape(e.toString()));
      }
      if (hsqldbTypes.equals(Set.of("CACHED"))) {
         return "Tables are stored as binary files.";
      } else {
         return toWarning("Tables are stored as text files.");
      }
   }

   private static String toWarning(String s) {
      return "<div style='color:red;'>" + s + "</div>";
   }

   @Override
   public boolean isConfigurationValid() {
      return !databaseName.getValue().isEmpty()
            && directory.getFile() != null;
   }

   @Override
   public boolean canConnect() {
      return isConfigurationValid()
            && HsqldbUtils.isHsqldbDatabase(getDir(), databaseName.getValue());
   }

   @Override
   public Configuration getConfiguration(ConnectionType connectionType) {
      String additionalProperties = switch (storageFormat.getValue()) {
         case BINARY -> HsqldbUtils.BINARY_CONNECTION_PROPERTIES;
         case TEXT -> "";
      };
      if (connectionType == ConnectionType.CONNECT) {
         additionalProperties += ";ifexists=true";
      }
      return HsqldbUtils.createConfiguration(getDir(), databaseName.getValue(), userName.getValue(), password.getValue(), additionalProperties);
   }

   @Override
   public void prepareToConnect(ConnectionType connectionType) throws IOException {
      HsqldbUtils.prepareToConnect(connectionType, getDir(), databaseName.getValue());
   }

   @Override
   public void shutDown() {
      HsqldbUtils.shutDown(getDir(), databaseName.getValue());
   }

   private Path getDir() {
      return directory.getValue().orElseThrow(() -> new IllegalStateException("No directory"));
   }

   public enum HsqldbStorageFormat implements ObjectParameterValue {
      BINARY, TEXT;

      @Override
      public String getDisplayLabel() {
         return switch (this) {
            case BINARY -> "Binary files";
            case TEXT -> "Text files (SQL)";
         };
      }
   }
}
