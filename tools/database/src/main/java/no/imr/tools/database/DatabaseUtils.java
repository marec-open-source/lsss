package no.imr.tools.database;

import no.imr.tools.Utils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.logging.Log;
import org.hibernate.Metamodel;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.type.CompositeType;
import org.hibernate.type.PrimitiveType;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
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
      MetamodelImplementor metamodelImplementor = (MetamodelImplementor) metamodel;
      EntityPersister entityPersister = metamodelImplementor.entityPersister(databaseObject.getClass());

      databaseConnection.executeQuery(session -> {
         // todo: Why is casting necessary? See https://stackoverflow.com/questions/4275502/why-was-hibernates-classmetadata-getidentifierobject-entitymode-deprecated
         SharedSessionContractImplementor implementor = (SharedSessionContractImplementor) session;
         Serializable identifier = entityPersister.getIdentifier(databaseObject, implementor);
         Type identifierType = entityPersister.getIdentifierType();
         if (identifierType instanceof PrimitiveType || identifierType instanceof StringType) {
            values.put(entityPersister.getIdentifierPropertyName(), identifier);
         } else if (identifierType instanceof CompositeType compositeType) {
            String[] identifierPropertyNames = compositeType.getPropertyNames();
            Object[] identifierPropertyValues = compositeType.getPropertyValues(identifier, implementor);
            for (int i = 0; i < identifierPropertyNames.length; i++) {
               values.put(identifierPropertyNames[i], identifierPropertyValues[i]);
            }
         } else {
            Log.global.warning("Unsupported type: " + identifierType.getClass());
         }

         for (String propertyName : entityPersister.getPropertyNames()) {
            Type type = entityPersister.getPropertyType(propertyName);
            if (type instanceof PrimitiveType || type instanceof StringType) {
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
         Query<T> query = sessionFrom.createQuery(fetchQuery.getQueryString(), fetchQuery.getQueryClass());
         try (ScrollableResults results = query.scroll(ScrollMode.FORWARD_ONLY)) {
            destination.executeStatelessQuery(sessionTo -> {
               while (results.next()) {
                  sessionTo.insert(results.get(0));
               }
            });
         }
      });
   }

   public static String getTableName(Class<? extends BaseDatabaseObject> clazz) {
      return clazz.getSimpleName();
   }

   public static Configuration createConfiguration(String sqlDialect, String driverClass, String connectionUrl, String username, String password) {
      try {
         Class.forName(driverClass);
      } catch (ClassNotFoundException e) {
         Log.global.log(Level.WARNING, "Error loading JDBC Driver: " + driverClass, e);
      }

      Configuration configuration = new Configuration()
            .setProperty(Environment.DIALECT, sqlDialect)
            .setProperty(Environment.DRIVER, driverClass)
            .setProperty(Environment.URL, connectionUrl);

      if (!username.isEmpty()) {
         configuration.setProperty(Environment.USER, username);
      }

      if (!password.isEmpty()) {
         configuration.setProperty(Environment.PASS, password);
      }

      return configuration;
   }

   public static void addClasses(Configuration configuration, Collection<Class<? extends BaseDatabaseObject>> databaseClasses) {
      databaseClasses.forEach(configuration::addClass);
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
