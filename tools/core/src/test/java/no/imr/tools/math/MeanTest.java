package no.imr.tools.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MeanTest {
   @Test
   void test() {
      Mean m = new Mean();
      assertEquals(0, m.getCount());
      assertEquals(Double.NaN, m.getMean());

      m.update(-1);
      assertEquals(1, m.getCount());
      assertEquals(-1, m.getMean());

      m.update(1);
      assertEquals(2, m.getCount());
      assertEquals(0, m.getMean());

      m.update(3);
      assertEquals(3, m.getCount());
      assertEquals(1, m.getMean());
   }

   @Test
   void update() {
      Mean m = new Mean();
      assertEquals(0, m.getCount());
      assertEquals(Double.NaN, m.getMean());

      m.update(10, 0);
      assertEquals(0, m.getCount());
      assertEquals(Double.NaN, m.getMean());

      m.update(10);
      m.update(10);
      assertEquals(10, m.getMean());

      m.update(30, 2);
      assertEquals(20, m.getMean());

      m.update(40, 4);
      assertEquals(30, m.getMean());
   }
}
