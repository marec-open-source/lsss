package no.imr.lsss.database;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.export.DatabaseExporter;
import no.imr.lsss.database.export.DatabaseImporter;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.types.DatabasePlugin;
import no.imr.lsss.database.types.GenericDatabasePlugin;
import no.imr.lsss.database.types.HsqldbDatabasePlugin;
import no.imr.lsss.database.types.JavaDBDatabasePlugin;
import no.imr.lsss.database.types.JavaDBFileDatabasePlugin;
import no.imr.lsss.database.types.PostgreSQLDatabasePlugin;
import no.imr.lsss.framework.config.survey.survey.SurveyConf;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.StatusView;
import no.imr.tools.swing.WorkerDialog;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.awt.Dimension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the toggling between global and local database.
 * <p>
 * Objects that needs to be notified of changes to the database should
 * listen to the change manager of this object to guarantee that they
 * are listening to the right database. Always use the
 * getDatabaseConnectionManager() to get the right database.
 */
public final class DatabaseManager {
   private static final String SURVEY_LOCAL_DIR = "SurveyLocalDatabase";
   private static final String SURVEY_LOCAL_DATABASE = "lsss_DB_surveyLocal";

   private final LSSS lsss;
   private final List<DatabasePlugin> databasePlugins = new ArrayList<>();
   private final DatabaseConnectionManager globalDatabaseConnectionManager;
   private final DatabaseConnectionManager surveyLocalDatabaseConnectionManager;
   private final JavaDBFileDatabasePlugin javaDBPluginLocal = new JavaDBFileDatabasePlugin(new Name("JavaDBPluginLocal"));
   private final ChangeManager connectionChangeManager = new ChangeManager();
   private final ChangeManager busyChangeManager = new ChangeManager();
   private boolean useSurveyLocalDatabase;
   private boolean globalDatabaseConnected;
   private @Nullable Element globalXml;

   public DatabaseManager(LSSS lsss) {
      this.lsss = lsss;

      JavaDBDatabasePlugin javaDB = new JavaDBDatabasePlugin(lsss);

      databasePlugins.add(new GenericDatabasePlugin(lsss));
      databasePlugins.add(new HsqldbDatabasePlugin(lsss));
      databasePlugins.add(javaDB);
      databasePlugins.add(new PostgreSQLDatabasePlugin(lsss));

      globalDatabaseConnectionManager = new DatabaseConnectionManager(lsss, javaDB);
      surveyLocalDatabaseConnectionManager = new DatabaseConnectionManager(lsss, javaDBPluginLocal);

      globalDatabaseConnectionManager.getChangeManager().addListener(connectionChangeManager);
      surveyLocalDatabaseConnectionManager.getChangeManager().addListener(connectionChangeManager);

      globalDatabaseConnectionManager.getDatabaseConnection().getBusyChangeManager().addListener(busyChangeManager);
      surveyLocalDatabaseConnectionManager.getDatabaseConnection().getBusyChangeManager().addListener(busyChangeManager);
   }

   public List<DatabasePlugin> getDatabasePlugins() {
      return databasePlugins;
   }

   public void addDatabasePlugin(DatabasePlugin databasePlugin) {
      databasePlugins.add(databasePlugin);
   }

   public DatabaseConnectionManager getSurveyLocalDatabaseConnectionManager() {
      return surveyLocalDatabaseConnectionManager;
   }

   public DatabaseConnectionManager getGlobalDatabaseConnectionManager() {
      return globalDatabaseConnectionManager;
   }

   public boolean isUseSurveyLocalDatabase() {
      return useSurveyLocalDatabase;
   }

   public Element toXml() {
      if (globalXml != null) {
         return (Element) globalXml.clone();
      } else {
         return globalDatabaseConnectionManager.toXml();
      }
   }

   public void fromXml(Element element) {
      if (useSurveyLocalDatabase) {
         // Do nothing if survey local
      } else {
         globalDatabaseConnectionManager.fromXml(element);
      }
   }

   public DatabaseConnectionManager getConnectionManager() {
      if (useSurveyLocalDatabase) {
         return surveyLocalDatabaseConnectionManager;
      } else {
         return globalDatabaseConnectionManager;
      }
   }

   public DatabaseConnection getDatabaseConnection() {
      return getConnectionManager().getDatabaseConnection();
   }

   public DatabaseData getDatabaseData() {
      return getConnectionManager().getDatabaseData();
   }

   public ChangeManager getConnectionChangeManager() {
      return connectionChangeManager;
   }

   public ChangeManager getBusyChangeManager() {
      return busyChangeManager;
   }

   public void setUseSurveyLocal(boolean useSurveyLocal, boolean doImport) {
      if (useSurveyLocalDatabase == useSurveyLocal) {
         return;
      }

      if (useSurveyLocal) {
         globalDatabaseConnected = globalDatabaseConnectionManager.getDatabaseConnection().isConnected();
         globalXml = globalDatabaseConnectionManager.toXml();
         switchToSurveyLocalDatabase(doImport);
      } else {
         globalXml = null;
         switchToGlobalDatabase(doImport);
      }
   }

   private void switchToGlobalDatabase(boolean importFromLocal) {
      SurveyConf surveyConf = lsss.getConfigurationManager().getSurveyConfiguration().getSurveyConf();
      Nation nation = surveyConf.getNation();
      Platform platform = surveyConf.getPlatform();
      Survey survey = surveyConf.getSurvey();

      Log.global.info("Switching to global database");
      surveyLocalDatabaseConnectionManager.closeConnection();
      useSurveyLocalDatabase = false;
      if (globalDatabaseConnected) {
         globalDatabaseConnectionManager.openConnection();
      }
      Path databaseDir = getSurveyLocalDatabaseDir();

      if (importFromLocal && survey != null && databaseDir != null) {
         boolean doImport;
         List<Survey> surveys = globalDatabaseConnectionManager.getDatabaseConnection().executeFetchQuery(LsssQuery.fetch(Survey.class, survey));
         if (!surveys.isEmpty()) {
            int answer = GuiUtils.showOptionDialog(lsss.getFrame(), "Question", """
                        The survey in the survey local database also exists in the global database.

                        Do you want to delete the survey in the global database and import from the survey local database,
                        or do you want to skip import and keep the survey in the global database?

                        """,
                  new String[]{"Delete global survey and import", "Skip import and use global survey"});
            doImport = answer == 0;
         } else {
            doImport = true;
         }

         if (doImport) {
            StatusView statusView = new StatusView("Switching to global database...");
            Component referenceComponent = lsss.getReferenceComponent();
            new WorkerDialog(referenceComponent, statusView.getComponent())
                  .setMinimumSize(new Dimension(400, 0))
                  .startWithoutCancel(() -> {
                     DatabaseImporter.Result result = new DatabaseImporter(lsss, databaseDir, SURVEY_LOCAL_DATABASE)
                           .setInteractiveMode(true)
                           .setStatusListener(statusView::setSecondaryText)
                           .setAskBeforeDeletingExistingSurveys(false)
                           .importFromDatabase(referenceComponent);

                     if (result == DatabaseImporter.Result.DONE) {
                        String message = "Deleting survey local database: " + databaseDir;
                        Log.global.info(message);
                        statusView.setSecondaryText(message);
                        FileUtils.deleteRecursively(databaseDir);
                     }
                  });
         }
      }

      surveyConf.setIfValid(nation, platform, survey);
   }

   private void switchToSurveyLocalDatabase(boolean doImport) {
      Path databaseDir = getSurveyLocalDatabaseDir();
      if (databaseDir == null) {
         return;
      }
      if (doImport) {
         boolean doCreate;
         if (Files.exists(databaseDir)) {
            int answer = GuiUtils.showOptionDialog(lsss.getFrame(), "Question",
                  "The survey local database exists\n" + databaseDir +
                        "\n\nDo you want to connect to the existing survey local database," +
                        "\nor delete the existing and create a new survey local database?\n\n",
                  new String[]{"Connect to existing", "Delete existing and create new"});
            doCreate = answer == 1;
         } else {
            doCreate = true;
         }

         if (doCreate) {
            StatusView statusView = new StatusView("Creating survey local database...");
            new WorkerDialog(lsss.getFrame(), statusView.getComponent())
                  .setMinimumSize(new Dimension(400, 0))
                  .setOnError(e -> lsss.showError("Failed to create survey local database.", e))
                  .startWithoutCancel(() -> {
                     Log.global.info("Exporting survey to local database dir: " + databaseDir);
                     if (Files.exists(databaseDir)) {
                        statusView.setSecondaryText("Deleting old survey local database: " + databaseDir);
                        FileUtils.deleteRecursively(databaseDir);
                     }
                     FileUtils.createDirectories(databaseDir);
                     Survey survey = lsss.getConfigurationManager().getSurveyConf().getSurvey();
                     assert survey != null;
                     new DatabaseExporter(lsss, databaseDir, SURVEY_LOCAL_DATABASE)
                           .setDumpTextFiles(false)
                           .setStatusListener(statusView::setSecondaryText)
                           .exportSurveys(List.of(survey));
                  });
         }
      }
      connectToSurveyLocalDatabase(databaseDir);
   }

   /**
    * Connects to a survey local database. Expects that the database exists.
    *
    * @param databasePath the location of the database
    */
   private void connectToSurveyLocalDatabase(Path databasePath) {
      SurveyConf surveyConf = lsss.getConfigurationManager().getSurveyConfiguration().getSurveyConf();
      Nation nation = surveyConf.getNation();
      Platform platform = surveyConf.getPlatform();
      Survey survey = surveyConf.getSurvey();

      Log.global.info("Switching to survey local database " + databasePath + " " + SURVEY_LOCAL_DATABASE);
      globalDatabaseConnectionManager.closeConnection();
      useSurveyLocalDatabase = true;
      javaDBPluginLocal.setDir(databasePath);
      javaDBPluginLocal.setDatabaseName(SURVEY_LOCAL_DATABASE);
      surveyLocalDatabaseConnectionManager.openConnection();

      surveyConf.setIfValid(nation, platform, survey);
   }

   private @Nullable Path getSurveyLocalDatabaseDir() {
      Path surveyFile = lsss.getSurveyManager().getSurveyFile();
      return surveyFile != null ? surveyFile.resolveSibling(SURVEY_LOCAL_DIR) : null;
   }

   public void close() {
      surveyLocalDatabaseConnectionManager.closeConnection();
      globalDatabaseConnectionManager.closeConnection();
   }
}
