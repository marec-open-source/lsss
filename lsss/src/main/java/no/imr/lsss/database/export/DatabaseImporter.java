package no.imr.lsss.database.export;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.LsssDatabaseUtils;
import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.DatabaseContent;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.JavaDBConnection;
import no.imr.tools.database.JavaDBUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.database.queries.StatelessDatabaseQuery;
import no.imr.tools.io.FilePredicates;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.upgrade.UpgradeException;
import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Imports a JavaDB database to the current LSSS database. It requires that the surveys
 * in the import database come from one and only one platform, and that
 * this platform already exists in the destination database.
 * It also checks that the nation-wide tables of
 * BiologicalSpecies and Area are the same. If the user still wants to
 * import, the tables of the destination database are kept.
 * Finally, the import checks if the survey already exists in the database, and
 * asks if it should be overwritten.
 */
public final class DatabaseImporter {
   private final LSSS lsss;
   private final Path sourceDirectory;
   private final String databaseName;
   private final DatabaseConnection destination;
   private boolean interactiveMode;
   private boolean askBeforeDeletingExistingSurveys = true;
   private AsyncHandle asyncHandle = new AsyncHandle();
   private Consumer<String> statusListener = Utils.emptyConsumer();

   public DatabaseImporter(LSSS lsss, Path sourceDirectory, String databaseName) {
      this.lsss = lsss;
      this.sourceDirectory = sourceDirectory;
      this.databaseName = databaseName;
      destination = lsss.getDatabaseManager().getDatabaseConnection();
   }

   public DatabaseImporter setInteractiveMode(boolean interactiveMode) {
      this.interactiveMode = interactiveMode;
      return this;
   }

   public DatabaseImporter setAskBeforeDeletingExistingSurveys(boolean askBeforeDeletingExistingSurveys) {
      this.askBeforeDeletingExistingSurveys = askBeforeDeletingExistingSurveys;
      return this;
   }

   public DatabaseImporter setAsyncHandle(AsyncHandle asyncHandle) {
      this.asyncHandle = asyncHandle;
      return this;
   }

   public DatabaseImporter setStatusListener(Consumer<String> statusListener) {
      this.statusListener = statusListener;
      return this;
   }

   public Result importFromTextFiles(Component referenceComponent) throws IOException, UpgradeException {
      Log.global.info("Importing database from text files: " + sourceDirectory);
      Path tmpDir = Utils.getTmpDir().resolve("databaseImporterTmp");
      statusListener.accept("Preparing temporary database");
      FileUtils.deleteRecursively(tmpDir);
      try (JavaDBConnection tmpJavaDB = new JavaDBConnection(ConnectionType.INITIALIZE, tmpDir, "tmpDb", LsssDatabaseUtils.getDatabaseClasses(lsss))) {
         statusListener.accept("Importing text files into temporary database");
         importTextFilesIntoTmpDatabase(tmpJavaDB.getDatabaseConnection());
         return doImport(tmpJavaDB.getDatabaseConnection(), referenceComponent);
      } finally {
         statusListener.accept("Deleting temporary directory");
         FileUtils.deleteRecursively(tmpDir);
      }
   }

   private void importTextFilesIntoTmpDatabase(DatabaseConnection databaseConnection) throws IOException {
      for (Class<? extends BaseDatabaseObject> databaseClass : LsssDatabaseUtils.getDatabaseClasses(lsss)) {
         Pattern pattern = Pattern.compile("(|.*_)" + Pattern.quote(DatabaseUtils.getTableName(databaseClass)) + "\\.txt");
         List<Path> files = FileUtils.listFiles(sourceDirectory, FilePredicates.fromPattern(pattern));
         for (Path file : files) {
            if (asyncHandle.isCancelled()) {
               return;
            }
            JavaDBUtils.importTableFromTextFile(databaseConnection, databaseClass, file);
         }
      }
   }

   public Result importFromDatabase(@Nullable Component referenceComponent) throws UpgradeException {
      Log.global.info("Importing database: " + sourceDirectory + " " + databaseName);
      statusListener.accept("Connecting to database");
      try (JavaDBConnection sourceJavaDB = new JavaDBConnection(ConnectionType.CONNECT, sourceDirectory, databaseName, LsssDatabaseUtils.getDatabaseClasses(lsss))) {
         try {
            return doImport(sourceJavaDB.getDatabaseConnection(), referenceComponent);
         } finally {
            statusListener.accept("Closing to database");
         }
      }
   }

   private Result doImport(DatabaseConnection source, @Nullable Component referenceComponent) throws UpgradeException {
      if (asyncHandle.isCancelled()) {
         return Result.CANCELLED;
      }
      statusListener.accept("Upgrading database if necessary");
      for (FeaturePlugin plugin : lsss.getPluginManager().getFeaturePlugins()) {
         DatabaseContent databaseContent = plugin.getDatabaseContent();
         if (databaseContent == null) {
            continue;
         }
         DatabaseContent.UpgradeResult upgradeResult = databaseContent.doUpgradeIfNecessary(source, referenceComponent, interactiveMode);
         if (upgradeResult == DatabaseContent.UpgradeResult.CANCELLED) {
            return Result.CANCELLED;
         }
      }

      for (Class<? extends BaseDatabaseObject> systemClass : LsssDatabaseUtils.getSystemClasses(lsss)) {
         if (asyncHandle.isCancelled()) {
            return Result.CANCELLED;
         }
         statusListener.accept("Importing table: " + DatabaseUtils.getTableName(systemClass));
         if (!doImportTable(source, destination, systemClass)) {
            return Result.CANCELLED;
         }
      }

      for (Class<? extends BaseDatabaseObject> nationClass : LsssDatabaseUtils.getNationClasses(lsss)) {
         if (asyncHandle.isCancelled()) {
            return Result.CANCELLED;
         }
         statusListener.accept("Importing table: " + DatabaseUtils.getTableName(nationClass));
         if (!doImportTable(source, destination, nationClass)) {
            return Result.CANCELLED;
         }
      }

      for (Class<? extends BaseDatabaseObject> platformClass : LsssDatabaseUtils.getPlatformClasses(lsss)) {
         if (asyncHandle.isCancelled()) {
            return Result.CANCELLED;
         }
         statusListener.accept("Importing table: " + DatabaseUtils.getTableName(platformClass));
         if (!doImportTable(source, destination, platformClass)) {
            return Result.CANCELLED;
         }
      }

      statusListener.accept("Checking surveys to import");
      List<Survey> surveysToDelete = new ArrayList<>();
      List<Survey> surveysToImport = new ArrayList<>();
      for (Survey survey : source.executeFetchQuery(LsssQuery.fetch(Survey.class))) {
         List<Survey> existingSurveys = destination.executeFetchQuery(LsssQuery.fetch(Survey.class, survey));
         if (existingSurveys.isEmpty()) {
            surveysToImport.add(survey);
         } else {
            String[] options = {"Skip this survey", "Delete existing and re-import", "Cancel"};
            int answer = 1;
            if (interactiveMode && askBeforeDeletingExistingSurveys) {
               answer = GuiUtils.getNowOrWait(() -> GuiUtils.showOptionDialog(lsss.getFrame(), "Import conflict",
                     "Survey " + DatabaseExporter.surveyToString(survey) + " already exists in the database.\nWhat would you like to do?",
                     options));
            }

            if (answer == 0) {
               // skip
            } else if (answer == 1) {
               surveysToDelete.add(survey);
               surveysToImport.add(survey);
            } else {
               return Result.CANCELLED;
            }
         }
      }

      for (Survey survey : surveysToDelete) {
         if (asyncHandle.isCancelled()) {
            return Result.CANCELLED;
         }
         statusListener.accept("Deleting survey: " + DatabaseExporter.surveyToString(survey));
         LsssDatabaseUtils.deleteSurvey(lsss, destination, survey);
      }

      DatabaseCopyHelper databaseCopyHelper = new DatabaseCopyHelper(source, destination);
      List<Class<? extends BaseDatabaseObject>> surveyClasses = LsssDatabaseUtils.getSurveyClasses(lsss);
      for (Survey survey : surveysToImport) {
         statusListener.accept("Importing survey: " + DatabaseExporter.surveyToString(survey));
         for (Class<? extends BaseDatabaseObject> surveyClass : surveyClasses) {
            if (asyncHandle.isCancelled()) {
               return Result.CANCELLED;
            }
            databaseCopyHelper.copyClassForSurvey(survey, surveyClass);
         }
      }

      return Result.DONE;
   }

   private <T extends BaseDatabaseObject> boolean doImportTable(DatabaseConnection source, DatabaseConnection destination, Class<T> clazz) {
      FetchQuery<T> fetchQuery = LsssQuery.fetch(clazz);
      Map<Object, T> destinationObjects = destination.executeFetchQuery(fetchQuery).stream()
            .collect(Collectors.toMap(BaseDatabaseObject::primaryKey, Function.identity()));
      for (T sourceObject : source.executeFetchQuery(fetchQuery)) {
         T destinationObject = destinationObjects.get(sourceObject.primaryKey());
         if (destinationObject == null) {
            destination.executeStatelessQuery(StatelessDatabaseQuery.insert(sourceObject));
            continue;
         }
         if (destinationObject.equals(sourceObject)) {
            continue;
         }

         String[] options = {"Keep existing", "Overwrite with import", "Cancel"};
         int answer = 1;

         if (interactiveMode) {
            Map<String, Object> sourceValues = DatabaseUtils.getValues(source, sourceObject);
            Map<String, Object> destinationValues = DatabaseUtils.getValues(destination, destinationObject);

            StringBuilder sb = new StringBuilder("<table border=1 cellspacing=0><tr><th>Column</th><th>Existing value</th><th>Import value</th></tr>");
            destinationValues.forEach((key, destinationValue) -> {
               sb.append("<tr><td>").append(key)
                     .append("</td><td>").append(destinationValue)
                     .append("</td><td>").append(sourceValues.get(key))
                     .append("</td></tr>");
            });
            sb.append("</table>");

            answer = GuiUtils.getNowOrWait(() -> GuiUtils.showOptionDialog(lsss.getFrame(), "Import conflict",
                  "<html>An import conflict is found for " + DatabaseUtils.getTableName(clazz) + ":<br><br>" + sb +
                        "<br><br>What would you like to do?<br><br>",
                  options));
         }
         if (answer == 0) {
            // Keep existing => do nothing
         } else if (answer == 1) {
            destination.executeStatelessQuery(StatelessDatabaseQuery.update(sourceObject));
         } else {
            return false;
         }
      }
      return true;
   }

   public enum Result {
      DONE, CANCELLED
   }
}
