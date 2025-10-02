package no.imr.tools.math.linalg;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class BoxIntersectionTest {
   @Test
   void unitBoxIntersectsTriangle() {
      JUnitUtils.runWithRandom(10, random -> {
         BoxIntersectionMain.Result result = BoxIntersectionMain.testUnitBoxIntersectsTriangle(random);
         assertEquals(result.hasPointInBox(), result.intersects());
      });
   }
}
