package no.imr.tools.math;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class PeakFindingTest {
   private static float[] generateSinglePeak(int length) {
      float[] singlePeak = new float[length];
      for (int i = 0; i < length; i++) {
         singlePeak[i] = 2 * (length / 2.0f - Math.abs(length / 2 - i)) / length;
      }

      return singlePeak;
   }

   private static float[] generateSingleMultiDimPeak(int length) {
      float[] singleMultiDimPeak = new float[length];
      for (int i = 0; i < length; i += 2) {
         singleMultiDimPeak[i] = 2 * (length / 2.0f - Math.abs(length / 2 - i)) / length;
      }
      return singleMultiDimPeak;
   }

   private static float[] generateMultipleIdenticalPeaks(int length, int numberOfPeaks) {
      float[] repeatedPeaks = new float[length];
      float[] singlePeak = generateSinglePeak(length / numberOfPeaks);
      int i = 0;
      while (i < numberOfPeaks) {
         System.arraycopy(singlePeak, 0, repeatedPeaks, i * singlePeak.length, singlePeak.length);
         i += 1;
      }
      return repeatedPeaks;
   }

   private static float[] generateMultipleIdenticalMultiDimPeaks(int length, int numberOfPeaks) {
      float[] repeatedPeaks = new float[length];
      float[] singlePeak = generateSingleMultiDimPeak(length / numberOfPeaks);
      int i = 0;
      while (i < numberOfPeaks) {
         System.arraycopy(singlePeak, 0, repeatedPeaks, i * singlePeak.length, singlePeak.length);
         i += 1;
      }
      return repeatedPeaks;
   }

   private static float[] generateSuperimposedPeaks(int length, int numberOfMainPeaks, int numberOfBackgroundPeaks) {
      float[] mainPeaks = generateMultipleIdenticalPeaks(length, numberOfMainPeaks);
      ArrayMath.multiply(mainPeaks, 10.0f);
      ArrayMath.add(mainPeaks, 0.5f);
      float[] backgroundPeaks = generateMultipleIdenticalPeaks(length, numberOfBackgroundPeaks);
      ArrayMath.add(backgroundPeaks, -0.5f);
      ArrayMath.add(mainPeaks, backgroundPeaks);
      return mainPeaks;
   }

   private static float[] generateSuperimposedMultiDimPeaks(int length, int numberOfMainPeaks, int numberOfBackgroundPeaks) {
      float[] mainPeaks = generateMultipleIdenticalMultiDimPeaks(length, numberOfMainPeaks);
      ArrayMath.multiply(mainPeaks, 10.0f);
      ArrayMath.add(mainPeaks, 0.5f);
      float[] backgroundPeaks = generateMultipleIdenticalPeaks(length, numberOfBackgroundPeaks);
      ArrayMath.add(backgroundPeaks, -0.5f);
      ArrayMath.add(mainPeaks, backgroundPeaks);
      return mainPeaks;
   }

   @Test
   void none() {
      assertEquals(List.of(), PeakFinding.getPeaks(new float[]{1, 1, 1, 1, 1, 1, 1}));
      assertEquals(List.of(), PeakFinding.getPeaks(new float[]{1, 2, 3, 4, 5, 6, 7}));
      assertEquals(List.of(), PeakFinding.getPeaks(new float[]{7, 6, 5, 4, 3, 2, 1}));
   }

   @Test
   void single() {
      float[] singlePeakEven = generateSinglePeak(80);
      float[] singlePeakOdd = generateSinglePeak(81);

      assertEquals(1, PeakFinding.getPeaks(singlePeakEven).size());
      assertEquals(1, PeakFinding.getPeaks(singlePeakOdd).size());
   }

   @Test
   void multiple() {
      float[] multiplePeakEven = generateMultipleIdenticalPeaks(800, 11);
      float[] multiplePeakOdd = generateMultipleIdenticalPeaks(801, 11);

      // first and last peak might not get detected, hence expected = 10 +/- 1

      assertEquals(10, PeakFinding.getPeaks(multiplePeakEven).size(), 1);
      assertEquals(10, PeakFinding.getPeaks(multiplePeakOdd).size(), 1);
   }

   @Test
   void singleMultiDim() {
      float[] singlePeakEven = generateSingleMultiDimPeak(80);
      float[] singlePeakOdd = generateSingleMultiDimPeak(81);

      assertEquals(1, PeakFinding.getPeaks(singlePeakEven).size());
      assertEquals(1, PeakFinding.getPeaks(singlePeakOdd).size());
   }

   @Test
   void multipleMultiDim() {
      float[] multiplePeakEven = generateMultipleIdenticalMultiDimPeaks(8000, 54);
      float[] multiplePeakOdd = generateMultipleIdenticalMultiDimPeaks(8001, 54);

      // First and last peak might not get detected, hence expected = 53 +/- 1.
      // Careful with multi peaks: Unexpected results may occur due to a nonunique centre for a peak.

      assertEquals(53, PeakFinding.getPeaks(multiplePeakEven).size(), 1);
      assertEquals(53, PeakFinding.getPeaks(multiplePeakOdd).size(), 1);
   }

   @Test
   void multipleSuperImposedMultiDim() {
      float[] multiplePeakEven = generateSuperimposedMultiDimPeaks(8000, 54, 300);
      float[] multiplePeakOdd = generateSuperimposedMultiDimPeaks(8001, 54, 300);

      // First and last peak might not get detected, hence expected = 53 +/- 1.
      // Careful with multi peaks: Unexpected results may occur due to a nonunique centre for a peak
      // or interference from background peaks.

      assertEquals(53, PeakFinding.getPeaks(multiplePeakEven).size(), 1);
      assertEquals(53, PeakFinding.getPeaks(multiplePeakOdd).size(), 1);
   }
}
