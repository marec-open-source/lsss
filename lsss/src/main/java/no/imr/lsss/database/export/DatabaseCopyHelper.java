package no.imr.lsss.database.export;

import no.imr.lsss.database.DatabaseData;
import no.imr.lsss.database.LsssDatabaseUtils;
import no.imr.lsss.database.tables.hibernate.PlatformPK;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.database.DatabaseColumn;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.HsqldbUtils;
import no.imr.tools.database.JavaDBUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Class with helper functions for copying the contents of database tables
 * between two DatabaseConnections.
 * It can also dump the contents of copied tables to text files.
 */
final class DatabaseCopyHelper {
   private final DatabaseConnection source;
   private final DatabaseExporter.Type sourceType;
   private final DatabaseConnection destination;

   private @Nullable Path textDumpDirectory;
   private boolean deleteEmptyTextFiles;

   DatabaseCopyHelper(DatabaseConnection source, DatabaseExporter.Type sourceType, DatabaseConnection destination) {
      this.source = source;
      this.sourceType = sourceType;
      this.destination = destination;
   }

   void doTextDump(Path textDumpDirectory, boolean deleteEmptyTextFiles) {
      this.textDumpDirectory = textDumpDirectory;
      this.deleteEmptyTextFiles = deleteEmptyTextFiles;
   }

   void copyClass(Class<? extends BaseDatabaseObject> clazz, AsyncHandle asyncHandle) {
      LsssDatabaseUtils.copyClass(source, clazz, destination, asyncHandle);
      if (asyncHandle.isCancelled()) {
         return;
      }

      if (textDumpDirectory != null) {
         Path file = textDumpDirectory.resolve(DatabaseUtils.getTableName(clazz) + ".txt");
         dumpToTextFile(clazz, List.of(), file);
      }
   }

   void copyClassForNation(Class<? extends BaseDatabaseObject> clazz, short nationPK, AsyncHandle asyncHandle) {
      LsssDatabaseUtils.copyClassForNation(source, clazz, nationPK, destination, asyncHandle);
      if (asyncHandle.isCancelled()) {
         return;
      }

      if (textDumpDirectory != null) {
         List<Map.Entry<DatabaseColumn, Integer>> criteria = List.of(
               Map.entry(DatabaseData.NATION, (int) nationPK)
         );
         Path file = textDumpDirectory.resolve(
               "Nation_" + nationPK +
                     "_" + DatabaseUtils.getTableName(clazz) + ".txt");
         dumpToTextFile(clazz, criteria, file);
      }
   }

   void copyClassForPlatform(Class<? extends BaseDatabaseObject> clazz, PlatformPK platformPK, AsyncHandle asyncHandle) {
      LsssDatabaseUtils.copyClassForPlatform(source, clazz, platformPK, destination, asyncHandle);
      if (asyncHandle.isCancelled()) {
         return;
      }

      if (textDumpDirectory != null) {
         List<Map.Entry<DatabaseColumn, Integer>> criteria = List.of(
               Map.entry(DatabaseData.NATION, (int) platformPK.getNation()),
               Map.entry(DatabaseData.PLATFORM, (int) platformPK.getPlatform())
         );
         Path file = textDumpDirectory.resolve(
               "Nation_" + platformPK.getNation() +
                     "_Platform_" + platformPK.getPlatform() +
                     "_" + DatabaseUtils.getTableName(clazz) + ".txt");
         dumpToTextFile(clazz, criteria, file);
      }
   }

   void copyClassForSurvey(Class<? extends BaseDatabaseObject> clazz, Survey survey, AsyncHandle asyncHandle) {
      LsssDatabaseUtils.copyClassForSurvey(source, clazz, survey, destination, asyncHandle);
      if (asyncHandle.isCancelled()) {
         return;
      }

      if (textDumpDirectory != null) {
         List<Map.Entry<DatabaseColumn, Integer>> criteria = List.of(
               Map.entry(DatabaseData.NATION, (int) survey.getCompId().getNation()),
               Map.entry(DatabaseData.PLATFORM, (int) survey.getCompId().getPlatform()),
               Map.entry(DatabaseData.SURVEY, survey.getCompId().getSurvey())
         );
         Path file = textDumpDirectory.resolve(
               "Nation_" + survey.getCompId().getNation() +
                     "_Platform_" + survey.getCompId().getPlatform()
                     + "_Survey_" + survey.getCompId().getSurvey()
                     + "_" + DatabaseUtils.getTableName(clazz) + ".txt");
         dumpToTextFile(clazz, criteria, file);
      }
   }

   private void dumpToTextFile(Class<? extends BaseDatabaseObject> clazz, List<Map.Entry<DatabaseColumn, Integer>> criteria, Path file) {
      try {
         switch (sourceType) {
            case HSQLDB -> HsqldbUtils.dumpTableToTextFile(destination, clazz, criteria, file);
            case JavaDB -> JavaDBUtils.dumpTableToTextFile(destination, clazz, criteria, file);
         }
         if (deleteEmptyTextFiles && Files.exists(file) && Files.size(file) == 0) {
            Files.delete(file);
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error exporting to text file: " + file, e);
      }
   }
}
