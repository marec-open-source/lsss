package no.imr.korona.computation.categorization.kdtree;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class KDTreeTest {
   @Test
   void test() {
      KDTree tree = new KDTree(3);

      assertThrows(IllegalArgumentException.class, () -> tree.get(new float[2]));
      assertThrows(IllegalArgumentException.class, () -> tree.add(new float[]{1}, "1"));
      assertThrows(IllegalArgumentException.class, () -> tree.nearestNeighbor(new float[]{1, 2, 3, 4}));

      tree.add(new float[]{0, 0, 0}, "0,0,0");
      tree.add(new float[]{1, 0, 0}, "1,0,0");
      tree.add(new float[]{0, 1, 0}, "0,1,0");
      tree.add(new float[]{0, 0, 1}, "0,0,1");
      tree.add(new float[]{2, 0, 0}, "2,0,0");

      assertEquals("1,0,0", tree.get(new float[]{1, 0, 0}));
      assertNull(tree.get(new float[]{-1, 0, 0}));
      assertEquals("2,0,0", tree.nearestNeighbor(new float[]{1.6f, 0, 0}));
      assertEquals("0,0,1", tree.nearestNeighbor(new float[]{0.5f, 0.6f, 0.7f}));

      tree.rebuild();

      assertEquals("1,0,0", tree.get(new float[]{1, 0, 0}));
      assertNull(tree.get(new float[]{-1, 0, 0}));
      assertEquals("2,0,0", tree.nearestNeighbor(new float[]{1.6f, 0, 0}));
      assertEquals("0,0,1", tree.nearestNeighbor(new float[]{0.5f, 0.6f, 0.7f}));
   }
}
