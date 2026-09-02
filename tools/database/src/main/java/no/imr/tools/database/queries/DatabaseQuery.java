package no.imr.tools.database.queries;

import org.hibernate.Session;

import java.util.Optional;

/**
 * A database query that can be executed synchronously or asynchronously.
 */
@FunctionalInterface
public interface DatabaseQuery extends ValuedDatabaseQuery<Optional<Void>> {
   void execute(Session session);

   @Override
   default Optional<Void> executeAndGetValue(Session session) {
      execute(session);
      return Optional.empty();
   }

   static DatabaseQuery empty() {
      return _ -> {
      };
   }
}
