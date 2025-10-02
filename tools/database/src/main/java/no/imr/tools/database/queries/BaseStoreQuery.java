package no.imr.tools.database.queries;

import no.imr.tools.database.hibernate.BaseDatabaseObject;

import java.util.Collection;
import java.util.List;

/**
 * Base class for StoreQueries. Subclasses implement the strategy for storing.
 */
public abstract class BaseStoreQuery implements DatabaseQuery {
   protected final List<BaseDatabaseObject> objects;

   /**
    * Creates a database query for storing a database objects.
    * The collection of objects is defensively copied so that this query is
    * not affected by subsequent modification to the original collection.
    *
    * @param objects the objects to be stored
    */
   BaseStoreQuery(Collection<? extends BaseDatabaseObject> objects) {
      this.objects = List.copyOf(objects);
   }
}
