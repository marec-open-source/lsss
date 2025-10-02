package no.imr.tools.database;

import no.imr.tools.logging.Log;
import org.hibernate.cfg.Configuration;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public final class HsqldbUtils {
   private static final String HSQLDB_SQL_DIALECT = "org.hibernate.dialect.HSQLDialect";
   private static final String HSQLDB_JDBC_DRIVER = "org.hsqldb.jdbc.JDBCDriver";
   private static final String HSQLDB_FILE_PREFIX = "jdbc:hsqldb:file:";

   public static final String SCRIPT_FILE_SUFFIX = ".script";

   static {
      System.setProperty("hsqldb.reconfig_logging", "false");
   }

   private HsqldbUtils() {
   }

   public static void shutDown(Path dir, String name) {
      shutDown(getConnectionURL(dir, name));
   }

   public static void shutDown(String connectionURL) {
      Log.global.info("Shutting down " + connectionURL);
      try (Connection connection = DriverManager.getConnection(connectionURL);
           Statement statement = connection.createStatement()) {
         statement.execute("SHUTDOWN");
         connection.commit();
      } catch (SQLException e) {
         Log.global.log(Level.WARNING, "Error shutting down HSQLDB embedded database", e);
      }
   }

   public static Configuration createConfiguration(Path dir, String name) {
      return createConfiguration(dir, name, "sa", "");
   }

   public static Configuration createConfiguration(Path dir, String name, String username, String password) {
      return DatabaseUtils.createConfiguration(
            HSQLDB_SQL_DIALECT,
            HSQLDB_JDBC_DRIVER,
            getConnectionURL(dir, name),
            username,
            password);
   }

   private static String getConnectionURL(Path dir, String name) {
      String path = dir.toAbsolutePath().toString().replace(File.separatorChar, '/');
      if (!path.endsWith("/")) {
         path = path + "/";
      }
      return HSQLDB_FILE_PREFIX + path + name;
   }
}
