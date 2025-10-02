package no.imr.tools.database.queries;

import no.imr.tools.database.DatabaseColumn;
import no.imr.tools.database.hibernate.BaseDatabaseObject;

import java.util.Locale;

public final class QueryUtils {
   private static final String EQUALS = "=";

   private QueryUtils() {
   }

   /**
    * {@return an HQL string of the form "from Class clazz"}
    *
    * @param clazz the class to query
    */
   public static String buildFromQuery(Class<? extends BaseDatabaseObject> clazz) {
      return "from " + clazz.getSimpleName() + " " + clazz.getSimpleName().toLowerCase(Locale.ENGLISH);
   }

   public static String buildFromQuery(Class<? extends BaseDatabaseObject> clazz,
                                       DatabaseColumn columnA, Object valueA) {
      return buildFromQuery(clazz) + " where " + buildEqualityString(clazz, columnA, valueA);
   }

   public static String buildFromQuery(Class<? extends BaseDatabaseObject> clazz,
                                       DatabaseColumn columnA, Object valueA,
                                       DatabaseColumn columnB, Object valueB) {
      return buildFromQuery(clazz, columnA, valueA) + " and " + buildEqualityString(clazz, columnB, valueB);
   }

   public static String buildFromQuery(Class<? extends BaseDatabaseObject> clazz,
                                       DatabaseColumn columnA, Object valueA,
                                       DatabaseColumn columnB, Object valueB,
                                       DatabaseColumn columnC, Object valueC) {
      return buildFromQuery(clazz, columnA, valueA, columnB, valueB) + " and " + buildEqualityString(clazz, columnC, valueC);
   }

   public static String buildFromQuery(Class<? extends BaseDatabaseObject> clazz,
                                       DatabaseColumn columnA, Object valueA,
                                       DatabaseColumn columnB, Object valueB,
                                       DatabaseColumn columnC, Object valueC,
                                       DatabaseColumn columnD, Object valueD) {
      return buildFromQuery(clazz, columnA, valueA, columnB, valueB, columnC, valueC) + " and " + buildEqualityString(clazz, columnD, valueD);
   }

   /**
    * Convenience function for building an equality string.
    *
    * @param clazz  the class to query
    * @param column the column
    * @param value  the value
    * @return a string of the form [a field = a value]
    */
   private static <T extends BaseDatabaseObject> String buildEqualityString(Class<T> clazz, DatabaseColumn column, Object value) {
      return buildCriteriaString(clazz, column, EQUALS, value);
   }

   /**
    * Function for building a substring for a query.
    * Builds a string with a criteria of the form "[a field path ][operator] [value]".
    *
    * @param clazz    Class to query. Necessary in order to resolve field path.
    *                 Assumes lower case HQL instance of class has been created in "from" statement
    * @param column   Java class field to filter
    * @param operator Should use one of the static types in DatabaseQuery : EQUALS, LESS_THAN or GREATER THAN
    * @param value    Required value of field
    * @return selection string
    */
   static <T extends BaseDatabaseObject> String buildCriteriaString(Class<T> clazz, DatabaseColumn column, String operator, Object value) {
      return clazz.getSimpleName().toLowerCase(Locale.ENGLISH) + "." + column.getFieldPath(clazz) + operator + valueToString(value);
   }

   private static String valueToString(Object value) {
      if (value instanceof String) {
         return "'" + value + "'";
      } else {
         return value.toString();
      }
   }
}
