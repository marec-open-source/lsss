package no.imr.lsss.database.export;

import no.imr.lsss.database.DatabaseData;
import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.tables.hibernate.PlatformPK;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.JavaDBUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Class with helper functions for copying the contents of database tables
 * between two DatabaseConnections.
 * It can also dump the contents of copied tables to text files.
 */
final class DatabaseCopyHelper {
   private final DatabaseConnection source;
   private final DatabaseConnection destination;

   private @Nullable Path textDumpDirectory;
   private boolean deleteEmptyTextFiles;

   DatabaseCopyHelper(DatabaseConnection source, DatabaseConnection destination) {
      this.source = source;
      this.destination = destination;
   }

   void doTextDump(Path textDumpDirectory, boolean deleteEmptyTextFiles) {
      this.textDumpDirectory = textDumpDirectory;
      this.deleteEmptyTextFiles = deleteEmptyTextFiles;
   }

   <T extends BaseDatabaseObject> void copyClass(Class<T> clazz) {
      FetchQuery<T> fetchQuery = new FetchQuery<>(clazz);

      DatabaseUtils.copyByInsert(source, fetchQuery, destination);

      if (textDumpDirectory != null) {
         Path file = textDumpDirectory.resolve(DatabaseUtils.getTableName(clazz) + ".txt");
         dumpToTextFile(clazz, fetchQuery, file);
      }
   }

   <T extends BaseDatabaseObject> void copyClassForNation(short nationPK, Class<T> clazz) {
      FetchQuery<T> fetchQuery = new FetchQuery<>(clazz,
            DatabaseData.NATION, nationPK);

      DatabaseUtils.copyByInsert(source, fetchQuery, destination);

      if (textDumpDirectory != null) {
         Path file = textDumpDirectory.resolve(
               "Nation_" + nationPK +
                     "_" + DatabaseUtils.getTableName(clazz) + ".txt");
         dumpToTextFile(clazz, fetchQuery, file);
      }
   }

   <T extends BaseDatabaseObject> void copyClassForPlatform(PlatformPK platformPK, Class<T> clazz) {
      FetchQuery<T> fetchQuery = new FetchQuery<>(clazz,
            DatabaseData.NATION, platformPK.getNation(),
            DatabaseData.PLATFORM, platformPK.getPlatform());

      DatabaseUtils.copyByInsert(source, fetchQuery, destination);

      if (textDumpDirectory != null) {
         Path file = textDumpDirectory.resolve(
               "Nation_" + platformPK.getNation() +
                     "_Platform_" + platformPK.getPlatform() +
                     "_" + DatabaseUtils.getTableName(clazz) + ".txt");
         dumpToTextFile(clazz, fetchQuery, file);
      }
   }

   <T extends BaseDatabaseObject> void copyClassForSurvey(Survey survey, Class<T> clazz) {
      FetchQuery<T> fetchQuery = LsssQuery.fetch(clazz, survey);

      DatabaseUtils.copyByInsert(source, fetchQuery, destination);

      if (textDumpDirectory != null) {
         Path file = textDumpDirectory.resolve(
               "Nation_" + survey.getCompId().getNation() +
                     "_Platform_" + survey.getCompId().getPlatform()
                     + "_Survey_" + survey.getCompId().getSurvey()
                     + "_" + DatabaseUtils.getTableName(clazz) + ".txt");
         dumpToTextFile(clazz, fetchQuery, file);
      }
   }

   private <T extends BaseDatabaseObject> void dumpToTextFile(Class<T> clazz, FetchQuery<T> fetchQuery, Path file) {
      try {
         JavaDBUtils.dumpTableToTextFile(destination, clazz, fetchQuery, file);
         if (deleteEmptyTextFiles && Files.size(file) == 0) {
            Files.delete(file);
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error exporting to text file: " + file, e);
      }
   }
}
