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
import no.imr.tools.database.HsqldbConnection;
import no.imr.tools.database.HsqldbUtils;
import no.imr.tools.database.JavaDBConnection;
import no.imr.tools.database.JavaDBUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.database.queries.StatelessDatabaseQuery;
import no.imr.tools.io.FilePredicates;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.HtmlStringBuilder;
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
   private final DatabaseConnection destination;
   private boolean interactiveMode;
   private boolean askBeforeDeletingExistingSurveys = true;
   private AsyncHandle asyncHandle = new AsyncHandle();
   private Consumer<String> statusListener = Utils.emptyConsumer();

   public DatabaseImporter(LSSS lsss) {
      this.lsss = lsss;
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

   public Result importFromTextFiles(@Nullable Component referenceComponent, Path directory) throws IOException, UpgradeException {
      Log.global.info("Importing database from text files: " + directory);
      Path tmpDir = Utils.getTmpDir().resolve("databaseImporterTmp");
      statusListener.accept("Preparing temporary database");
      FileUtils.deleteRecursively(tmpDir);
      try (HsqldbConnection tmpDB = new HsqldbConnection(ConnectionType.INITIALIZE, tmpDir, "tmpDb", LsssDatabaseUtils.getAllDatabaseClasses(lsss))) {
         statusListener.accept("Importing text files into temporary database");
         importTextFilesIntoTmpDatabase(tmpDB.getDatabaseConnection(), directory);
         return doImport(tmpDB.getDatabaseConnection(), referenceComponent);
      } finally {
         statusListener.accept("Deleting temporary directory");
         FileUtils.deleteRecursively(tmpDir);
      }
   }

   private void importTextFilesIntoTmpDatabase(DatabaseConnection databaseConnection, Path directory) throws IOException {
      for (Class<? extends BaseDatabaseObject> databaseClass : LsssDatabaseUtils.getAllDatabaseClasses(lsss)) {
         Pattern pattern = Pattern.compile("(|.*_)" + Pattern.quote(DatabaseUtils.getTableName(databaseClass)) + "\\.txt");
         List<Path> files = FileUtils.listFiles(directory, FilePredicates.fromPattern(pattern));
         for (Path file : files) {
            if (asyncHandle.isCancelled()) {
               return;
            }
            HsqldbUtils.importTableFromTextFile(databaseConnection, databaseClass, file);
         }
      }
   }

   public Result importFromDatabase(@Nullable Component referenceComponent, Path directory, String databaseName) throws IOException, UpgradeException {
      Log.global.info("Importing database: " + directory + " " + databaseName);
      statusListener.accept("Connecting to database");
      try {
         if (HsqldbUtils.isHsqldbDatabase(directory, databaseName)) {
            try (HsqldbConnection source = new HsqldbConnection(ConnectionType.CONNECT, directory, databaseName, LsssDatabaseUtils.getAllDatabaseClasses(lsss))) {
               return doImport(source.getDatabaseConnection(), referenceComponent);
            }
         } else if (JavaDBUtils.isJavaDBDatabase(directory, databaseName)) {
            try (JavaDBConnection source = new JavaDBConnection(ConnectionType.CONNECT, directory, databaseName, LsssDatabaseUtils.getAllDatabaseClasses(lsss))) {
               return doImport(source.getDatabaseConnection(), referenceComponent);
            }
         } else {
            throw new UnsupportedOperationException("Cannot import from database " + directory + " " + databaseName);
         }
      } finally {
         statusListener.accept("Closing database connection");
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

      List<Class<? extends BaseDatabaseObject>> surveyClasses = LsssDatabaseUtils.getSurveyClasses(lsss);
      for (Survey survey : surveysToImport) {
         statusListener.accept("Importing survey: " + DatabaseExporter.surveyToString(survey));
         for (Class<? extends BaseDatabaseObject> surveyClass : surveyClasses) {
            LsssDatabaseUtils.copyClassForSurvey(source, surveyClass, survey, destination, asyncHandle);
            if (asyncHandle.isCancelled()) {
               return Result.CANCELLED;
            }
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

         int answer;
         if (interactiveMode) {
            Map<String, Object> sourceValues = DatabaseUtils.getValues(source, sourceObject);
            Map<String, Object> destinationValues = DatabaseUtils.getValues(destination, destinationObject);

            HtmlStringBuilder sb = new HtmlStringBuilder()
                  .html("An import conflict is found for ").text(DatabaseUtils.getTableName(clazz)).html(":<br><br>")
                  .html("<table border=1 cellspacing=0><tr><th>Column</th><th>Existing value</th><th>Import value</th></tr>");
            destinationValues.forEach((key, destinationValue) -> {
               sb.html("<tr><td>").text(key)
                     .html("</td><td>").text(String.valueOf(destinationValue))
                     .html("</td><td>").text(String.valueOf(sourceValues.get(key)))
                     .html("</td></tr>");
            });
            sb.html("</table>")
                  .html("<br><br>What would you like to do?<br><br>");

            answer = GuiUtils.getNowOrWait(() -> GuiUtils.showOptionDialog(lsss.getFrame(), "Import conflict",
                  sb.build(), new String[]{"Keep existing", "Overwrite with import", "Cancel"}));
         } else {
            answer = 1;
         }
         if (answer == 0) {
            // Keep existing => do nothing.
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
