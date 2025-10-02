package no.imr.lsss.database;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.export.DatabaseExporter;
import no.imr.lsss.database.export.DatabaseImporter;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.test.UniqueTmpDir;
import no.imr.tools.upgrade.UpgradeException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class DatabaseExportImportTest {
   @Test
   void runTest() throws IOException, UpgradeException {
      //First, create LSSS, a database and a survey.
      DatabaseManagerTest databaseManagerTest = new DatabaseManagerTest();
      databaseManagerTest.beforeEach();
      LSSS lsss = databaseManagerTest.getLSSS();

      databaseManagerTest.createSurvey();
      Survey survey = lsss.getConfigurationManager().getSurveyConf().getSurvey();
      assertNotNull(survey);

      Path tmpDir = UniqueTmpDir.newSubDir("DatabaseExportImportTest");
      new DatabaseExporter(lsss, tmpDir, DatabaseExporter.DATABASE_NAME)
            .exportSurveys(List.of(survey));

      databaseManagerTest.afterEach();

      //Now start a new instance of LSSS and import the survey.

      databaseManagerTest = new DatabaseManagerTest();
      databaseManagerTest.beforeEach();
      LSSS lsss2 = databaseManagerTest.getLSSS();

      //Explicitly create test nation and test platform since we don't allow import of non-existing
      //platforms or nations.
      Nation testNation = DatabaseTestUtils.createTestNation(lsss2.getDatabaseManager().getDatabaseConnection());
      DatabaseTestUtils.createTestPlatform(lsss2.getDatabaseManager().getDatabaseConnection(), testNation);

      List<Survey> surveyListBefore = lsss2.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(Survey.class));
      assertEquals(0, surveyListBefore.size());

      new DatabaseImporter(lsss2, tmpDir, DatabaseExporter.DATABASE_NAME)
            .setInteractiveMode(false)
            .importFromDatabase(null);

      List<Survey> surveyListAfter = lsss2.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(Survey.class));
      assertEquals(1, surveyListAfter.size());

      databaseManagerTest.afterEach();

      FileUtils.deleteRecursively(tmpDir);
   }
}
