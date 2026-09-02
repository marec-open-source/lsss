package no.imr.lsss.database;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.Area;
import no.imr.lsss.database.tables.hibernate.AreaPK;
import no.imr.lsss.database.tables.hibernate.DBParameter;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.HsqldbConnection;
import no.imr.tools.database.HsqldbUtils;
import no.imr.tools.database.JavaDBConnection;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.StatelessDatabaseQuery;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.test.UniqueTmpDir;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

final class JavaDBMigrationTest {
   @Test
   void convertJavaDBToHsqldb() throws Exception {
      Path dir = UniqueTmpDir.newSubDir("convertJavaDBToHsqldb");
      String databaseName = "testName";

      LSSS lsss = LsssTestUtils.start(List.of(), List.of());

      List<BaseDatabaseObject> someObjects = List.of(
            new DBParameter(LsssDatabaseContent.VERSION_NAME, LsssDatabaseContent.VERSION_VALUE),
            new Nation((short) 1, "Test nation"),
            new Area(new AreaPK((short) 1, 2), "Test area")
      );
      Set<BaseDatabaseObject> someObjectsAsSet = Set.copyOf(someObjects);

      List<Class<? extends BaseDatabaseObject>> databaseClasses = LsssDatabaseUtils.getAllDatabaseClasses(lsss);

      try (JavaDBConnection javaDBConnection = new JavaDBConnection(ConnectionType.INITIALIZE, dir, databaseName, databaseClasses)) {
         DatabaseConnection databaseConnection = javaDBConnection.getDatabaseConnection();
         assertEquals(Set.of(), getDatabaseContent(databaseClasses, databaseConnection));
         databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.insert(someObjects));
         assertEquals(someObjectsAsSet, getDatabaseContent(databaseClasses, databaseConnection));
      }

      assertTrue(Files.exists(dir.resolve(databaseName)));
      assertFalse(HsqldbUtils.isHsqldbDatabase(dir, databaseName));

      JavaDBMigration.convertJavaDBToHsqldb(lsss, dir, databaseName, new AsyncHandle());

      assertFalse(Files.exists(dir.resolve(databaseName)));
      assertTrue(HsqldbUtils.isHsqldbDatabase(dir, databaseName));

      try (HsqldbConnection hsqldbConnection = new HsqldbConnection(ConnectionType.CONNECT, dir, databaseName, databaseClasses)) {
         DatabaseConnection databaseConnection = hsqldbConnection.getDatabaseConnection();
         assertEquals(someObjectsAsSet, getDatabaseContent(databaseClasses, databaseConnection));
      }

      lsss.close();
      FileUtils.deleteRecursively(dir);
   }

   private static Set<BaseDatabaseObject> getDatabaseContent(List<Class<? extends BaseDatabaseObject>> databaseClasses,
                                                             DatabaseConnection databaseConnection) {
      return databaseClasses.stream()
            .flatMap(c -> databaseConnection.executeFetchQuery(LsssQuery.fetch(c)).stream())
            .collect(Collectors.toSet());
   }
}
