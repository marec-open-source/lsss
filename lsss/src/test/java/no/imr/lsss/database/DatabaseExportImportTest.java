package no.imr.lsss.database;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.export.DatabaseExporter;
import no.imr.lsss.database.export.DatabaseImporter;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.types.JavaDBInMemoryDatabasePlugin;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.database.queries.QueryBuilder;
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
      // First, create LSSS, a database and a survey.
      LSSS lsss = LsssTestUtils.start(List.of(), List.of());
      JavaDBInMemoryDatabasePlugin.install(lsss);
      Survey survey = DatabaseTestUtils.resetCompleteTestSurvey(lsss);

      Path tmpDir = UniqueTmpDir.newSubDir("DatabaseExportImportTest");
      new DatabaseExporter(lsss, tmpDir, DatabaseExporter.DATABASE_NAME)
            .exportSurveys(List.of(survey));

      lsss.close();

      // Now start a new instance of LSSS and import the survey.

      LSSS lsss2 = LsssTestUtils.start(List.of(), List.of());
      JavaDBInMemoryDatabasePlugin.install(lsss2);
      DatabaseTestUtils.createTestDatabase(lsss2);

      // Explicitly create test nation and test platform since we don't allow import of non-existing
      // platforms or nations.
      Nation testNation = DatabaseTestUtils.createTestNation(lsss2.getDatabaseManager().getDatabaseConnection());
      DatabaseTestUtils.createTestPlatform(lsss2.getDatabaseManager().getDatabaseConnection(), testNation);

      long surveyCountBefore = lsss2.getDatabaseManager().getDatabaseConnection().executeStatelessValuedQuery(QueryBuilder.count(Survey.class).build());
      assertEquals(0, surveyCountBefore);

      new DatabaseImporter(lsss2, tmpDir, DatabaseExporter.DATABASE_NAME)
            .setInteractiveMode(false)
            .importFromDatabase(null);

      long surveyCountAfter = lsss2.getDatabaseManager().getDatabaseConnection().executeStatelessValuedQuery(QueryBuilder.count(Survey.class).build());
      assertEquals(1, surveyCountAfter);

      lsss2.close();

      FileUtils.deleteRecursively(tmpDir);
   }
}
