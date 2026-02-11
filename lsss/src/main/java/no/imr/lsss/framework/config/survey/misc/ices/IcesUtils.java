package no.imr.lsss.framework.config.survey.misc.ices;

import no.imr.lsss.database.DatabaseData;
import no.imr.lsss.database.LsssDatabaseUtils;
import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.ices.IcesAcousticMetadata;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.tables.hibernate.SurveyInfo;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.queries.QueryBuilder;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.logging.Level;

public final class IcesUtils {
   private static final String SURVEY_INFO_KEY = "ICES-acoustic-metadata";
   public static final String AC_SCHEMA_PREFIX = "AC_";
   public static final String SURVEY_SCHEMA = "AC_Survey";
   static final String PLATFORM_SCHEMA = "SHIPC";
   static final String ORGANIZATION_SCHEMA = "EDMO";

   private IcesUtils() {
   }

   static void acousticMetadataStringToDatabase(DatabaseConnection databaseConnection, Survey survey, String acousticMetadata) {
      List<SurveyInfo> surveyInfos = LsssDatabaseUtils.toSurveyInfos(survey.getCompId(), SURVEY_INFO_KEY, acousticMetadata);
      databaseConnection.executeStatelessQuery(session -> {
         LsssQuery.forSurvey(QueryBuilder.delete(SurveyInfo.class), survey.getCompId()).and()
               .eq(DatabaseData.INFO_KEY, SURVEY_INFO_KEY)
               .build()
               .execute(session);
         session.insertMultiple(surveyInfos);
      });
   }

   static String acousticMetadataStringFromDatabase(DatabaseConnection databaseConnection, Survey survey) {
      List<SurveyInfo> surveyInfos = databaseConnection.executeFetchQuery(
            LsssQuery.forSurvey(QueryBuilder.fetch(SurveyInfo.class), survey.getCompId()).and()
                  .eq(DatabaseData.INFO_KEY, SURVEY_INFO_KEY)
                  .build());
      return LsssDatabaseUtils.fromSurveyInfos(surveyInfos);
   }

   public static @Nullable IcesAcousticMetadata acousticMetadataFromDatabase(DatabaseConnection databaseConnection, Survey survey) {
      String xml = acousticMetadataStringFromDatabase(databaseConnection, survey);
      if (xml.isEmpty()) {
         return null;
      }
      IcesAcousticMetadata icesAcousticMetadata = new IcesAcousticMetadata();
      try {
         icesAcousticMetadata.fromXml(XmlUtils.readDocument(xml).getRootElement());
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error getting ICES acoustic metadata from database", e);
         return null;
      }
      return icesAcousticMetadata;
   }
}
