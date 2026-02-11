package no.imr.lsss.database;

import com.google.common.base.Splitter;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.AcousticCategoryPK;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.PlatformName;
import no.imr.lsss.database.tables.hibernate.PlatformNamePK;
import no.imr.lsss.database.tables.hibernate.PlatformPK;
import no.imr.lsss.database.tables.hibernate.Purpose;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.tables.hibernate.SurveyPK;
import no.imr.lsss.database.types.DatabasePlugin;
import no.imr.tools.Utils;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.queries.StatelessDatabaseQuery;
import no.imr.tools.logging.LoggingManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DatabaseTestUtils {
   private DatabaseTestUtils() {
   }

   public static Path upgradeTestDataDir() {
      return LoggingManager.getTopInstallationDir().resolve("lsss/src/test/resources/no/imr/lsss/database/DatabaseUpgradeTest");
   }

   public static void createTestDatabase(LSSS lsss) {
      lsss.getDatabaseManager().getConnectionManager().createDatabase("Creating test database", LsssDatabaseUtils::isSystemClass);
   }

   public static void addFileDatabasePluginHsqldb(LSSS lsss, Path dir, String name) {
      DatabasePlugin databasePlugin = new HsqldbTestDatabasePlugin(dir, name);
      lsss.getDatabaseManager().addDatabasePlugin(databasePlugin);
      lsss.getDatabaseManager().getConnectionManager().setDatabasePlugin(databasePlugin);
   }

   public static Survey resetCompleteTestSurvey(LSSS lsss) {
      DatabaseManager databaseManager = lsss.getDatabaseManager();

      createTestDatabase(lsss);

      DatabaseConnection databaseConnection = databaseManager.getDatabaseConnection();

      Nation nation = createTestNation(databaseConnection);
      Platform platform = createTestPlatform(databaseConnection, nation);
      Survey survey = createTestSurvey(databaseConnection, platform);

      createAcousticCategory(databaseConnection, platform, 0, "raw");
      createAcousticCategory(databaseConnection, platform, 1, "f1");
      createAcousticCategory(databaseConnection, platform, 2, "f2");
      createAcousticCategory(databaseConnection, platform, 3, "f3");
      createAcousticCategory(databaseConnection, platform, 4, "f4");
      createAcousticCategory(databaseConnection, platform, 5, "f5");

      List<AcousticCategory> acousticCategories = databaseManager.getDatabaseData().getAcousticCategories(platform).getAll().subList(0, 3);
      List<Purpose> purposes = List.of(
            new Purpose(survey, acousticCategories.get(0), DatabaseData.Purpose.MAIN),
            new Purpose(survey, acousticCategories.get(1), DatabaseData.Purpose.USABLE),
            new Purpose(survey, acousticCategories.get(2), DatabaseData.Purpose.OTHER));
      databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.insert(purposes));

      databaseManager.getConnectionManager().resetDatabaseData();

      lsss.getConfigurationManager().getSurveyConf().mNation.setValue(Optional.of(nation));
      lsss.getConfigurationManager().getSurveyConf().mPlatform.setValue(Optional.of(platform));
      lsss.getConfigurationManager().getSurveyConf().mSurvey.setValue(Optional.of(survey));

      return survey;
   }

   public static Nation createTestNation(DatabaseConnection databaseConnection) {
      Nation nation = new Nation((short) 12345, "Test nation");
      databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.insert(nation));
      return nation;
   }

   public static Platform createTestPlatform(DatabaseConnection databaseConnection) {
      return createTestPlatform(databaseConnection, createTestNation(databaseConnection));
   }

   public static Platform createTestPlatform(DatabaseConnection databaseConnection, Nation nation) {
      PlatformPK platformPK = new PlatformPK(nation.getNation(), (short) 12345);
      Platform platform = new Platform(platformPK,
            (short) 0,
            (short) 0,
            0,
            0);

      PlatformNamePK platformNamePK = new PlatformNamePK(platformPK.getNation(), platformPK.getPlatform(), 0);
      PlatformName platformName = new PlatformName(platformNamePK, 0, "Test platform");

      platform.setNation(nation);
      platform.setPlatformNames(Set.of(platformName));

      databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.insert(List.of(platform, platformName)));

      return platform;
   }

   public static Survey createTestSurvey(DatabaseConnection databaseConnection) {
      return createTestSurvey(databaseConnection, createTestPlatform(databaseConnection));
   }

   public static Survey createTestSurvey(DatabaseConnection databaseConnection, Platform platform) {
      SurveyPK surveyPK = new SurveyPK(platform.getCompId().getNation(), platform.getCompId().getPlatform(), 1234567890);
      Survey survey = new Survey(surveyPK,
            "Test survey",
            2006_01_01, 0,
            2006_02_02, 23_59_59_99,
            "Test survey comment",
            0, 0, 0, 0);
      survey.setPlatform(platform);

      databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.insert(survey));
      return survey;
   }

   public static void createAcousticCategory(DatabaseConnection databaseConnection, Platform platform, int category, String name) {
      AcousticCategoryPK acousticCategoryPK = new AcousticCategoryPK(platform.getCompId().getNation(), platform.getCompId().getPlatform(), category);
      AcousticCategory acousticCategory = new AcousticCategory(acousticCategoryPK, (short) 0, name, name, name, name);
      databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.insert(acousticCategory));
   }

   public static Set<String> readHsqldbScript(Path file) throws IOException {
      Pattern alterTablePattern = Pattern.compile("^ALTER TABLE (PUBLIC.\\w+) ADD CONSTRAINT \\w+ (.*)");
      Pattern createTablePattern = Pattern.compile("^CREATE MEMORY TABLE (PUBLIC.\\w+)(.*)");

      Set<String> set = new HashSet<>();
      for (String line : Files.readAllLines(file, Utils.UTF_8)) {
         if (line.startsWith("SET DATABASE UNIQUE NAME ")) {
            continue;
         }
         if (line.startsWith("SET FILES CHECK ")) {
            continue;
         }

         Matcher alterTableMatcher = alterTablePattern.matcher(line);
         if (alterTableMatcher.matches()) {
            String table = alterTableMatcher.group(1);
            set.add(table + " CONSTRAINT: " + alterTableMatcher.group(2));
            continue;
         }

         Matcher createTableMatcher = createTablePattern.matcher(line);
         if (createTableMatcher.matches()) {
            String table = createTableMatcher.group(1);
            List<String> parts = Splitter.onPattern(",CONSTRAINT \\w+ ")
                  .splitToList(line.replaceAll("\\)\\)$", ")"));
            set.add(parts.getFirst());
            for (int i = 1; i < parts.size(); i++) {
               set.add(table + " CONSTRAINT: " + parts.get(i));
            }
            continue;
         }

         set.add(line);
      }
      return set;
   }
}
