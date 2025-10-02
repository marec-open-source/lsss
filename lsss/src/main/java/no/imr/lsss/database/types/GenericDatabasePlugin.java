package no.imr.lsss.database.types;

import no.imr.lsss.LSSS;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;
import org.hibernate.cfg.Configuration;

import java.util.List;

public final class GenericDatabasePlugin extends AbstractDatabasePlugin {
   private final StringParameter jdbcDriver = new StringParameter(
         new Name("JdbcDriver", "JDBC driver"),
         "",
         "Fully qualified class name for the jdbc driver");

   private final StringParameter sqlDialect = new StringParameter(
         new Name("SqlDialect", "SQL dialect"),
         "",
         "Fully qualified class name for the hibernate sql dialect");

   private final StringParameter connectionUrl = new StringParameter(
         new Name("ConnectionUrl", "Connection URL"),
         "",
         "Database connection URL");

   private final StringParameter userName = new StringParameter(
         new Name("UserName", "Username"));

   public GenericDatabasePlugin(LSSS lsss) {
      super(new Name("GenericDB", "Generic"), lsss);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            jdbcDriver,
            sqlDialect,
            connectionUrl,
            userName,
            password,
            savePassword
      );
   }

   @Override
   public String getDescription() {
      return """
            For specifying the low level connection details to any database.<br>
            Please refer to the help pages for further information.
            """;
   }

   @Override
   public boolean isConfigurationValid() {
      return !jdbcDriver.getValue().isEmpty()
            && !sqlDialect.getValue().isEmpty()
            && !connectionUrl.getValue().isEmpty();
   }

   @Override
   public Configuration getConfiguration(ConnectionType connectionType) {
      return DatabaseUtils.createConfiguration(sqlDialect.getValue(), jdbcDriver.getValue(),
            connectionUrl.getValue(), userName.getValue(), password.getValue());
   }
}
