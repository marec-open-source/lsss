package no.imr.tools.database.queries;

import org.hibernate.StatelessSession;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@FunctionalInterface
public interface StatelessValuedDatabaseQuery<T extends @Nullable Object> {
   T executeAndGetValue(StatelessSession session);

   @SuppressWarnings("SqlSourceToSinkFlow")
   static <T> StatelessValuedDatabaseQuery<T> uniqueNonNullResult(String query, Class<T> resultType) {
      return session -> {
         T result = session.createSelectionQuery(query, resultType).uniqueResult();
         return Objects.requireNonNull(result);
      };
   }
}
