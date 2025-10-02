package no.imr.tools.database;

import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.concurrent.SerialExecutor;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.DatabaseQuery;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.database.queries.StatelessDatabaseQuery;
import no.imr.tools.database.queries.StatelessValuedDatabaseQuery;
import no.imr.tools.database.queries.ValuedDatabaseQuery;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.logging.Log;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A connection to a database.
 * Provides functionality for loading, storing and deleting database entries.
 */
public final class DatabaseConnection {
   public static final int MAX_WAITING_COUNT = 100;

   private final ChangeManager busyChangeManager = new ChangeManager();
   private @Nullable SessionFactory sessionFactory;
   private final Executor executor = new SerialExecutor(Exec.CACHED_THREAD_POOL);
   private final AsyncHandle asyncHandle = new AsyncHandle();
   private final AtomicInteger busyCounter = new AtomicInteger();

   public DatabaseConnection() {
   }

   public DatabaseConnection(ConnectionType connectionType, Configuration configuration, List<Class<? extends BaseDatabaseObject>> databaseClasses) {
      connect(connectionType, configuration, databaseClasses);
   }

   public ChangeManager getBusyChangeManager() {
      return busyChangeManager;
   }

   public boolean isBusy() {
      return busyCounter.intValue() > 0 || getWaitingCount() > 0;
   }

   public boolean isConnected() {
      return sessionFactory != null;
   }

   public void connect(ConnectionType connectionType, Configuration configuration, List<Class<? extends BaseDatabaseObject>> databaseClasses) {
      DatabaseUtils.addClasses(configuration, databaseClasses);
      if (connectionType == ConnectionType.INITIALIZE) {
         DatabaseUtils.addCreateProperty(configuration);
      }
      if (sessionFactory != null) {
         disconnect();
      }

      ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
            .applySettings(configuration.getProperties())
            .build();
      sessionFactory = configuration.buildSessionFactory(serviceRegistry);

      try {
         executeQuery(session -> {
            // Connection OK
         });
      } catch (RuntimeException e) {
         disconnect();
         throw e;
      }
   }

   public void disconnect() {
      if (sessionFactory == null) {
         return;
      }

      if (!asyncHandle.isFinished()) {
         Log.global.info("Waiting for asynchronous database access to complete...");
         asyncHandle.waitUntilFinished();
      }

      sessionFactory.close();
      sessionFactory = null;
   }

   public void waitUntilFinished() {
      asyncHandle.waitUntilFinished();
   }

   public int getWaitingCount() {
      return asyncHandle.getWaitingCount();
   }

   public void asyncExecuteQuery(DatabaseQuery databaseQuery) {
      executor.execute(asyncHandle.createManagedRunnable(() -> executeQuery(databaseQuery)));
   }

   /**
    * Synchronous execution of a database fetch query.
    *
    * @param fetchQuery the query to execute
    * @return a list of the objects fetched, which is empty if not connected
    */
   public <T extends BaseDatabaseObject> List<T> executeFetchQuery(FetchQuery<T> fetchQuery) {
      if (sessionFactory == null) {
         return Collections.emptyList();
      }
      return executeStatelessValuedQuery(fetchQuery);
   }

   SessionFactory getSessionFactory() {
      if (sessionFactory == null) {
         throw new IllegalStateException("Not connected to database");
      }
      return sessionFactory;
   }

   public void executeQuery(DatabaseQuery databaseQuery) {
      executeValuedQuery(databaseQuery);
   }

   public <T> T executeValuedQuery(ValuedDatabaseQuery<T> databaseQuery) {
      if (sessionFactory == null) {
         throw new IllegalStateException("Not connected to database");
      }
      busyCounter.incrementAndGet();
      busyChangeManager.notifyListeners();
      try (DatabaseQueryRunner databaseQueryRunner = new DatabaseQueryRunner(sessionFactory)) {
         return databaseQueryRunner.doTransaction(databaseQuery);
      } finally {
         busyCounter.decrementAndGet();
         busyChangeManager.notifyListeners();
      }
   }

   public void executeStatelessQuery(StatelessDatabaseQuery databaseQuery) {
      executeStatelessValuedQuery(databaseQuery);
   }

   public <T> T executeStatelessValuedQuery(StatelessValuedDatabaseQuery<T> databaseQuery) {
      if (sessionFactory == null) {
         throw new IllegalStateException("Not connected to database");
      }
      busyCounter.incrementAndGet();
      busyChangeManager.notifyListeners();
      try (StatelessDatabaseQueryRunner databaseQueryRunner = new StatelessDatabaseQueryRunner(sessionFactory)) {
         return databaseQueryRunner.doTransaction(databaseQuery);
      } finally {
         busyCounter.decrementAndGet();
         busyChangeManager.notifyListeners();
      }
   }
}
