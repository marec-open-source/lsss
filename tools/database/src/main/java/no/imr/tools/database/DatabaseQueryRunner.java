package no.imr.tools.database;

import no.imr.tools.database.queries.ValuedDatabaseQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * Opens a {@link Session} and executes a {@link ValuedDatabaseQuery} in a {@link Transaction}.
 */
final class DatabaseQueryRunner implements AutoCloseable {
   private final Session session;

   DatabaseQueryRunner(SessionFactory sessionFactory) {
      session = sessionFactory.openSession();
   }

   <T> T doTransaction(ValuedDatabaseQuery<T> databaseQuery) {
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
