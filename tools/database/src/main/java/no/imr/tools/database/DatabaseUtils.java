package no.imr.tools.database;

import jakarta.persistence.metamodel.Metamodel;
import no.imr.tools.Utils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.logging.Log;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SelectionQuery;
import org.hibernate.type.BasicType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public final class DatabaseUtils {
   private DatabaseUtils() {
   }

   public static Map<String, Object> getValues(DatabaseConnection databaseConnection, BaseDatabaseObject databaseObject) {
      Map<String, Object> values = new LinkedHashMap<>();

      Metamodel metamodel = databaseConnection.getSessionFactory().getMetamodel();
      MappingMetamodel metamodelImplementor = (MappingMetamodel) metamodel;
      EntityPersister entityPersister = metamodelImplementor.getEntityDescriptor(databaseObject.getClass());

      databaseConnection.executeStatelessQuery(session -> {
         Object identifier = entityPersister.getIdentifier(databaseObject);
         Type identifierType = entityPersister.getIdentifierType();
         switch (identifierType) {
            case BasicType<?> _ -> {
               values.put(entityPersister.getIdentifierPropertyName(), identifier);
            }
            case ComponentType componentType -> {
               String[] identifierPropertyNames = componentType.getPropertyNames();
               Object[] identifierPropertyValues = componentType.getPropertyValues(identifier);
               for (int i = 0; i < identifierPropertyNames.length; i++) {
                  values.put(identifierPropertyNames[i], identifierPropertyValues[i]);
               }
            }
            default -> {
               Log.global.warning("Unsupported type: " + identifierType.getClass());
            }
         }

         for (String propertyName : entityPersister.getPropertyNames()) {
            AttributeMapping attributeMapping = entityPersister.findAttributeMapping(propertyName);
            if (attributeMapping instanceof BasicAttributeMapping) {
               Object propertyValue = entityPersister.getPropertyValue(databaseObject, propertyName);
               values.put(propertyName, propertyValue);
            }
         }
      });

      return values;
   }

   /**
    * Copies the results of a query from a source database to a destination database.
    * The copied items must not exist in the destination database.
    *
    * @param source      the database to copy from
    * @param fetchQuery  what to copy
    * @param destination the database to copy to
    */
   public static <T extends BaseDatabaseObject> void copyByInsert(DatabaseConnection source, FetchQuery<T> fetchQuery, DatabaseConnection destination) {
      source.executeStatelessQuery(sessionFrom -> {
         SelectionQuery<T> query = sessionFrom.createSelectionQuery(fetchQuery.getQueryString(), fetchQuery.getQueryClass());
         try (ScrollableResults<T> results = query.scroll(ScrollMode.FORWARD_ONLY)) {
            destination.executeStatelessQuery(sessionTo -> {
               while (results.next()) {
                  sessionTo.insert(results.get());
               }
            });
         }
      });
   }

   public static String getTableName(Class<? extends BaseDatabaseObject> clazz) {
      return clazz.getSimpleName();
   }

   public static Configuration createConfiguration(String driverClass, String connectionUrl, String username, String password) {
      try {
         Class.forName(driverClass);
      } catch (ClassNotFoundException e) {
         Log.global.log(Level.WARNING, "Error loading JDBC Driver: " + driverClass, e);
      }

      Configuration configuration = new Configuration()
            // Hibernate 6 selects the dialect automatically.
            .setProperty(Environment.JAKARTA_JDBC_DRIVER, driverClass)
            .setProperty(Environment.JAKARTA_JDBC_URL, connectionUrl);

      if (!username.isEmpty()) {
         configuration.setProperty(Environment.JAKARTA_JDBC_USER, username);
      }

      if (!password.isEmpty()) {
         configuration.setProperty(Environment.JAKARTA_JDBC_PASSWORD, password);
      }

      return configuration;
   }

   public static void addClasses(Configuration configuration, Collection<Class<? extends BaseDatabaseObject>> databaseClasses) {
      databaseClasses.forEach(configuration::addAnnotatedClass);
   }

   public static void addCreateProperty(Configuration configuration) {
      configuration.setProperty(Environment.HBM2DDL_AUTO, "create");
   }

   /**
    * Avoid infinity. See ticket #634.
    *
    * @param value a possible infinite value
    * @return a finite float
    */
   public static float toFiniteFloat(double value) {
      return Utils.avoidInfinity((float) value);
   }

   public static float toFiniteFloat(float value) {
      return Utils.avoidInfinity(value);
   }

   /**
    * Convert null to empty string.
    * Needed since Oracle databases convert the empty string to null.
    * See issue #852.
    *
    * @param string nullable input string
    * @return {@code string} or {@code ""} if string is {@code null}
    */
   public static String nullToEmpty(@Nullable String string) {
      return string != null ? string : "";
   }
}
