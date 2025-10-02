package no.imr.tools.database;

import no.imr.tools.database.queries.StatelessValuedDatabaseQuery;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;

/**
 * Opens a {@link StatelessSession} and executes a {@link StatelessValuedDatabaseQuery} in a {@link Transaction}.
 */
final class StatelessDatabaseQueryRunner implements AutoCloseable {
   private final StatelessSession session;

   StatelessDatabaseQueryRunner(SessionFactory sessionFactory) {
      session = sessionFactory.openStatelessSession();
   }

   <T> T doTransaction(StatelessValuedDatabaseQuery<T> databaseQuery) {
      Transaction transaction = session.beginTransaction();
      try {
         T value = databaseQuery.executeAndGetValue(session);
         transaction.commit();
         return value;
      } catch (RuntimeException e) {
         transaction.rollback();
         throw e;
      }
   }

   @Override
   public void close() {
      session.close();
   }
}
