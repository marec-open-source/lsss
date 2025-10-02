package no.imr.lsss.region.ek500;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SpeciesConverterTest {
   @Test
   void test() {
      SpeciesConverter speciesConverter = new SpeciesConverter();

      speciesConverter.setPlatform(58, 999);
      for (int i = 1; i < 100; i++) {
         int expected = i <= 69 ? i : -1;
         assertEquals(expected, speciesConverter.beiToLSSS(i));
      }

      speciesConverter.setPlatform(352, 1001);
      for (int i = 1; i < 100; i++) {
         int expected = i <= 31 || i >= 41 && i <= 52 ? i : -1;
         assertEquals(expected, speciesConverter.beiToLSSS(i));
      }
   }
}
