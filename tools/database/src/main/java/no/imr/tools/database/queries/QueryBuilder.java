package no.imr.tools.database.queries;

import no.imr.tools.database.DatabaseColumn;
import no.imr.tools.database.hibernate.BaseDatabaseObject;

import java.util.Locale;

/**
 * For building HQL queries.
 */
public final class QueryBuilder {
   /*
    * todo: Usage of JPA
    * CriteriaBuilder builder = session.getCriteriaBuilder();
         CriteriaQuery<ScatterObject3D> query = builder.createQuery(ScatterObject3D.class);
         Root<ScatterObject3D> root = query.from(ScatterObject3D.class);
         Predicate[] criteriaList  = {
               builder.equal(root.get("compId").get("nation"), observation.getCompId().getNation()),
               builder.equal(root.get("compId").get("platform"), observation.getCompId().getPlatform()),
               builder.equal(root.get("compId").get("survey"), observation.getCompId().getSurvey()),
               builder.equal(root.get("observationDate"), observation.getCompId().getObservationDate()),
               builder.equal(root.get("observationTime"), observation.getCompId().getObservationTime()),
               builder.equal(root.get("observationType"), observation.getCompId().getObservationType())};
         query.where(builder.and(criteriaList));
    */

   private final Class<? extends BaseDatabaseObject> clazz;
   private final StringBuilder query = new StringBuilder();

   private QueryBuilder(String operation, Class<? extends BaseDatabaseObject> clazz) {
      this.clazz = clazz;
      query.append(operation).append("from ").append(clazz.getSimpleName()).append(' ').append(clazz.getSimpleName().toLowerCase(Locale.ENGLISH));
   }

   public static QueryBuilder fetch(Class<? extends BaseDatabaseObject> clazz) {
      return new QueryBuilder("", clazz);
   }

   public static QueryBuilder delete(Class<? extends BaseDatabaseObject> clazz) {
      return new QueryBuilder("delete ", clazz);
   }

   @Override
   public String toString() {
      return getQuery();
   }

   public String getQuery() {
      return query.toString();
   }

   public BeforeTerm where() {
      query.append(" where ");
      return new BeforeTerm();
   }

   public final class BeforeTerm {
      private BeforeTerm() {
      }

      public AfterTerm eq(DatabaseColumn columnA, Object valueA) {
         return op(columnA, "=", valueA);
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
         query.append(QueryUtils.buildCriteriaString(clazz, column, operator, value));
         return new AfterTerm();
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

      public String getQuery() {
         return QueryBuilder.this.getQuery();
      }
   }
}
