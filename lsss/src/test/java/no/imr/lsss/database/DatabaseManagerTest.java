package no.imr.lsss.database;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.Purpose;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class DatabaseManagerTest {
   private LSSS lsss;
   private DatabaseManager databaseManager;

   @BeforeEach
   void beforeEach() {
      lsss = LsssTestUtils.start(List.of(), List.of());
      databaseManager = lsss.getDatabaseManager();
      assertFalse(databaseManager.getDatabaseConnection().isConnected());
      DatabaseTestUtils.connectToInMemoryDatabase(lsss);
      assertTrue(databaseManager.getDatabaseConnection().isConnected());
   }

   @AfterEach
   void afterEach() {
      lsss.close();
   }

   Survey createSurvey() {
      return DatabaseTestUtils.resetCompleteTestSurvey(lsss);
   }

   @Test
   void testInit() {
      assertTrue(databaseManager.getDatabaseConnection().isConnected());
   }

   @Test
   void testXml() {
      assertTrue(databaseManager.getDatabaseConnection().isConnected());

      Element xml = databaseManager.toXml();

      databaseManager.getConnectionManager().closeConnection();
      assertFalse(databaseManager.getDatabaseConnection().isConnected());

      databaseManager.fromXml(xml);

      assertTrue(databaseManager.getDatabaseConnection().isConnected());

      assertTrue(XmlUtils.equalContent(xml, databaseManager.toXml()));
   }

   @Test
   void testCreateSurvey() {
      Survey survey = createSurvey();

      assertEquals(lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getSelectedCategories().size(),
            databaseManager.getDatabaseConnection().executeFetchQuery(LsssQuery.fetch(Purpose.class, survey)).size());
   }

   LSSS getLSSS() {
      return lsss;
   }
}
