package no.imr.tools.database.queries;

import org.hibernate.StatelessSession;

public final class DeleteQuery implements StatelessDatabaseQuery {
   private final String query;

   public DeleteQuery(String query) {
      this.query = query;
   }

   @Override
   public void execute(StatelessSession session) {
      session.createMutationQuery(query).executeUpdate();
   }
}
