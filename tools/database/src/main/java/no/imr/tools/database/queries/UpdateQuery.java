package no.imr.tools.database.queries;

import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.hibernate.Session;

import java.util.Collection;
import java.util.List;

public final class UpdateQuery extends BaseStoreQuery {
   public UpdateQuery(Collection<? extends BaseDatabaseObject> objects) {
      super(objects);
   }

   public UpdateQuery(BaseDatabaseObject object) {
      this(List.of(object));
   }

   @Override
   public void execute(Session session) {
      objects.forEach(session::update);
   }
}
