package no.imr.lsss.database;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.export.DatabaseExporter;
import no.imr.lsss.database.export.DatabaseImporter;
import no.imr.lsss.database.tables.ObservationTypeEnum;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationComment;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.lsss.database.tables.hibernate.StandardComment;
import no.imr.lsss.database.tables.hibernate.StandardCommentPK;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.types.TestDatabasePlugin;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.StatelessDatabaseQuery;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.test.UniqueTmpDir;
import no.imr.tools.upgrade.UpgradeException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

final class DatabaseExportImportTest {
   @Test
   void runTest() throws IOException, UpgradeException {
      // First, create LSSS, a database and a survey.
      LSSS lsss = LsssTestUtils.start(List.of(), List.of());
      TestDatabasePlugin.install(lsss);
      Survey survey = DatabaseTestUtils.resetCompleteTestSurvey(lsss);

      ObservationPK observationPK = new ObservationPK(survey.getCompId(), 2026_03_20, 15_33_44_55, ObservationTypeEnum.NAVIGATION_DATA_INPUT.getValue());
      List<BaseDatabaseObject> someObjects = List.of(
            new StandardComment(new StandardCommentPK(survey.getCompId().getNation(), survey.getCompId().getPlatform(), 0), "std"),
            new Observation(observationPK, 1, 2, 3, 4),
            new ObservationComment(observationPK, 0, 1, 2, "")
      );
      lsss.getDatabaseManager().getDatabaseConnection().executeStatelessQuery(StatelessDatabaseQuery.insert(someObjects));

      var originalContent = getDatabaseContent(lsss);
      assertEquals(1, originalContent.get(Survey.class).size());

      Path tmpDir = UniqueTmpDir.newSubDir("DatabaseExportImportTest");
      new DatabaseExporter(lsss, tmpDir, DatabaseExporter.DATABASE_NAME, DatabaseExporter.Type.JavaDB)
            .exportSurveys(List.of(survey));

      lsss.close();

      // Now start a new instance of LSSS and import the survey.
      for (boolean importFromTextFiles : List.of(false, true)) {
         LSSS lsss2 = LsssTestUtils.start(List.of(), List.of());
         TestDatabasePlugin.install(lsss2);
         DatabaseTestUtils.createTestDatabase(lsss2);

         var beforeImportContent = getDatabaseContent(lsss2);
         assertNotEquals(originalContent, beforeImportContent);
         assertEquals(Set.of(), beforeImportContent.get(Survey.class));
         assertEquals(0, containsCount(beforeImportContent, someObjects));

         DatabaseImporter databaseImporter = new DatabaseImporter(lsss2)
               .setInteractiveMode(false);
         DatabaseImporter.Result result = importFromTextFiles
               ? databaseImporter.importFromTextFiles(null, tmpDir)
               : databaseImporter.importFromDatabase(null, tmpDir, DatabaseExporter.DATABASE_NAME);
         assertEquals(DatabaseImporter.Result.DONE, result);

         var afterImportContent = getDatabaseContent(lsss2);
         assertEquals(originalContent, afterImportContent);
         assertEquals(Set.of(survey), afterImportContent.get(Survey.class));
         assertEquals(someObjects.size(), containsCount(afterImportContent, someObjects));

         lsss2.close();
      }

      FileUtils.deleteRecursively(tmpDir);
   }

   private static Map<Class<? extends BaseDatabaseObject>, Set<BaseDatabaseObject>> getDatabaseContent(LSSS lsss) {
      return LsssDatabaseUtils.getAllDatabaseClasses(lsss).stream()
            .collect(Collectors.toMap(
                  c -> c,
                  c -> new HashSet<>(lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(LsssQuery.fetch(c)))
            ));
   }

   private static int containsCount(Map<Class<? extends BaseDatabaseObject>, Set<BaseDatabaseObject>> databaseContent, List<BaseDatabaseObject> objects) {
      return (int) objects.stream()
            .filter(object -> databaseContent.get(object.getClass()).contains(object))
            .count();
   }
}
