package no.imr.tools.database.content;

import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.Utils;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.SaveOrUpdateQuery;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class DatabaseXmlContent {
   private final Map<String, Optional<ClassInfo>> classInfos = new HashMap<>();
   private final Map<Class<? extends BaseDatabaseObject>, List<BaseDatabaseObject>> content = new LinkedHashMap<>();

   public DatabaseXmlContent(Collection<Class<? extends BaseDatabaseObject>> databaseClasses, Path dir) throws IOException {
      try {
         for (Class<? extends BaseDatabaseObject> databaseClass : databaseClasses) {
            ClassInfo classInfo = new ClassInfo(databaseClass);
            classInfos.put(DatabaseUtils.getTableName(databaseClass).toLowerCase(Locale.ENGLISH), Optional.of(classInfo));
            content.put(classInfo.databaseClass, new ArrayList<>());
         }
      } catch (ReflectiveOperationException e) {
         throw new ShouldNotHappenException(e);
      }

      Files.walkFileTree(dir, new SimpleFileVisitor<>() {
         @Override
         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (!Utils.endsWithIgnoringCase(file.getFileName().toString(), ".xml")) {
               return FileVisitResult.CONTINUE;
            }
            try {
               parse(file);
            } catch (ReflectiveOperationException e) {
               throw new IOException(e);
            }
            return FileVisitResult.CONTINUE;
         }
      });
   }

   public Map<Class<? extends BaseDatabaseObject>, List<BaseDatabaseObject>> getContent() {
      return content;
   }

   public void save(DatabaseConnection connection, Predicate<Class<? extends BaseDatabaseObject>> predicate) {
      content.forEach((c, list) -> {
         if (list.isEmpty()) {
            return;
         }
         if (predicate.test(c)) {
            connection.executeQuery(new SaveOrUpdateQuery(list));
         }
      });
   }

   private void parse(Path file) throws IOException, ReflectiveOperationException {
      Element rootElement = XmlUtils.readDocument(file).getRootElement();
      String tableName = rootElement.getName();
      ClassInfo classInfo = get(classInfos, tableName);
      if (classInfo == null) {
         Log.global.warning("Unknown class '" + tableName + "' in file " + file);
         return;
      }
      List<BaseDatabaseObject> databaseObjects = content.get(classInfo.databaseClass);
      for (Element element : rootElement.elements()) {
         databaseObjects.add(classInfo.newInstance(element));
      }
   }

   static <T> @Nullable T get(Map<String, Optional<T>> map, String key) {
      Optional<T> value = map.computeIfAbsent(key, k -> {
         return map.getOrDefault(k.toLowerCase(Locale.ENGLISH), Optional.empty());
      });
      return value.orElse(null);
   }
}
