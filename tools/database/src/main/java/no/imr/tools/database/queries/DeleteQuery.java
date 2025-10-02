package no.imr.tools.database.queries;

import no.imr.tools.database.DatabaseColumn;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.hibernate.Session;

public final class DeleteQuery implements DatabaseQuery {
   private final String query;

   public DeleteQuery(String query) {
      this.query = query;
   }

   public DeleteQuery(Class<? extends BaseDatabaseObject> clazz,
                      DatabaseColumn columnA, Object valueA,
                      DatabaseColumn columnB, Object valueB) {
      this("delete " + QueryUtils.buildFromQuery(clazz,
            columnA, valueA,
            columnB, valueB));
   }

   public DeleteQuery(Class<? extends BaseDatabaseObject> clazz,
                      DatabaseColumn columnA, Object valueA,
                      DatabaseColumn columnB, Object valueB,
                      DatabaseColumn columnC, Object valueC) {
      this("delete " + QueryUtils.buildFromQuery(clazz,
            columnA, valueA,
            columnB, valueB,
            columnC, valueC));
   }

   public DeleteQuery(Class<? extends BaseDatabaseObject> clazz,
                      DatabaseColumn columnA, Object valueA,
                      DatabaseColumn columnB, Object valueB,
                      DatabaseColumn columnC, Object valueC,
                      DatabaseColumn columnD, Object valueD) {
      this("delete " + QueryUtils.buildFromQuery(clazz,
            columnA, valueA,
            columnB, valueB,
            columnC, valueC,
            columnD, valueD));
   }

   @Override
   public void execute(Session session) {
      session.createQuery(query).executeUpdate();
   }
}
