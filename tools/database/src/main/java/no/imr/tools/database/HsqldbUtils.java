package no.imr.tools.database;

import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.StatelessDatabaseQuery;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import org.hibernate.cfg.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class HsqldbUtils {
   private static final String JDBC_DRIVER = "org.hsqldb.jdbc.JDBCDriver";
   private static final String JDBC_FILE_PREFIX = "jdbc:hsqldb:file:";

   public static final String SCRIPT_FILE_SUFFIX = ".script";
   public static final String BINARY_CONNECTION_PROPERTIES = ";hsqldb.default_table_type=cached;hsqldb.script_format=3";

   static {
      System.setProperty("hsqldb.reconfig_logging", "false");
   }

   private HsqldbUtils() {
   }

   public static void shutDown(Path dir, String databaseName) {
      shutDown(getConnectionURL(dir, databaseName, ""));
   }

   public static void shutDown(String connectionURL) {
      Log.global.info("Shutting down " + connectionURL);
      try (Connection connection = DriverManager.getConnection(connectionURL);
           Statement statement = connection.createStatement()) {
         statement.execute("SHUTDOWN");
      } catch (SQLException e) {
         Log.global.log(Level.WARNING, "Error shutting down HSQLDB embedded database", e);
      }
   }

   private static String getConnectionURL(Path dir, String databaseName, String additionalProperties) {
      String path = dir.toAbsolutePath().toString().replace(File.separatorChar, '/');
      if (!path.endsWith("/")) {
         path = path + "/";
      }
      return JDBC_FILE_PREFIX + path + databaseName + additionalProperties;
   }

   public static Configuration createConfiguration(Path dir, String databaseName) {
      return createConfiguration(dir, databaseName, "sa", "", "");
   }

   public static Configuration createConfiguration(Path dir, String databaseName,
                                                   String username, String password,
                                                   String additionalProperties) {
      return DatabaseUtils.createConfiguration(
            JDBC_DRIVER,
            getConnectionURL(dir, databaseName, additionalProperties),
            username,
            password);
   }

   public static String inMemoryConnectionUrl(String databaseName) {
      return "jdbc:hsqldb:mem:" + databaseName;
   }

   public static Configuration createInMemoryConfiguration(String databaseName) {
      return DatabaseUtils.createConfiguration(JDBC_DRIVER, inMemoryConnectionUrl(databaseName), "sa", "");
   }

   public static void prepareToConnect(ConnectionType connectionType, Path dir, String databaseName) throws IOException {
      if (connectionType == ConnectionType.INITIALIZE) {
         FileUtils.createDirectories(dir);
         deleteDatabaseFiles(dir, databaseName);
      }
   }

   public static boolean doesSomeDatabaseFileExist(Path dir, String databaseName) {
      return Files.exists(dir.resolve(databaseName + ".data"))
            || Files.exists(dir.resolve(databaseName + ".properties"))
            || Files.exists(dir.resolve(databaseName + SCRIPT_FILE_SUFFIX));
   }

   public static void deleteDatabaseFiles(Path dir, String databaseName) throws IOException {
      Files.deleteIfExists(dir.resolve(databaseName + ".data"));
      Files.deleteIfExists(dir.resolve(databaseName + ".properties"));
      Files.deleteIfExists(dir.resolve(databaseName + SCRIPT_FILE_SUFFIX));
   }

   public static boolean isHsqldbDatabase(Path dir, String databaseName) {
      return Files.isRegularFile(dir.resolve(databaseName + ".properties"))
            && Files.isRegularFile(dir.resolve(databaseName + SCRIPT_FILE_SUFFIX));
   }

   public static Set<String> getHsqldbTypes(DatabaseConnection databaseConnection) {
      return databaseConnection.executeStatelessValuedQuery(session -> {
         String sql = "select HSQLDB_TYPE from INFORMATION_SCHEMA.SYSTEM_TABLES where TABLE_SCHEM='PUBLIC'";
         return session.createNativeQuery(sql, String.class)
               .setReadOnly(true)
               .stream()
               .collect(Collectors.toSet());
      });
   }

   public static void importTableFromTextFile(DatabaseConnection databaseConnection, Class<? extends BaseDatabaseObject> clazz, Path file) {
      String tableName = DatabaseUtils.getTableName(clazz);
      String path = toEscapedPath(file);
      databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.nativeSql(List.of(
            "DROP TABLE IF EXISTS import_tmp;",
            "CREATE TEXT TABLE import_tmp (LIKE " + tableName + ");",
            "SET TABLE import_tmp SOURCE '" + path + ";encoding=UTF-8;fs=\\semi';",
            "INSERT INTO " + tableName + " SELECT * FROM import_tmp;",
            "SET TABLE import_tmp SOURCE OFF;",
            "DROP TABLE import_tmp;"
      )));
   }

   public static void dumpTableToTextFile(DatabaseConnection databaseConnection, Class<? extends BaseDatabaseObject> clazz,
                                          List<Map.Entry<DatabaseColumn, Integer>> criteria, Path file) throws IOException {
      Files.deleteIfExists(file);
      String whereClause = criteria.isEmpty() ? "" :
            criteria.stream()
                  .map(e -> e.getKey().name() + "=" + e.getValue())
                  .collect(Collectors.joining(" and ", "where ", ""));
      String tableName = DatabaseUtils.getTableName(clazz);
      String path = toEscapedPath(file);
      databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.nativeSql(List.of(
            "DROP TABLE IF EXISTS export_tmp;",
            "CREATE TEXT TABLE export_tmp (LIKE " + tableName + ");",
            "SET TABLE export_tmp SOURCE '" + path + ";encoding=UTF-8;fs=\\semi';",
            "INSERT INTO export_tmp SELECT * FROM " + tableName + " " + whereClause + ";",
            "SET TABLE export_tmp SOURCE OFF;",
            "DROP TABLE export_tmp;"
      )));
   }

   private static String toEscapedPath(Path file) {
      return FileUtils.toSlashSeparatorChar(file.toAbsolutePath().toString())
            .replace("'", "''");
   }
}
