package no.imr.tools.database.queries;

import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.query.SelectionQuery;

import java.util.List;

/**
 * Fetch from database.
 */
public final class FetchQuery<T extends BaseDatabaseObject> implements ValuedDatabaseQuery<List<T>>, StatelessValuedDatabaseQuery<List<T>> {
   private final Class<T> clazz;
   private final String query;

   public FetchQuery(Class<T> clazz, String query) {
      this.clazz = clazz;
      this.query = query;
   }

   @Override
   public List<T> executeAndGetValue(Session session) {
      return session.createSelectionQuery(query, clazz).list();
   }

   @Override
   public List<T> executeAndGetValue(StatelessSession session) {
      return createSelectionQuery(session).list();
   }

   public SelectionQuery<T> createSelectionQuery(StatelessSession session) {
      return session.createSelectionQuery(query, clazz);
   }
}
