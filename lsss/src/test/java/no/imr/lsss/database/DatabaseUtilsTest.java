package no.imr.lsss.database;

import no.imr.lsss.database.tables.hibernate.DBParameter;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.ObservationComment;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.JavaDBUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class DatabaseUtilsTest {
   @Test
   void getValues() {
      String databaseName = "DatabaseUtilsTest";
      DatabaseConnection databaseConnection = new DatabaseConnection(
            ConnectionType.INITIALIZE, JavaDBUtils.createInMemoryConfiguration(databaseName), LsssDatabaseContent.DATABASE_CLASSES);
      try {
         DBParameter dbParameter = new DBParameter("someName", "someValue");
         assertEquals(Map.of(
               "parName", "someName",
               "parValue", "someValue"
         ), DatabaseUtils.getValues(databaseConnection, dbParameter));

         Nation nation = new Nation((short) 17, "Nation 17");
         assertEquals(Map.of(
               "nation", (short) 17,
               "nationName", "Nation 17"
         ), DatabaseUtils.getValues(databaseConnection, nation));

         ObservationComment comment = new ObservationComment(
               new ObservationPK((short) 1, (short) 2, 3, 4, 5, (short) 6),
               7, 8, 9, "10");
         assertEquals(Map.of(
               "nation", (short) 1,
               "platform", (short) 2,
               "survey", 3,
               "observationDate", 4,
               "observationTime", 5,
               "observationType", (short) 6,
               "standardComment", 7,
               "mantissa", 8,
               "exp", 9,
               "text", "10"
         ), DatabaseUtils.getValues(databaseConnection, comment));
      } finally {
         databaseConnection.disconnect();
         JavaDBUtils.dropInMemoryDatabase(databaseName);
      }
   }
}
