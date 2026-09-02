package no.imr.tools.database.queries;

import no.imr.tools.database.DatabaseColumn;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;

import java.util.function.Function;

/**
 * For building HQL queries.
 */
public final class QueryBuilder<T extends BaseDatabaseObject, Q> {
   private final Class<T> clazz;
   private final Function<String, Q> factory;
   private final StringBuilder query = new StringBuilder();

   private QueryBuilder(String operation, Class<T> clazz, Function<String, Q> factory) {
      this.clazz = clazz;
      this.factory = factory;
      query.append(operation).append("from ").append(DatabaseUtils.getTableName(clazz)).append(" x");
   }

   public static <T extends BaseDatabaseObject> QueryBuilder<T, StatelessValuedDatabaseQuery<Long>> count(Class<T> clazz) {
      return new QueryBuilder<>("select count(*) ", clazz, query -> StatelessValuedDatabaseQuery.uniqueNonNullResult(query, Long.class));
   }

   public static <T extends BaseDatabaseObject> QueryBuilder<T, FetchQuery<T>> fetch(Class<T> clazz) {
      return new QueryBuilder<>("", clazz, query -> new FetchQuery<>(clazz, query));
   }

   public static <T extends BaseDatabaseObject> QueryBuilder<T, DeleteQuery> delete(Class<T> clazz) {
      return new QueryBuilder<>("delete ", clazz, DeleteQuery::new);
   }

   @Override
   public String toString() {
      return query.toString();
   }

   public Q build() {
      return factory.apply(query.toString());
   }

   public BeforeTerm where() {
      query.append(" where ");
      return new BeforeTerm();
   }

   public final class BeforeTerm {
      private BeforeTerm() {
      }

      public AfterTerm eq(DatabaseColumn column, Object value) {
         return op(column, "=", value);
      }

      public AfterTerm lt(DatabaseColumn column, Object value) {
         return op(column, "<", value);
      }

      public AfterTerm lte(DatabaseColumn column, Object value) {
         return op(column, "<=", value);
      }

      public AfterTerm gt(DatabaseColumn column, Object value) {
         return op(column, ">", value);
      }

      public AfterTerm gte(DatabaseColumn column, Object value) {
         return op(column, ">=", value);
      }

      private AfterTerm op(DatabaseColumn column, String operator, Object value) {
         query.append("x.");
         column.appendFieldPath(query, clazz);
         query.append(operator);
         appendLiteral(query, value);
         return new AfterTerm();
      }

      private static void appendLiteral(StringBuilder stringBuilder, Object value) {
         if (value instanceof String s) {
            // In HQL string literals are enclosed in single quotes.
            // To escape a single quote within a string literal, use a doubled single quote: ''.
            stringBuilder.append('\'').append(s.replace("'", "''")).append('\'');
         } else {
            stringBuilder.append(value);
         }
      }

      public BeforeTerm parenthesisBegin() {
         query.append('(');
         return this;
      }
   }

   public final class AfterTerm {
      private AfterTerm() {
      }

      public BeforeTerm and() {
         query.append(" and ");
         return new BeforeTerm();
      }

      public BeforeTerm or() {
         query.append(" or ");
         return new BeforeTerm();
      }

      public AfterTerm parenthesisEnd() {
         query.append(')');
         return this;
      }

      public Q build() {
         return QueryBuilder.this.build();
      }
   }
}
