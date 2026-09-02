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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class JavaDBUtils {
   private static final String JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
   private static final String JDBC_FILE_PREFIX = "jdbc:derby:";

   static {
      System.setProperty("derby.stream.error.field", "java.lang.System.err");
   }

   private JavaDBUtils() {
   }

   public static void shutDown(Path dir, String databaseName) {
      shutDown(getConnectionURL(dir, databaseName, ConnectionType.CONNECT));
   }

   public static void shutDown(String connectionURL) {
      try (Connection connection = DriverManager.getConnection(connectionURL + ";shutdown=true")) {
         assert connection.isClosed();
      } catch (SQLException e) {
         // JavaDB shutdown throws an exception when successful, see documentation for expected state.
         if (e.getSQLState().equals("08006")) {
            Log.global.info(e.getMessage());
         } else {
            Log.global.log(Level.WARNING, "Error shutting down JavaDB embedded database", e);
         }
      }
   }

   public static String getConnectionURL(Path dir, String databaseName, ConnectionType connectionType) {
      String path = dir.toAbsolutePath().toString().replace(File.separatorChar, '/');
      if (!path.endsWith("/")) {
         path = path + "/";
      }
      String connectionString = JDBC_FILE_PREFIX + path + databaseName;
      if (connectionType == ConnectionType.INITIALIZE) {
         connectionString += ";create=true";
         // Rolf: log-file on different disk "T", but do not seem to improve speed:
         // connectionString += ";create=true; logDevice=T:/javaDB_log";
      }
      return connectionString;
   }

   public static Configuration createConfiguration(Path dir, String databaseName, ConnectionType connectionType) {
      return createConfiguration(dir, databaseName, connectionType, "", "");
   }

   public static Configuration createConfiguration(Path dir, String databaseName, ConnectionType connectionType,
                                                   String username, String password) {
      //Settings prior to 2009.09.25 (until LSSS-1.3.2):
      //System.setProperty("derby.storage.pageCacheSize", "2500");  //Default: 1000

      // Rolf performance test - also see JavaDBUtils.java
      System.setProperty("derby.storage.pageCacheSize", "10000");  //Default: 1000
      System.setProperty("derby.storage.pageSize", "32768");  //Possible: 4096, 8192, 16384, or 32768 bytes
      System.setProperty("derby.replication.logBufferSize", "65536");
      // This may be a bit dangerous: System.setProperty("derby.system.durability","test");

      String connectionURL = getConnectionURL(dir, databaseName, connectionType);

      return DatabaseUtils.createConfiguration(JDBC_DRIVER, connectionURL, username, password);
   }

   public static String inMemoryConnectionUrl(String databaseName) {
      return "jdbc:derby:memory:" + databaseName;
   }

   public static Configuration createInMemoryConfiguration(String databaseName) {
      return DatabaseUtils.createConfiguration(JDBC_DRIVER, inMemoryConnectionUrl(databaseName) + ";create=true", "", "");
   }

   public static void dropInMemoryDatabase(String databaseName) {
      try (Connection connection = DriverManager.getConnection(inMemoryConnectionUrl(databaseName) + ";drop=true")) {
         assert connection.isClosed();
      } catch (SQLException e) {
         // JavaDB shutdown throws an exception when successful, see documentation for expected state.
         if (e.getSQLState().equals("08006")) {
            Log.global.info(e.getMessage());
         } else {
            Log.global.log(Level.WARNING, "Error dropping in-memory JavaDB: " + databaseName, e);
         }
      }
   }

   public static void importTableFromTextFile(DatabaseConnection databaseConnection, Class<? extends BaseDatabaseObject> clazz, Path file) {
      String tableName = DatabaseUtils.getTableName(clazz).toUpperCase(Locale.ENGLISH);
      String sql = "CALL SYSCS_UTIL.SYSCS_IMPORT_TABLE('APP','" + tableName + "', '" + toEscapedPath(file) + "',';','\"', null, 0)";
      databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.nativeSql(sql));
   }

   /**
    * Issues an Apache Derby/JavaDB native command for dumping the result of
    * a query to a text file. See Derby/JavaDB documentation for more info.
    *
    * @param databaseConnection database connection
    * @param clazz              the table to be dumped
    * @param criteria           what to dump
    * @param file               the output file
    */
   public static void dumpTableToTextFile(DatabaseConnection databaseConnection, Class<? extends BaseDatabaseObject> clazz,
                                          List<Map.Entry<DatabaseColumn, Integer>> criteria, Path file) throws IOException {
      Files.deleteIfExists(file);
      String whereClause = criteria.isEmpty() ? "" :
            criteria.stream()
                  .map(e -> e.getKey().name() + "=" + e.getValue())
                  .collect(Collectors.joining(" and ", "where ", ""));
      String sql = "CALL SYSCS_UTIL.SYSCS_EXPORT_QUERY " +
            "('select * from " + DatabaseUtils.getTableName(clazz).toUpperCase(Locale.ENGLISH) + " " + whereClause + "', '" +
            toEscapedPath(file) + "',';',null,null)";
      databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.nativeSql(sql));
   }

   private static String toEscapedPath(Path file) {
      return file.toAbsolutePath().toString().replace("'", "''");
   }

   public static void prepareToConnect(ConnectionType connectionType, Path dir, String databaseName) throws IOException {
      if (connectionType == ConnectionType.INITIALIZE) {
         FileUtils.createDirectories(dir);
         FileUtils.deleteRecursively(dir.resolve(databaseName));
      }
   }

   public static boolean isJavaDBDatabase(Path dir, String databaseName) {
      return isJavaDBDirectory(dir.resolve(databaseName));
   }

   public static boolean isJavaDBDirectory(Path dir) {
      return Files.isDirectory(dir.resolve("log")) &&
            Files.isDirectory(dir.resolve("seg0")) &&
            Files.isRegularFile(dir.resolve("service.properties"));
   }
}
