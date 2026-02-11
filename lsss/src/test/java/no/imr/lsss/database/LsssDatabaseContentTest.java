package no.imr.lsss.database;

import no.imr.tools.database.DatabaseClassChecker;
import org.junit.jupiter.api.Test;

final class LsssDatabaseContentTest {
   @Test
   void databaseClassCheck() throws ReflectiveOperationException {
      DatabaseClassChecker.checkAll(LsssDatabaseContent.DATABASE_CLASSES);
   }
}
