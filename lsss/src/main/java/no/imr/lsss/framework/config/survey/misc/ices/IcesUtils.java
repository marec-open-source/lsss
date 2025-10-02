package no.imr.lsss.framework.config.survey.misc.ices;

import no.imr.lsss.database.DatabaseData;
import no.imr.lsss.database.LsssDatabaseUtils;
import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.ices.IcesAcousticMetadata;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.tables.hibernate.SurveyInfo;
import no.imr.tools.database.DatabaseConnection;
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

   static void acousticMetadataToDatabase(DatabaseConnection databaseConnection, Survey survey, IcesAcousticMetadata icesAcousticMetadata) {
      String infoValue = XmlUtils.toCompactString(icesAcousticMetadata.toXml());
      List<SurveyInfo> surveyInfos = LsssDatabaseUtils.toSurveyInfos(survey.getCompId(), SURVEY_INFO_KEY, infoValue);
      databaseConnection.executeQuery(session -> {
         LsssQuery.delete(SurveyInfo.class, survey, DatabaseData.INFO_KEY, SURVEY_INFO_KEY).execute(session);
         surveyInfos.forEach(session::save);
      });
   }

   public static @Nullable IcesAcousticMetadata acousticMetadataFromDatabase(DatabaseConnection databaseConnection, Survey survey) {
      List<SurveyInfo> surveyInfos = databaseConnection.executeFetchQuery(LsssQuery.fetch(SurveyInfo.class, survey.getCompId(), DatabaseData.INFO_KEY, SURVEY_INFO_KEY));
      String xml = LsssDatabaseUtils.fromSurveyInfos(surveyInfos);
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
