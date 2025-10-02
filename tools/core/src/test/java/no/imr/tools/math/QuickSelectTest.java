package no.imr.tools.math;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class QuickSelectTest {
   @Test
   void getInt() {
      int[] values = {2, 0, 5, 1, 6, 4, 9, 7, 8, 3}; // [0..9] in unsorted order
      for (int i = 0; i < values.length; i++) {
         assertEquals(i, QuickSelect.get(values.clone(), i));
      }
   }

   @Test
   void getFloatRandom() {
      JUnitUtils.runWithRandom(random -> {
         float[] a = JUnitUtils.createRandomFloatArray(random, random.nextInt(1, 101));
         float[] b = a.clone();
         int targetIndex = random.nextInt(a.length);
         Arrays.sort(b);
         assertEquals(b[targetIndex], QuickSelect.get(a, targetIndex));
      });
   }

   @Test
   void getDoubleRandom() {
      JUnitUtils.runWithRandom(random -> {
         double[] a = JUnitUtils.createRandomDoubleArray(random, random.nextInt(1, 101));
         double[] b = a.clone();
         int targetIndex = random.nextInt(a.length);
         Arrays.sort(b);
         assertEquals(b[targetIndex], QuickSelect.get(a, targetIndex));
      });
   }

   @Test
   void getListRandom() {
      JUnitUtils.runWithRandom(random -> {
         int n = random.nextInt(1, 101);
         List<Float> a = new ArrayList<>(n);
         for (int i = 0; i < n; i++) {
            a.add(random.nextFloat());
         }
         List<Float> b = new ArrayList<>(a);
         int targetIndex = random.nextInt(n);
         b.sort(null);
         assertEquals(b.get(targetIndex), QuickSelect.get(a, Comparator.comparingDouble(Float::floatValue), targetIndex));
      });
   }
}
