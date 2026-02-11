package no.imr.tools.database.queries;

import org.hibernate.StatelessSession;

@FunctionalInterface
public interface StatelessValuedDatabaseQuery<T> {
   T executeAndGetValue(StatelessSession session);

   static <T> StatelessValuedDatabaseQuery<T> uniqueResult(String query, Class<T> resultType) {
      return session -> session.createSelectionQuery(query, resultType).uniqueResult();
   }
}
