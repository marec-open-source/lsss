package no.imr.tools;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class RandomUtilsTest {
   @Test
   void stream() {
      JUnitUtils.runWithRandom(random -> {
         List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
         assertEquals(0, RandomUtils.stream(random, list, 0).count());
         int n = random.nextInt(list.size());
         assertEquals(n, RandomUtils.stream(random, list, n).count());
         assertEquals(list.size(), RandomUtils.stream(random, list, list.size()).count());
         assertEquals(list.size(), RandomUtils.stream(random, list, list.size() + 1).count());
      });
   }
}
