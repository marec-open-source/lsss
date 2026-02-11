package no.imr.lsss.database.reports;

import no.imr.lsss.database.LsssDatabaseContent;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.AcousticCategoryPK;
import no.imr.lsss.database.tables.hibernate.Nation;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

final class GetIcesTest {
   @Test
   void acousticCategory() {
      assertEquals("HER", GetIces.acousticCategory(new AcousticCategory(new AcousticCategoryPK((short) 0, (short) 0, 12), (short) 0, "SILD", "HERR", "Sild", "Herring")));
      assertEquals("MAC", GetIces.acousticCategory(new AcousticCategory(new AcousticCategoryPK((short) 0, (short) 0, 21), (short) 0, "MAKRE", "MACKE", "Makrell", "Macerel")));
      assertEquals("UNK", GetIces.acousticCategory(new AcousticCategory(new AcousticCategoryPK((short) 0, (short) 0, 0), (short) 0, "X", "X", "X", "X")));
   }

   @Test
   void nation() throws IOException {
      assertEquals("AD", GetIces.nation(new Nation((short) 20, "Andorra")));
      assertEquals("DK", GetIces.nation(new Nation((short) 208, "Denmark")));
      assertEquals("IS", GetIces.nation(new Nation((short) 352, "Iceland")));
      assertEquals("NO", GetIces.nation(new Nation((short) 578, "Norway")));
      assertEquals("??", GetIces.nation(new Nation((short) 0, "X")));

      LsssDatabaseContent.loadDefaultContent(c -> c == Nation.class).getContent().get(Nation.class).stream()
            .map(Nation.class::cast)
            .filter(nation -> nation.getNation() != 0)
            .forEach(nation -> assertNotEquals("??", GetIces.nation(nation), nation::toString));
   }
}
