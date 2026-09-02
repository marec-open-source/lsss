package no.imr.lsss.database;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.SurveyInfo;
import no.imr.lsss.database.tables.hibernate.SurveyPK;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class LsssDatabaseUtilsTest {
   @Test
   void testDatabaseClasses() {
      LSSS lsss = LsssTestUtils.start(List.of(), List.of());
      Set<Class<? extends BaseDatabaseObject>> databaseClasses = new HashSet<>(LsssDatabaseUtils.getAllDatabaseClasses(lsss));
      LsssDatabaseUtils.getSystemClasses(lsss).forEach(c -> assertTrue(databaseClasses.remove(c), c.getName()));
      LsssDatabaseUtils.getNationClasses(lsss).forEach(c -> assertTrue(databaseClasses.remove(c), c.getName()));
      LsssDatabaseUtils.getPlatformClasses(lsss).forEach(c -> assertTrue(databaseClasses.remove(c), c.getName()));
      LsssDatabaseUtils.getSurveyClasses(lsss).forEach(c -> assertTrue(databaseClasses.remove(c), c.getName()));
      assertEquals(0, databaseClasses.size());
      lsss.close();
   }

   @Test
   void surveyInfo() {
      String infoValue = "";
      List<SurveyInfo> surveyInfos = LsssDatabaseUtils.toSurveyInfos(new SurveyPK((short) 0, (short) 0, 0), "test", infoValue);
      assertEquals(1, surveyInfos.size());
      assertEquals(infoValue, LsssDatabaseUtils.fromSurveyInfos(surveyInfos));

      infoValue = "b".repeat(DatabaseData.MAX_SURVEY_INFO_VALUE_LENGTH);
      surveyInfos = LsssDatabaseUtils.toSurveyInfos(new SurveyPK((short) 0, (short) 0, 0), "test", infoValue);
      assertEquals(1, surveyInfos.size());
      assertEquals(infoValue, LsssDatabaseUtils.fromSurveyInfos(surveyInfos));

      infoValue = "a" + "b".repeat(DatabaseData.MAX_SURVEY_INFO_VALUE_LENGTH);
      surveyInfos = LsssDatabaseUtils.toSurveyInfos(new SurveyPK((short) 0, (short) 0, 0), "test", infoValue);
      assertEquals(2, surveyInfos.size());
      assertEquals(infoValue, LsssDatabaseUtils.fromSurveyInfos(surveyInfos));
   }
}
