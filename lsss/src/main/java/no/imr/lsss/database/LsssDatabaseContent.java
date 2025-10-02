package no.imr.lsss.database;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.AcCatToBiologicalSpecies;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.AcousticCategoryComposite;
import no.imr.lsss.database.tables.hibernate.Area;
import no.imr.lsss.database.tables.hibernate.AreaOfAcousticCategory;
import no.imr.lsss.database.tables.hibernate.BiologicalSpecies;
import no.imr.lsss.database.tables.hibernate.DBParameter;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationComment;
import no.imr.lsss.database.tables.hibernate.ObservationType;
import no.imr.lsss.database.tables.hibernate.Personnel;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.PlatformCodes;
import no.imr.lsss.database.tables.hibernate.PlatformName;
import no.imr.lsss.database.tables.hibernate.PlatformType;
import no.imr.lsss.database.tables.hibernate.Purpose;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.ScatterData;
import no.imr.lsss.database.tables.hibernate.ScatterObject;
import no.imr.lsss.database.tables.hibernate.ScatterType;
import no.imr.lsss.database.tables.hibernate.SchoolCategory;
import no.imr.lsss.database.tables.hibernate.SchoolCategorySystem;
import no.imr.lsss.database.tables.hibernate.SchoolData;
import no.imr.lsss.database.tables.hibernate.SchoolDetect;
import no.imr.lsss.database.tables.hibernate.SchoolMorphology;
import no.imr.lsss.database.tables.hibernate.SchoolObjectType;
import no.imr.lsss.database.tables.hibernate.StandardComment;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.tables.hibernate.SurveyInfo;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.content.DatabaseXmlContent;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.database.upgrade.DatabaseUpgraderFactory;
import no.imr.tools.database.upgrade.UpgradableDatabaseContent;
import no.imr.tools.logging.Log;
import no.imr.tools.upgrade.UpgradeEngine;
import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;

public final class LsssDatabaseContent extends UpgradableDatabaseContent {
   public static final String VERSION_NAME = "version";
   public static final String VERSION_VALUE = "5"; // The database version required by this version of LSSS.

   public static final Path DEFAULT_CONTENT_DIR = LSSS.getInstallationDir().resolve("data").resolve("database").resolve("default").resolve("content");

   public static final List<Class<? extends BaseDatabaseObject>> DATABASE_CLASSES = List.of(
         // System classes:
         DBParameter.class,
         PlatformType.class,
         ObservationType.class,
         SchoolCategorySystem.class,
         SchoolObjectType.class,
         ScatterType.class,
         Nation.class, // Should really have been a subclass of BaseNationObject, but does not have a composite primary key.

         // Nation classes:
         Area.class,
         BiologicalSpecies.class,

         // Platform classes:
         Platform.class,
         PlatformName.class,
         PlatformCodes.class,
         AcousticCategory.class,
         AreaOfAcousticCategory.class,
         AcousticCategoryComposite.class,
         AcCatToBiologicalSpecies.class,
         StandardComment.class,

         // Survey classes:
         Survey.class,
         SurveyInfo.class,
         Purpose.class,
         Observation.class,
         ScatterObject.class,
         Scatter.class,
         ScatterData.class,
         ObservationComment.class,
         Personnel.class,
         SchoolCategory.class,
         SchoolDetect.class,
         SchoolMorphology.class,
         SchoolData.class
   );

   public LsssDatabaseContent() {
      super("LSSS");
   }

   @Override
   public List<Class<? extends BaseDatabaseObject>> getDatabaseClasses() {
      return DATABASE_CLASSES;
   }

   @Override
   public String getVersionOf(DatabaseConnection databaseConnection) {
      List<DBParameter> parameters = databaseConnection.executeFetchQuery(new FetchQuery<>(DBParameter.class, DatabaseData.PAR_NAME, VERSION_NAME));
      return parameters.isEmpty() ? "0" : parameters.getFirst().getParValue();
   }

   @Override
   public String getTargetVersion() {
      return VERSION_VALUE;
   }

   @Override
   public UpgradeEngine<DatabaseConnection> getUpgradeEngine(@Nullable Component referenceComponent) {
      return new UpgradeEngine<>("LSSS database", getTargetVersion(), this::getVersionOf,
            new DatabaseUpgraderFactory(referenceComponent, "no/imr/lsss/resources/databaseUpgrade"));
   }

   @Override
   public void copyDefaultDataIntoTables(DatabaseConnection databaseConnection, Predicate<Class<? extends BaseDatabaseObject>> predicate) {
      try {
         loadDefaultContent().save(databaseConnection, predicate);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
      }
   }

   public static DatabaseXmlContent loadDefaultContent() throws IOException {
      DatabaseXmlContent databaseXmlContent = new DatabaseXmlContent(DATABASE_CLASSES, DEFAULT_CONTENT_DIR);
      databaseXmlContent.getContent().get(DBParameter.class).add(new DBParameter(VERSION_NAME, VERSION_VALUE));
      return databaseXmlContent;
   }
}
