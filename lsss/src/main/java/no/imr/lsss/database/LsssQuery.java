package no.imr.lsss.database;

import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.BasePlatformPK;
import no.imr.lsss.database.tables.hibernate.BaseSurveyPK;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.ScatterObject;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.database.DatabaseColumn;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.DeleteQuery;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.database.queries.QueryBuilder;

public final class LsssQuery {
   private LsssQuery() {
   }

   public static <T extends BaseDatabaseObject, Q> QueryBuilder<T, Q>.AfterTerm forPlatform(QueryBuilder<T, Q> queryBuilder, BasePlatformPK compId) {
      return forPlatform(queryBuilder, compId.getNation(), compId.getPlatform());
   }

   public static <T extends BaseDatabaseObject, Q> QueryBuilder<T, Q>.AfterTerm forPlatform(QueryBuilder<T, Q> queryBuilder, short nation, short platform) {
      return queryBuilder.where()
            .eq(DatabaseData.NATION, nation).and()
            .eq(DatabaseData.PLATFORM, platform);
   }

   public static <T extends BaseDatabaseObject, Q> QueryBuilder<T, Q>.AfterTerm forSurvey(QueryBuilder<T, Q> queryBuilder, BaseSurveyPK compId) {
      return forPlatform(queryBuilder, compId).and()
            .eq(DatabaseData.SURVEY, compId.getSurvey());
   }

   // -------- Delete:

   public static DeleteQuery delete(Class<? extends BaseDatabaseObject> clazz, Platform platform) {
      return forPlatform(QueryBuilder.delete(clazz), platform.getCompId()).build();
   }

   public static DeleteQuery delete(Class<? extends BaseDatabaseObject> clazz, Survey survey) {
      return forSurvey(QueryBuilder.delete(clazz), survey.getCompId()).build();
   }

   public static DeleteQuery delete(Class<? extends BaseDatabaseObject> clazz, AcousticCategory acousticCategory) {
      return forPlatform(QueryBuilder.delete(clazz), acousticCategory.getCompId()).and()
            .eq(DatabaseData.ACOUSTIC_CATEGORY, acousticCategory.getCompId().getAcousticCategory())
            .build();
   }

   public static DeleteQuery delete(Class<? extends BaseDatabaseObject> clazz, ScatterObject scatterObject) {
      return forSurvey(QueryBuilder.delete(clazz), scatterObject.getCompId()).and()
            .eq(DatabaseData.OBJECT, scatterObject.getCompId().getObject())
            .build();
   }

   // -------- Fetch:

   public static <T extends BaseDatabaseObject> FetchQuery<T> fetch(Class<T> clazz) {
      return QueryBuilder.fetch(clazz).build();
   }

   public static <T extends BaseDatabaseObject> FetchQuery<T> fetch(Class<T> clazz,
                     DatabaseColumn columnA, Object valueA) {
      return QueryBuilder.fetch(clazz).where()
            .eq(columnA, valueA)
            .build();
   }

   public static <T extends BaseDatabaseObject> FetchQuery<T> fetch(Class<T> clazz, Platform platform) {
      return forPlatform(QueryBuilder.fetch(clazz), platform.getCompId()).build();
   }

   public static <T extends BaseDatabaseObject> FetchQuery<T> fetch(Class<T> clazz, Survey survey) {
      return forSurvey(QueryBuilder.fetch(clazz), survey.getCompId()).build();
   }

   public static <T extends BaseDatabaseObject> FetchQuery<T> fetch(Class<T> clazz, AcousticCategory acousticCategory) {
      return forPlatform(QueryBuilder.fetch(clazz), acousticCategory.getCompId()).and()
            .eq(DatabaseData.ACOUSTIC_CATEGORY, acousticCategory.getCompId().getAcousticCategory())
            .build();
   }
}
