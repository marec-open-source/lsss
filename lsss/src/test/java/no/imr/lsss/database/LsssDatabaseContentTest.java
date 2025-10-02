package no.imr.lsss.database;

import no.imr.tools.ResourceUtils;
import no.imr.tools.database.DatabaseClassChecker;
import no.imr.tools.database.HibernateTestUtils;
import org.junit.jupiter.api.Test;

final class LsssDatabaseContentTest {
   @Test
   void hibernateCfgXml() {
      HibernateTestUtils.testHibernateCfgXml(LsssDatabaseContent.DATABASE_CLASSES, ResourceUtils.getUrl("no/imr/lsss/database/tables/hibernate/hibernate.cfg.xml"));
   }

   @Test
   void databaseClassCheck() throws ReflectiveOperationException {
      DatabaseClassChecker.checkAll(LsssDatabaseContent.DATABASE_CLASSES);
   }
}
