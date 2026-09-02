package no.imr.tools.database.queries;

import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.hibernate.StatelessSession;

import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface StatelessDatabaseQuery extends StatelessValuedDatabaseQuery<Optional<Void>> {
   @Override
   default Optional<Void> executeAndGetValue(StatelessSession session) {
      execute(session);
      return Optional.empty();
   }

   void execute(StatelessSession session);

   static StatelessDatabaseQuery empty() {
      return _ -> {
      };
   }

   static StatelessDatabaseQuery delete(List<? extends BaseDatabaseObject> objects) {
      return session -> session.deleteMultiple(objects);
   }

   static StatelessDatabaseQuery insert(BaseDatabaseObject object) {
      return session -> session.insert(object);
   }

   static StatelessDatabaseQuery insert(List<? extends BaseDatabaseObject> objects) {
      return session -> session.insertMultiple(objects);
   }

   static StatelessDatabaseQuery update(BaseDatabaseObject object) {
      return session -> session.update(object);
   }

   static StatelessDatabaseQuery upsert(BaseDatabaseObject object) {
      return session -> session.upsert(object);
   }

   static StatelessDatabaseQuery upsert(List<? extends BaseDatabaseObject> objects) {
      return session -> session.upsertMultiple(objects);
   }

   @SuppressWarnings("SqlSourceToSinkFlow")
   static StatelessDatabaseQuery nativeSql(String sql) {
      return session -> session.createNativeQuery(sql, (Class<?>) null).executeUpdate();
   }

   @SuppressWarnings("SqlSourceToSinkFlow")
   static StatelessDatabaseQuery nativeSql(List<String> sqls) {
      return session -> {
         for (String sql : sqls) {
            session.createNativeQuery(sql, (Class<?>) null).executeUpdate();
         }
      };
   }
}
