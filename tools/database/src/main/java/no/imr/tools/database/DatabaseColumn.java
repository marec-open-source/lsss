package no.imr.tools.database;

import no.imr.tools.database.hibernate.BaseDatabaseObject;

import java.util.Set;

/**
 * The fields of java classes mapped to DB tables.
 * Used for getting the path to the particular field one wants
 * to use for query on a class.
 *
 * @param name             the name of the column/field in the Java class
 * @param nonCompIdClasses classes that should not add "compId." before the field name
 */
public record DatabaseColumn(
      String name,
      Set<Class<? extends BaseDatabaseObject>> nonCompIdClasses
) {
   public DatabaseColumn(String name) {
      this(name, Set.of());
   }

   public DatabaseColumn(String name, Class<? extends BaseDatabaseObject> nonCompIdClass) {
      this(name, Set.of(nonCompIdClass));
   }

   /**
    * Function for looking up the path to the field of a class.
    *
    * @param clazz the class containing the field
    * @return the path to the field
    */
   public String getFieldPath(Class<? extends BaseDatabaseObject> clazz) {
      if (nonCompIdClasses.contains(clazz)) {
         return name;
      } else {
         return "compId." + name;
      }
   }
}
