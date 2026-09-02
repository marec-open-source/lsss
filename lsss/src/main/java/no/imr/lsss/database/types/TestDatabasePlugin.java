package no.imr.lsss.database.types;

import no.imr.lsss.LSSS;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.parameter.Name;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

public final class TestDatabasePlugin extends NoGuiDatabasePlugin {
   // Use a different database name each time to enable parallel test execution.
   private static final AtomicInteger UNIQUE_COUNTER = new AtomicInteger();

   private final DatabasePlugin databasePlugin = "javadb".equalsIgnoreCase(System.getProperty("no.marec.testDatabase"))
         ? new JavaDBInMemoryDatabasePlugin("TestDatabase." + UNIQUE_COUNTER.incrementAndGet())
         : new HsqldbInMemoryDatabasePlugin("TestDatabase." + UNIQUE_COUNTER.incrementAndGet());
   private final boolean normalShutdown;

   private TestDatabasePlugin(boolean normalShutdown) {
      super(new Name("TestDatabasePlugin"));

      this.normalShutdown = normalShutdown;
   }

   @Override
   public Configuration getConfiguration(ConnectionType connectionType) {
      return databasePlugin.getConfiguration(connectionType)
            .setProperty(AvailableSettings.CONNECTION_PROVIDER, "org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl");
   }

   @Override
   public void shutDown() {
      if (normalShutdown) {
         doShutDown();
      }
   }

   private void doShutDown() {
      databasePlugin.shutDown();
   }

   public static TestDatabasePlugin newDatabasePlugin() {
      return new TestDatabasePlugin(true);
   }

   public static void install(LSSS lsss) {
      TestDatabasePlugin databasePlugin = new TestDatabasePlugin(false);
      lsss.getDatabaseManager().addDatabasePlugin(databasePlugin);
      lsss.getDatabaseManager().getConnectionManager().setDatabasePlugin(databasePlugin);
      Runnable originalOnClose = lsss.getLsssConfig().onClose;
      lsss.getLsssConfig().onClose = () -> {
         originalOnClose.run();
         databasePlugin.doShutDown();
      };
   }
}
