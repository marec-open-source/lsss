package no.imr.tools.database;

import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.database.queries.StatelessDatabaseQuery;
import no.imr.tools.logging.Log;
import org.hibernate.cfg.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Level;

public final class JavaDBUtils {
   private static final String JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
   private static final String JDBC_DERBY_PREFIX = "jdbc:derby:";

   static {
      System.setProperty("derby.stream.error.field", "java.lang.System.err");
   }

   private JavaDBUtils() {
   }

   public static void shutDown(Path dir, String name) {
      shutDown(getConnectionURL(dir, name, ConnectionType.CONNECT));
   }

   public static void shutDown(String connectionURL) {
      try (Connection connection = DriverManager.getConnection(connectionURL + ";shutdown=true")) {
         assert connection.isClosed();
      } catch (SQLException e) {
         //javadb shutdown throws an exception when successful, see documentation for expected state.
         if (e.getSQLState().equals("08006")) {
            Log.global.info(e.getMessage());
         } else {
            Log.global.log(Level.WARNING, "Error shutting down embedded JavaDB", e);
         }
      }
   }

   public static String getConnectionURL(Path dir, String name, ConnectionType connectionType) {
      String path = dir.toAbsolutePath().toString().replace(File.separatorChar, '/');
      if (!path.endsWith("/")) {
         path = path + "/";
      }
      String connectionString = JDBC_DERBY_PREFIX + path + name;
      if (connectionType == ConnectionType.INITIALIZE) {
         connectionString += ";create=true";
         // Rolf: log-file on different disk "T", but do not seem to improve speed:
         // connectionString += ";create=true; logDevice=T:/javaDB_log";
      }
      return connectionString;
   }

   public static Configuration createConfiguration(Path dir, String name, ConnectionType connectionType) {
      return createConfiguration(dir, name, connectionType, "", "");
   }

   public static Configuration createConfiguration(Path dir, String name, ConnectionType connectionType, String username, String password) {
      //Settings prior to 2009.09.25 (until LSSS-1.3.2):
      //System.setProperty("derby.storage.pageCacheSize", "2500");  //Default: 1000

      // Rolf performance test - also see JavaDBUtils.java
      System.setProperty("derby.storage.pageCacheSize", "10000");  //Default: 1000
      System.setProperty("derby.storage.pageSize", "32768");  //Possible: 4096, 8192, 16384, or 32768 bytes
      System.setProperty("derby.replication.logBufferSize", "65536");
      // This may be a bit dangerous: System.setProperty("derby.system.durability","test");

      String connectionURL = getConnectionURL(dir, name, connectionType);

      return DatabaseUtils.createConfiguration(JDBC_DRIVER, connectionURL, username, password);
   }

   public static String inMemoryConnectionUrl(String databaseName) {
      return "jdbc:derby:memory:" + databaseName;
   }

   public static Configuration createInMemoryConfiguration(String databaseName) {
      return DatabaseUtils.createConfiguration(JDBC_DRIVER, inMemoryConnectionUrl(databaseName) + ";create=true", "sa", "");
   }

   public static void dropInMemoryDatabase(String databaseName) {
      try (Connection connection = DriverManager.getConnection(inMemoryConnectionUrl(databaseName) + ";drop=true")) {
         assert connection.isClosed();
      } catch (SQLException e) {
         //javadb shutdown throws an exception when successful, see documentation for expected state.
         if (e.getSQLState().equals("08006")) {
            Log.global.info(e.getMessage());
         } else {
            Log.global.log(Level.WARNING, "Error dropping in-memory JavaDB: " + databaseName, e);
         }
      }
   }

   /**
    * Strips the two first words from the input query.
    * FetchQuery creates a query on the form
    * {@code "from ClassName className where className.compId.ColumnName = Object"}.
    * When using SQL through JDBC, it is not guaranteed that the ClassName matches a
    * table name. The three first words are therefore removed. The caller of
    * this function is responsible for adding "select * from [tablename]" to
    * the string in order to get a meaningful query.
    * <p>
    * It is assumed that the column name matches the field name.
    * There is no guarantee for this other than convention.
    *
    * @param query the query to make a string from
    * @return a string on the form "where ColumnName = Object"
    */
   private static String stripQuerySQLString(FetchQuery<?> query) {
      StringTokenizer t = new StringTokenizer(query.getQueryString());

      t.nextToken(); //Remove "from"
      t.nextToken(); //Remove class name.
      t.nextToken(); //Remove class instance name.

      //Keep the rest.
      StringBuilder builder = new StringBuilder();

      while (t.hasMoreTokens()) {
         builder.append(' ');
         String token = t.nextToken();
         //Remove . and path from field reference.
         String[] fieldName = token.split("\\.");
         if (fieldName.length == 0) {
            builder.append(token);
         } else {
            builder.append(fieldName[fieldName.length - 1]);
         }
      }

      return builder.toString();
   }

   public static void importTableFromTextFile(DatabaseConnection databaseConnection, Class<? extends BaseDatabaseObject> clazz, Path file) {
      String tableName = DatabaseUtils.getTableName(clazz).toUpperCase(Locale.ENGLISH);
      String sql = "CALL SYSCS_UTIL.SYSCS_IMPORT_TABLE('APP','" + tableName + "', '" + file.toAbsolutePath() + "',';','\"', null, 0)";
      databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.nativeSql(sql));
   }

   /**
    * Issues an Apache Derby/JavaDB native command for dumping the result of
    * a query to a text file. See Derby/JavaDB documentation for more info.
    *
    * @param databaseConnection database connection
    * @param clazz              the table to be dumped
    * @param fetchQuery         what to dump
    * @param file               the output file
    */
   public static <T extends BaseDatabaseObject> void dumpTableToTextFile(DatabaseConnection databaseConnection, Class<T> clazz, FetchQuery<T> fetchQuery, Path file) throws IOException {
      Files.deleteIfExists(file);
      String sql = "CALL SYSCS_UTIL.SYSCS_EXPORT_QUERY " +
            "('select * from " + DatabaseUtils.getTableName(clazz) + " " + stripQuerySQLString(fetchQuery) + "', '" +
            file.toAbsolutePath() + "',';',null,null)";
      databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.nativeSql(sql));
   }

   public static boolean isJavaDBDirectory(Path dir) {
      return Files.isDirectory(dir.resolve("log")) &&
            Files.isDirectory(dir.resolve("seg0")) &&
            Files.isRegularFile(dir.resolve("service.properties"));
   }
}
