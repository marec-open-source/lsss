package no.imr.lsss.database.types;

import no.imr.lsss.LSSS;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.parameter.Name;
import org.hibernate.cfg.Configuration;

/**
 * Plugin for connecting to a PostgreSQL database.
 */
public final class PostgreSQLDatabasePlugin extends AbstractServerDatabasePlugin {
   private static final String SQL_DIALECT = "org.hibernate.dialect.PostgreSQL95Dialect";
   private static final String JDBC_DRIVER = "org.postgresql.Driver";

   public PostgreSQLDatabasePlugin(LSSS lsss) {
      super(new Name("PostgrSQL"), lsss);

      host.setValue("localhost:5432");
   }

   @Override
   public Configuration getConfiguration(ConnectionType connectionType) {
      return DatabaseUtils.createConfiguration(SQL_DIALECT, JDBC_DRIVER, getConnectionURL(),
            userName.getValue(), password.getValue());
   }

   private String getConnectionURL() {
      return "jdbc:postgresql://" + host.getValue() + "/" + databaseName.getValue();
   }
}
