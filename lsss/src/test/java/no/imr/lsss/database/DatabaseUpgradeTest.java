package no.imr.lsss.database;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationComment;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.PlatformName;
import no.imr.lsss.database.tables.hibernate.PlatformNamePK;
import no.imr.lsss.database.tables.hibernate.PlatformPK;
import no.imr.lsss.database.tables.hibernate.StandardComment;
import no.imr.lsss.database.tables.hibernate.StandardCommentPK;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.tables.hibernate.SurveyInfo;
import no.imr.lsss.database.tables.hibernate.SurveyPK;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.BaseSystemFeatureService;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.database.queries.SaveOrUpdateQuery;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.test.UniqueTmpDir;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test upgrade from older database versions to current version.
 */
final class DatabaseUpgradeTest {
   private Path tmpDir;
   private LSSS lsss;

   @BeforeEach
   void beforeEach() {
      tmpDir = UniqueTmpDir.newSubDir("DatabaseUpgradeTest");
      lsss = LsssTestUtils.start(List.of(new BaseSystemFeatureService()));
   }

   @AfterEach
   void afterEach() throws IOException {
      lsss.close();
      FileUtils.deleteRecursively(tmpDir);
   }

   @Test
   void latestVersion() {
      assertEquals("5", LsssDatabaseContent.VERSION_VALUE);
      // If this test fails we need to write another upgradeFromVersionX test

      assertTrue(Files.isDirectory(DatabaseTestUtils.upgradeTestDataDir().resolve(LsssDatabaseContent.VERSION_VALUE)));
   }

   // Versions before 3 used old HSQLDB version that is now unusable

   @Test
   void upgradeFromVersion3() throws IOException {
      doUpgradeFromVersion("3");

      DatabaseConnection databaseConnection = lsss.getDatabaseManager().getDatabaseConnection();
      assertEquals(List.of(new Nation((short) 1, "TestNationName")), databaseConnection.executeFetchQuery(new FetchQuery<>(Nation.class)));
      assertEquals(List.of(new Platform(new PlatformPK((short) 1, (short) 1), (short) 1, (short) 1, 0, 0)), databaseConnection.executeFetchQuery(new FetchQuery<>(Platform.class)));
      assertEquals(List.of(new PlatformName(new PlatformNamePK((short) 1, (short) 1, 0), 0, "TestPlatformName")), databaseConnection.executeFetchQuery(new FetchQuery<>(PlatformName.class)));
      assertEquals(List.of(new StandardComment(new StandardCommentPK((short) 1, (short) 1, 1), "TestStandardCommentText")), databaseConnection.executeFetchQuery(new FetchQuery<>(StandardComment.class)));
      assertEquals(List.of(new Survey(new SurveyPK((short) 1, (short) 1, 1), "TestSurveyTitle", 0, 0, 0, 0, "TestSurveyComment", 0, 0, 0, 0)), databaseConnection.executeFetchQuery(new FetchQuery<>(Survey.class)));
      assertEquals(List.of(new Observation(new ObservationPK((short) 1, (short) 1, 1, 20160427, 11223344, (short) 1000), 0, 0, 0, 0)), databaseConnection.executeFetchQuery(new FetchQuery<>(Observation.class)));
      assertEquals(List.of(new ObservationComment(new ObservationPK((short) 1, (short) 1, 1, 20160427, 11223344, (short) 1000), 1, 12, 34, "TestCommentText")), databaseConnection.executeFetchQuery(new FetchQuery<>(ObservationComment.class)));
   }

   @Test
   void upgradeFromVersion4() throws IOException {
      doUpgradeFromVersion("4");

      DatabaseConnection databaseConnection = lsss.getDatabaseManager().getDatabaseConnection();
      Survey survey = DatabaseTestUtils.createTestSurvey(databaseConnection);
      String infoValue = "a" + "b".repeat(DatabaseData.MAX_SURVEY_INFO_VALUE_LENGTH);
      databaseConnection.executeQuery(new SaveOrUpdateQuery(LsssDatabaseUtils.toSurveyInfos(survey.getCompId(), "test", infoValue)));
      List<SurveyInfo> surveyInfos = databaseConnection.executeFetchQuery(LsssQuery.fetch(SurveyInfo.class, survey.getCompId(), DatabaseData.INFO_KEY, "test"));
      assertEquals(2, surveyInfos.size());
      assertEquals(infoValue, LsssDatabaseUtils.fromSurveyInfos(surveyInfos));
   }

   private void doUpgradeFromVersion(String fromVersion) throws IOException {
      Path sourceDir = DatabaseTestUtils.upgradeTestDataDir().resolve(fromVersion);
      Path destinationDir = tmpDir.resolve(fromVersion);
      FileUtils.copyRecursively(sourceDir, destinationDir);

      assertFalse(lsss.getDatabaseManager().getDatabaseConnection().isConnected());
      DatabaseTestUtils.addFileDatabasePluginHsqldb(lsss, destinationDir, "lsss");
      lsss.getDatabaseManager().getConnectionManager().openConnection();
      assertTrue(lsss.getDatabaseManager().getDatabaseConnection().isConnected());

      LsssDatabaseContent databaseContent = (LsssDatabaseContent) lsss.getPluginManager().getFeaturePlugin(BaseSystemFeaturePlugin.class).getDatabaseContent();
      String currentDatabaseVersion = databaseContent.getVersionOf(lsss.getDatabaseManager().getDatabaseConnection());
      assertEquals(LsssDatabaseContent.VERSION_VALUE, currentDatabaseVersion);
   }
}
