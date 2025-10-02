package no.imr.tools.database.queries;

import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.hibernate.Session;

import java.util.Collection;
import java.util.List;

public final class RemoveQuery implements DatabaseQuery {
   private final List<BaseDatabaseObject> objects;

   public RemoveQuery(Collection<? extends BaseDatabaseObject> objects) {
      this.objects = List.copyOf(objects);
   }

   @Override
   public void execute(Session session) {
      objects.forEach(session::remove);
   }
}
