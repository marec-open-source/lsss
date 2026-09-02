package no.imr.tools.database.content;

import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.TableWithOnlyPrimaryKeyColumns;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.QueryBuilder;
import no.imr.tools.database.queries.StatelessDatabaseQuery;
import no.imr.tools.io.FilePredicates;
import no.imr.tools.io.FileUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class DatabaseXmlContent {
   private final Map<Class<? extends BaseDatabaseObject>, List<BaseDatabaseObject>> content = new LinkedHashMap<>();

   public DatabaseXmlContent(List<Class<? extends BaseDatabaseObject>> databaseClasses, Path dir, Predicate<Class<? extends BaseDatabaseObject>> predicate) throws IOException {
      Map<String, Path> tableNameToFile = new HashMap<>();
      for (Path file : FileUtils.listFiles(dir, FilePredicates.endsWith(".xml"))) {
         tableNameToFile.put(FileUtils.baseName(file), file);
      }
      for (Class<? extends BaseDatabaseObject> databaseClass : databaseClasses) {
         List<BaseDatabaseObject> databaseObjects = new ArrayList<>();
         content.put(databaseClass, databaseObjects);
         if (predicate.test(databaseClass)) {
            Path file = tableNameToFile.get(DatabaseUtils.getTableName(databaseClass));
            if (file != null) {
               try {
                  ClassInfo classInfo = new ClassInfo(databaseClass);
                  for (Element element : XmlUtils.readDocument(file).getRootElement().elements()) {
                     databaseObjects.add(classInfo.newInstance(element));
                  }
               } catch (ReflectiveOperationException e) {
                  throw new IOException("Error creating data in file " + file, e);
               }
            }
         }
      }
   }

   public Map<Class<? extends BaseDatabaseObject>, List<BaseDatabaseObject>> getContent() {
      return content;
   }

   public void save(DatabaseConnection connection) {
      content.forEach((clazz, list) -> {
         if (list.isEmpty()) {
            return;
         }
         if (clazz.getAnnotation(TableWithOnlyPrimaryKeyColumns.class) != null) {
            DatabaseUtils.updateOrInsert(connection, list, QueryBuilder.fetch(clazz).build());
         } else {
            connection.executeStatelessQuery(StatelessDatabaseQuery.upsert(list));
         }
      });
   }
}
