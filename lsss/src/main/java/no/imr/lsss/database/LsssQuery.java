package no.imr.lsss.database;

import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.Area;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.tables.hibernate.SurveyPK;
import no.imr.tools.database.DatabaseColumn;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.DeleteQuery;
import no.imr.tools.database.queries.FetchQuery;

public final class LsssQuery {
   private LsssQuery() {
   }

   public static DeleteQuery delete(Class<? extends BaseDatabaseObject> clazz, Survey survey) {
      return new DeleteQuery(clazz,
            DatabaseData.NATION, survey.getCompId().getNation(),
            DatabaseData.PLATFORM, survey.getCompId().getPlatform(),
            DatabaseData.SURVEY, survey.getCompId().getSurvey());
   }

   public static DeleteQuery delete(Class<? extends BaseDatabaseObject> clazz, Survey survey, DatabaseColumn column, Object value) {
      return new DeleteQuery(clazz,
            DatabaseData.NATION, survey.getCompId().getNation(),
            DatabaseData.PLATFORM, survey.getCompId().getPlatform(),
            DatabaseData.SURVEY, survey.getCompId().getSurvey(),
            column, value);
   }

   public static DeleteQuery delete(Class<? extends BaseDatabaseObject> clazz, AcousticCategory acousticCategory) {
      return new DeleteQuery(clazz,
            DatabaseData.NATION, acousticCategory.getCompId().getNation(),
            DatabaseData.PLATFORM, acousticCategory.getCompId().getPlatform(),
            DatabaseData.ACOUSTIC_CATEGORY, acousticCategory.getCompId().getAcousticCategory());
   }

   public static DeleteQuery delete(Class<? extends BaseDatabaseObject> clazz, Platform platform, Area area) {
      return new DeleteQuery(clazz,
            DatabaseData.NATION, platform.getCompId().getNation(),
            DatabaseData.PLATFORM, platform.getCompId().getPlatform(),
            DatabaseData.AREA, area.getCompId().getArea());
   }

   public static DeleteQuery delete(Class<? extends BaseDatabaseObject> clazz, Platform platform) {
      return new DeleteQuery(clazz,
            DatabaseData.NATION, platform.getCompId().getNation(),
            DatabaseData.PLATFORM, platform.getCompId().getPlatform());
   }

   public static <T extends BaseDatabaseObject> FetchQuery<T> fetch(Class<T> clazz, Platform platform) {
      return new FetchQuery<>(clazz,
            DatabaseData.NATION, platform.getCompId().getNation(),
            DatabaseData.PLATFORM, platform.getCompId().getPlatform());
   }

   public static <T extends BaseDatabaseObject> FetchQuery<T> fetch(Class<T> clazz, Survey survey) {
      return new FetchQuery<>(clazz,
            DatabaseData.NATION, survey.getCompId().getNation(),
            DatabaseData.PLATFORM, survey.getCompId().getPlatform(),
            DatabaseData.SURVEY, survey.getCompId().getSurvey());
   }

   public static <T extends BaseDatabaseObject> FetchQuery<T> fetch(Class<T> clazz, SurveyPK surveyPK, DatabaseColumn column, Object value) {
      return new FetchQuery<>(clazz,
            DatabaseData.NATION, surveyPK.getNation(),
            DatabaseData.PLATFORM, surveyPK.getPlatform(),
            DatabaseData.SURVEY, surveyPK.getSurvey(),
            column, value);
   }

   public static <T extends BaseDatabaseObject> FetchQuery<T> fetch(Class<T> clazz, AcousticCategory acousticCategory) {
      return new FetchQuery<>(clazz,
            DatabaseData.NATION, acousticCategory.getCompId().getNation(),
            DatabaseData.PLATFORM, acousticCategory.getCompId().getPlatform(),
            DatabaseData.ACOUSTIC_CATEGORY, acousticCategory.getCompId().getAcousticCategory());
   }
}
