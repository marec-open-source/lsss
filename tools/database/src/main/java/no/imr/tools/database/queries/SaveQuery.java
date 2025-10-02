package no.imr.tools.database.queries;

import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.hibernate.Session;

import java.util.Collection;
import java.util.List;

public final class SaveQuery extends BaseStoreQuery {
   public SaveQuery(Collection<? extends BaseDatabaseObject> objects) {
      super(objects);
   }

   public SaveQuery(BaseDatabaseObject object) {
      this(List.of(object));
   }

   @Override
   public void execute(Session session) {
      objects.forEach(session::save);
   }
}
