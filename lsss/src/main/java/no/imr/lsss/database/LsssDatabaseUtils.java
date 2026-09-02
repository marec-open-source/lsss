package no.imr.lsss.database;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.BaseNationObject;
import no.imr.lsss.database.tables.hibernate.BasePlatformObject;
import no.imr.lsss.database.tables.hibernate.BaseSurveyObject;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.PlatformPK;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.tables.hibernate.SurveyInfo;
import no.imr.lsss.database.tables.hibernate.SurveyInfoPK;
import no.imr.lsss.database.tables.hibernate.SurveyPK;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.UnionList;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.database.queries.QueryBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class LsssDatabaseUtils {
   private LsssDatabaseUtils() {
   }

   public static List<Class<? extends BaseDatabaseObject>> getAllDatabaseClasses(LSSS lsss) {
      return getSomeClasses(lsss, _ -> true);
   }

   public static List<Class<? extends BaseDatabaseObject>> getSystemClasses(LSSS lsss) {
      return getSomeClasses(lsss, LsssDatabaseUtils::isSystemClass);
   }

   public static boolean isSystemClass(Class<? extends BaseDatabaseObject> c) {
      return c != Nation.class
            && !BaseNationObject.class.isAssignableFrom(c)
            && !BasePlatformObject.class.isAssignableFrom(c)
            && !BaseSurveyObject.class.isAssignableFrom(c);
   }

   public static List<Class<? extends BaseDatabaseObject>> getNationClasses(LSSS lsss) {
      return getSomeClasses(lsss, c -> c == Nation.class || BaseNationObject.class.isAssignableFrom(c));
   }

   public static List<Class<? extends BaseDatabaseObject>> getPlatformClasses(LSSS lsss) {
      return getSomeClasses(lsss, BasePlatformObject.class::isAssignableFrom);
   }

   public static List<Class<? extends BaseDatabaseObject>> getSurveyClasses(LSSS lsss) {
      return getSomeClasses(lsss, BaseSurveyObject.class::isAssignableFrom);
   }

   private static List<Class<? extends BaseDatabaseObject>> getSomeClasses(LSSS lsss, Predicate<Class<? extends BaseDatabaseObject>> predicate) {
      return lsss.getPluginManager().getFeaturePlugins().stream()
            .map(FeaturePlugin::getDatabaseContent)
            .filter(Objects::nonNull)
            .flatMap(databaseContent -> databaseContent.getDatabaseClasses().stream())
            .filter(predicate)
            .toList();
   }


   public static <T extends BaseDatabaseObject> void copyClass(DatabaseConnection source,
                                                               Class<T> clazz,
                                                               DatabaseConnection destination,
                                                               AsyncHandle asyncHandle) {
      FetchQuery<T> fetchQuery = LsssQuery.fetch(clazz);
      DatabaseUtils.copyByInsert(source, fetchQuery, destination, asyncHandle);
   }

   public static <T extends BaseDatabaseObject> void copyClassForNation(DatabaseConnection source,
                                                                        Class<T> clazz, short nationPK,
                                                                        DatabaseConnection destination,
                                                                        AsyncHandle asyncHandle) {
      FetchQuery<T> fetchQuery = LsssQuery.fetch(clazz, DatabaseData.NATION, nationPK);
      DatabaseUtils.copyByInsert(source, fetchQuery, destination, asyncHandle);
   }

   public static <T extends BaseDatabaseObject> void copyClassForPlatform(DatabaseConnection source,
                                                                          Class<T> clazz, PlatformPK platformPK,
                                                                          DatabaseConnection destination,
                                                                          AsyncHandle asyncHandle) {
      FetchQuery<T> fetchQuery = LsssQuery.forPlatform(QueryBuilder.fetch(clazz), platformPK).build();
      DatabaseUtils.copyByInsert(source, fetchQuery, destination, asyncHandle);
   }

   public static <T extends BaseDatabaseObject> void copyClassForSurvey(DatabaseConnection source,
                                                                        Class<T> clazz, Survey survey,
                                                                        DatabaseConnection destination,
                                                                        AsyncHandle asyncHandle) {
      FetchQuery<T> fetchQuery = LsssQuery.fetch(clazz, survey);
      DatabaseUtils.copyByInsert(source, fetchQuery, destination, asyncHandle);
   }

   public static void deleteSurvey(LSSS lsss, DatabaseConnection databaseConnection, Survey survey) {
      List<Class<? extends BaseDatabaseObject>> classes = getSurveyClasses(lsss);
      for (int i = classes.size() - 1; i >= 0; i--) { // Reverse order when deleting
         databaseConnection.executeStatelessQuery(LsssQuery.delete(classes.get(i), survey));
      }
   }

   public static void deletePlatform(LSSS lsss, DatabaseConnection databaseConnection, Platform platform) {
      List<Class<? extends BaseDatabaseObject>> classes = new UnionList<>(getPlatformClasses(lsss), getSurveyClasses(lsss));
      for (int i = classes.size() - 1; i >= 0; i--) { // Reverse order when deleting
         databaseConnection.executeStatelessQuery(LsssQuery.delete(classes.get(i), platform));
      }
   }

   public static Set<Short> toNationPKs(Collection<Survey> surveys) {
      return surveys.stream()
            .map(s -> s.getCompId().getNation())
            .collect(Collectors.toSet());
   }

   public static Set<PlatformPK> toPlatformPKs(Collection<Survey> surveys) {
      return surveys.stream()
            .map(PlatformPK::new)
            .collect(Collectors.toSet());
   }

   public static List<SurveyInfo> toSurveyInfos(SurveyPK surveyPK, String infoKey, String infoValue) {
      List<SurveyInfo> surveyInfos = new ArrayList<>();
      for (int i = 0; i == 0 || i < infoValue.length(); i += DatabaseData.MAX_SURVEY_INFO_VALUE_LENGTH) {
         int j = Math.min(infoValue.length(), i + DatabaseData.MAX_SURVEY_INFO_VALUE_LENGTH);
         surveyInfos.add(new SurveyInfo(new SurveyInfoPK(surveyPK, infoKey, surveyInfos.size()), infoValue.substring(i, j)));
      }
      return surveyInfos;
   }

   public static String fromSurveyInfos(List<SurveyInfo> surveyInfos) {
      return surveyInfos.stream()
            .sorted(Comparator.comparingInt(info -> info.getCompId().getInfoIndex()))
            .map(SurveyInfo::getInfoValue)
            .collect(Collectors.joining());
   }
}
