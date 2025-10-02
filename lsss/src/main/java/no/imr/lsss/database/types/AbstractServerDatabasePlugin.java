package no.imr.lsss.database.types;

import no.imr.lsss.LSSS;
import no.imr.tools.Utils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;

import java.util.List;

/**
 * Abstract base class for database plugins using an external database server.
 */
abstract class AbstractServerDatabasePlugin extends AbstractDatabasePlugin {
   final StringParameter databaseName = new StringParameter(
         new Name("DatabaseName", "Database name"),
         "lsss",
         "Name of the database");

   final StringParameter host = new StringParameter(
         new Name("Host"),
         "",
         "Host name (and port) of the database server");

   final StringParameter userName = new StringParameter(
         new Name("UserName", "Username"),
         Utils.getUserName());

   AbstractServerDatabasePlugin(Name name, LSSS lsss) {
      super(name, lsss);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            databaseName,
            host,
            userName,
            password,
            savePassword
      );
   }

   @Override
   public String getDescription() {
      return "This database requires that you have an account on a database server.";
   }

   @Override
   public boolean isConfigurationValid() {
      return !databaseName.getValue().isEmpty()
            && !host.getValue().isEmpty();
   }
}
