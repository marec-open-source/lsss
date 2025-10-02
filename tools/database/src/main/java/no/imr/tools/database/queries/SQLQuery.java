package no.imr.tools.database.queries;

import org.hibernate.Session;

/**
 * Execute an SQL statement directly through JDBC connection.
 */
public final class SQLQuery implements DatabaseQuery {
   private final String query;

   public SQLQuery(String query) {
      this.query = query;
   }

   @Override
   public void execute(Session session) {
      session.createNativeQuery(query).executeUpdate();
   }
}
