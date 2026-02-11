package no.imr.lsss.database;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.Purpose;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.types.JavaDBInMemoryDatabasePlugin;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.database.queries.QueryBuilder;
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
      JavaDBInMemoryDatabasePlugin.install(lsss);
   }

   @AfterEach
   void afterEach() {
      lsss.close();
   }

   @Test
   void testInit() {
      DatabaseTestUtils.createTestDatabase(lsss);
      assertTrue(databaseManager.getDatabaseConnection().isConnected());
   }

   @Test
   void testXml() {
      DatabaseTestUtils.createTestDatabase(lsss);

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
      Survey survey = DatabaseTestUtils.resetCompleteTestSurvey(lsss);

      assertEquals(lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getSelectedCategories().size(),
            databaseManager.getDatabaseConnection().executeStatelessValuedQuery(LsssQuery.forSurvey(QueryBuilder.count(Purpose.class), survey.getCompId()).build()));
   }
}
