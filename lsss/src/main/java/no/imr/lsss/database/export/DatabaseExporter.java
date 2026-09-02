package no.imr.lsss.database.export;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.LsssDatabaseUtils;
import no.imr.lsss.database.tables.hibernate.BaseSurveyObject;
import no.imr.lsss.database.tables.hibernate.PlatformPK;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.tables.hibernate.SurveyPK;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.HsqldbConnection;
import no.imr.tools.database.JavaDBConnection;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * DatabaseExporter - class for exporting one or all surveys of a platform to a
 * JavaDB. The location of the DB is taken from the database dir in DataConf.
 * The results can also be dumped to text files.
 */
public final class DatabaseExporter {
   public static final String DATABASE_NAME = "lsssExportDb";

   public enum Type {
      HSQLDB, JavaDB
   }

   private final LSSS lsss;
   private final Path destinationDirectory;
   private final String databaseName;
   private final Type databaseType;
   private final DatabaseConnection source;
   private AsyncHandle asyncHandle = new AsyncHandle();
   private Consumer<String> statusListener = Utils.emptyConsumer();
   private boolean dumpTextFiles = true;
   private boolean deleteEmptyTextFiles;

   public DatabaseExporter(LSSS lsss, Path destinationDirectory, String databaseName, Type databaseType) {
      this.lsss = lsss;
      this.destinationDirectory = destinationDirectory;
      this.databaseName = databaseName;
      this.databaseType = databaseType;
      source = lsss.getDatabaseManager().getDatabaseConnection();
   }

   public DatabaseExporter setAsyncHandle(AsyncHandle asyncHandle) {
      this.asyncHandle = asyncHandle;
      return this;
   }

   public DatabaseExporter setStatusListener(Consumer<String> statusListener) {
      this.statusListener = statusListener;
      return this;
   }

   public DatabaseExporter setDumpTextFiles(boolean dumpTextFiles) {
      this.dumpTextFiles = dumpTextFiles;
      return this;
   }

   public DatabaseExporter setDeleteEmptyTextFiles(boolean deleteEmptyTextFiles) {
      this.deleteEmptyTextFiles = deleteEmptyTextFiles;
      return this;
   }

   private void doExport(Consumer<DatabaseConnection> export) throws IOException {
      statusListener.accept("Clearing destination directory");
      FileUtils.createDirectories(destinationDirectory);
      FileUtils.deleteContentsRecursively(destinationDirectory);
      if (!Files.isWritable(destinationDirectory)) {
         throw new IOException("Directory is not writable: " + destinationDirectory);
      }
      statusListener.accept("Preparing export database");
      try {
         switch (databaseType) {
            case JavaDB -> {
               try (JavaDBConnection destinationJavaDB = new JavaDBConnection(ConnectionType.INITIALIZE, destinationDirectory, databaseName, LsssDatabaseUtils.getAllDatabaseClasses(lsss))) {
                  export.accept(destinationJavaDB.getDatabaseConnection());
               }
            }
            case HSQLDB -> {
               try (HsqldbConnection destinationHsqldb = new HsqldbConnection(ConnectionType.INITIALIZE, destinationDirectory, databaseName, LsssDatabaseUtils.getAllDatabaseClasses(lsss))) {
                  export.accept(destinationHsqldb.getDatabaseConnection());
               }
            }
         }
      } finally {
         statusListener.accept("Closing export database");
      }
   }

   public void exportReferenceTables() throws IOException {
      Log.global.info("Exporting reference tables to: " + destinationDirectory);
      doExport(this::doExportReferenceTables);
   }

   private void doExportReferenceTables(DatabaseConnection destination) {
      DatabaseCopyHelper databaseCopyHelper = new DatabaseCopyHelper(source, databaseType, destination);
      if (dumpTextFiles) {
         databaseCopyHelper.doTextDump(destinationDirectory, deleteEmptyTextFiles);
      }
      for (Class<? extends BaseDatabaseObject> databaseClass : LsssDatabaseUtils.getAllDatabaseClasses(lsss)) {
         if (BaseSurveyObject.class.isAssignableFrom(databaseClass)) {
            continue;
         }
         if (asyncHandle.isCancelled()) {
            return;
         }
         statusListener.accept("Exporting table: " + DatabaseUtils.getTableName(databaseClass));
         databaseCopyHelper.copyClass(databaseClass, asyncHandle);
      }
   }

   public void exportEntireDatabase() throws IOException {
      Log.global.info("Exporting entire database to: " + destinationDirectory);
      doExport(this::doExportEntireDatabase);
   }

   private void doExportEntireDatabase(DatabaseConnection destination) {
      DatabaseCopyHelper databaseCopyHelper = new DatabaseCopyHelper(source, databaseType, destination);
      if (dumpTextFiles) {
         databaseCopyHelper.doTextDump(destinationDirectory, deleteEmptyTextFiles);
      }
      for (Class<? extends BaseDatabaseObject> databaseClass : LsssDatabaseUtils.getAllDatabaseClasses(lsss)) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         statusListener.accept("Exporting table: " + DatabaseUtils.getTableName(databaseClass));
         databaseCopyHelper.copyClass(databaseClass, asyncHandle);
      }
   }

   public void exportSurveys(List<Survey> surveys) throws IOException {
      Log.global.info("Exporting " + surveys.size() + " surveys to: " + destinationDirectory);
      doExport(databaseConnection -> doExportSurveys(surveys, databaseConnection));
   }

   private void doExportSurveys(List<Survey> surveys, DatabaseConnection destination) {
      DatabaseCopyHelper databaseCopyHelper = new DatabaseCopyHelper(source, databaseType, destination);
      if (dumpTextFiles) {
         databaseCopyHelper.doTextDump(destinationDirectory, deleteEmptyTextFiles);
      }
      for (Class<? extends BaseDatabaseObject> systemClass : LsssDatabaseUtils.getSystemClasses(lsss)) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         statusListener.accept("Exporting table: " + DatabaseUtils.getTableName(systemClass));
         databaseCopyHelper.copyClass(systemClass, asyncHandle);
      }

      Set<Short> nationPKs = LsssDatabaseUtils.toNationPKs(surveys);
      for (Class<? extends BaseDatabaseObject> nationClass : LsssDatabaseUtils.getNationClasses(lsss)) {
         statusListener.accept("Exporting table: " + DatabaseUtils.getTableName(nationClass));
         for (short nationPK : nationPKs) {
            if (asyncHandle.isCancelled()) {
               return;
            }
            databaseCopyHelper.copyClassForNation(nationClass, nationPK, asyncHandle);
         }
      }

      Set<PlatformPK> platformPKs = LsssDatabaseUtils.toPlatformPKs(surveys);
      for (Class<? extends BaseDatabaseObject> platformClass : LsssDatabaseUtils.getPlatformClasses(lsss)) {
         statusListener.accept("Exporting table: " + DatabaseUtils.getTableName(platformClass));
         for (PlatformPK platformPK : platformPKs) {
            if (asyncHandle.isCancelled()) {
               return;
            }
            databaseCopyHelper.copyClassForPlatform(platformClass, platformPK, asyncHandle);
         }
      }

      List<Class<? extends BaseDatabaseObject>> surveyClasses = LsssDatabaseUtils.getSurveyClasses(lsss);
      for (Survey survey : surveys) {
         statusListener.accept("Exporting survey: " + surveyToString(survey));
         for (Class<? extends BaseDatabaseObject> surveyClass : surveyClasses) {
            if (asyncHandle.isCancelled()) {
               return;
            }
            databaseCopyHelper.copyClassForSurvey(surveyClass, survey, asyncHandle);
         }
      }
   }

   public static String surveyToString(Survey survey) {
      SurveyPK surveyPK = survey.getCompId();
      return "[" + surveyPK.getNation() + ", " + surveyPK.getPlatform() + ", " + surveyPK.getSurvey() + "] \"" + survey.getSurveyTitle() + "\"";
   }
}
