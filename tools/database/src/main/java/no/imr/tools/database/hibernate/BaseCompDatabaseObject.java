package no.imr.tools.database.hibernate;

public interface BaseCompDatabaseObject<T extends BaseCompPK> extends BaseDatabaseObject {
   @Override
   default Object primaryKey() {
      return getCompId();
   }

   T getCompId();

   void setCompId(T compId);
}
