package no.imr.lsss.database.types;

import no.imr.lsss.LSSS;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.JavaDBUtils;
import no.imr.tools.parameter.Name;
import org.hibernate.cfg.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory database for testing.
 */
public class JavaDBInMemoryDatabasePlugin extends NoGuiDatabasePlugin {
   private static final AtomicInteger UNIQUE_COUNTER = new AtomicInteger();

   private final String databaseName;

   public JavaDBInMemoryDatabasePlugin(String databaseName) {
      super(new Name("JavaDBInMemoryDatabasePlugin"));

      this.databaseName = databaseName;
   }

   @Override
   public Configuration getConfiguration(ConnectionType connectionType) {
      return JavaDBUtils.createInMemoryConfiguration(databaseName);
   }

   @Override
   public void shutDown() {
      JavaDBUtils.shutDown(JavaDBUtils.inMemoryConnectionUrl(databaseName));
   }

   public void drop() {
      JavaDBUtils.dropInMemoryDatabase(databaseName);
   }

   public static void install(LSSS lsss) {
      // Use a different database name each time to enable parallel test execution.
      JavaDBInMemoryDatabasePlugin databasePlugin = new JavaDBInMemoryDatabasePlugin("lsss-" + UNIQUE_COUNTER.incrementAndGet());
      lsss.getDatabaseManager().addDatabasePlugin(databasePlugin);
      lsss.getDatabaseManager().getConnectionManager().setDatabasePlugin(databasePlugin);
      Runnable originalOnClose = lsss.getLsssConfig().onClose;
      lsss.getLsssConfig().onClose = () -> {
         databasePlugin.drop();
         originalOnClose.run();
      };
   }
}
