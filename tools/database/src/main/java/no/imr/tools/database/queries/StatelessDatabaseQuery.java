package no.imr.tools.database.queries;

import org.hibernate.StatelessSession;

import java.util.Optional;

@FunctionalInterface
public interface StatelessDatabaseQuery extends StatelessValuedDatabaseQuery<Optional<Void>> {
   @Override
   default Optional<Void> executeAndGetValue(StatelessSession session) {
      execute(session);
      return Optional.empty();
   }

   void execute(StatelessSession session);
}
