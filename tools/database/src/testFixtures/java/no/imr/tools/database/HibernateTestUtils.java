package no.imr.tools.database;

import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.hibernate.Metamodel;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public final class HibernateTestUtils {
   private HibernateTestUtils() {
   }

   public static void testHibernateCfgXml(List<Class<? extends BaseDatabaseObject>> databaseClasses, URL... resourceURLs) {
      Set<String> inCodeClasses = getInCodeClassNames(databaseClasses);
      Set<String> cfgXmlClasses = getConfiguredClassNames(resourceURLs);
      assertEquals(inCodeClasses, cfgXmlClasses);
   }

   private static Set<String> getInCodeClassNames(List<Class<? extends BaseDatabaseObject>> databaseClasses) {
      Set<String> classNames = new HashSet<>();
      for (Class<? extends BaseDatabaseObject> databaseClass : databaseClasses) {
         String className = databaseClass.getName();
         assertTrue(classNames.add(className), className);
      }
      return classNames;
   }

   private static Set<String> getConfiguredClassNames(URL... resourceURLs) {
      Configuration configuration = JavaDBUtils.createInMemoryConfiguration("test");
      for (URL resourceURL : resourceURLs) {
         configuration.configure(resourceURL);
      }
      try (SessionFactory sessionFactory = configuration.buildSessionFactory()) {
         Metamodel metamodel = sessionFactory.getMetamodel();
         return metamodel.getEntities().stream()
               .map(entity -> entity.getBindableJavaType().getName())
               .collect(Collectors.toSet());
      } finally {
         JavaDBUtils.dropInMemoryDatabase("test");
      }
   }
}
