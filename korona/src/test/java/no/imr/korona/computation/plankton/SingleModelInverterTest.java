package no.imr.korona.computation.plankton;

import no.imr.korona.computation.plankton.models.BackscatterModel;
import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.RangeMap;
import no.imr.tools.range.RangeUtils;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class SingleModelInverterTest {
   @Test
   void initialSizesSolveProblem() {
      Pic0Datagram.PlanktonCategory planktonCategoryA = new Pic0Datagram.PlanktonCategory("A", "A", 1, Color.GREEN);
      Pic0Datagram.PlanktonCategory planktonCategoryB = new Pic0Datagram.PlanktonCategory("B", "B", 2, Color.RED);

      // One model, initial sizes solve problem

      PlanktonScatterer<BackscatterModel> scattererA = createScatterer(planktonCategoryA, new double[]{0.5, 1.5, 1.5, 2.5, 2.5, 3.5}, new double[][]{
            new double[]{1, 1, 3},
            new double[]{1, 2, 3},
            new double[]{2, 1, 4},
      });

      SizeHistogram initialSizeHistogram = scattererA.getInitialSizeHistogram(0);
      assertNotNull(initialSizeHistogram);
      double[] initialSizes = initialSizeHistogram.getCenters();
      double[] abundances = {3, 2, 1};

      float[] frequencies = {1, 2, 3};
      float[] sv = computeSv(scattererA.getBackscatterModel(), frequencies, initialSizes, abundances);
      boolean[] validSv = new boolean[sv.length];
      Arrays.fill(validSv, true);

      SingleModelInverter.InversionParameters inversionParameters = new SingleModelInverter.InversionParameters(
            1e-14,
            Double.POSITIVE_INFINITY,
            0,
            4
      );
      SingleModelInverter singleModelInverter = new SingleModelInverter(inversionParameters, frequencies, List.of(scattererA));

      SingleModelInverter.ScattererInversion scattererInversion = singleModelInverter.invert(sv, validSv, 0);
      assertNotNull(scattererInversion);
      assertEquals(scattererA, scattererInversion.getPlanktonScatterer());
      assertEquals(0, scattererInversion.getResidualError(), 1e-14);
      assertArrayEquals(initialSizes, scattererInversion.getSizeHistogram().getCenters());
      assertArrayEquals(abundances, scattererInversion.getSizeHistogram().getAbundances(), 1e-14);

      testInvalidSv(sv, singleModelInverter);

      // One model, initial sizes does not solve problem

      PlanktonScatterer<BackscatterModel> scattererB = createScatterer(planktonCategoryB, new double[]{0.5, 1.5, 1.5, 2.5}, new double[][]{
            new double[]{1, 0},
            new double[]{0, 1},
            new double[]{1, 1},
      });

      singleModelInverter = new SingleModelInverter(inversionParameters, frequencies, List.of(scattererB));
      scattererInversion = singleModelInverter.invert(sv, validSv, 0);
      assertNotNull(scattererInversion);
      assertTrue(scattererInversion.getResidualError() > 1e-14);

      // Two models, initial sizes of one of them solve problem

      singleModelInverter = new SingleModelInverter(inversionParameters, frequencies, List.of(scattererA, scattererB));
      scattererInversion = singleModelInverter.invert(sv, validSv, 0);
      assertNotNull(scattererInversion);
      assertEquals(scattererA, scattererInversion.getPlanktonScatterer());
      assertEquals(0, scattererInversion.getResidualError(), 1e-14);
      assertArrayEquals(initialSizes, scattererInversion.getSizeHistogram().getCenters());
      assertArrayEquals(abundances, scattererInversion.getSizeHistogram().getAbundances(), 1e-14);
   }

   private static void testInvalidSv(float[] sv, SingleModelInverter singleModelInverter) {
      //Adding a sv-value not matching the model changes the solution
      float[] svExt = new float[sv.length + 1];
      System.arraycopy(sv, 0, svExt, 0, sv.length);
      boolean[] validSvExt = new boolean[svExt.length];
      Arrays.fill(validSvExt, true);
      svExt[3] = 1234567;
      SingleModelInverter.ScattererInversion scattererInversion = singleModelInverter.invert(svExt, validSvExt, 0);
      assertNotNull(scattererInversion);
      if (Math.abs(scattererInversion.getResidualError()) <= 1e-14) {
         fail(Double.toString(scattererInversion.getResidualError()));
      }

      // By invalidating the sv-value, the solution again matches the model
      validSvExt[3] = false;
      scattererInversion = singleModelInverter.invert(svExt, validSvExt, 0);
      assertNotNull(scattererInversion);
      assertEquals(0, scattererInversion.getResidualError(), 1e-14);
   }

   private static float[] computeSv(BackscatterModel backscatterModel, float[] frequencies, double[] sizes, double[] abundances) {
      float[] sv = new float[frequencies.length];
      for (int iFreq = 0; iFreq < frequencies.length; iFreq++) {
         float frequency = frequencies[iFreq];
         double sum = 0;
         for (int iSize = 0; iSize < sizes.length; iSize++) {
            double size = sizes[iSize];
            double sigma = backscatterModel.getBackscatter(size, frequency);
            sum += sigma * abundances[iSize];
         }
         sv[iFreq] = (float) sum;
      }
      return sv;
   }

   private static PlanktonScatterer<BackscatterModel> createScatterer(Pic0Datagram.PlanktonCategory planktonCategory, double[] dividers, double[][] matrix) {
      BackscatterModel model = new BackscatterModel() {
         @Override
         public double getBackscatter(double size, double frequency) {
            return matrix[(int) frequency - 1][(int) size - 1];
         }

         @Override
         public double getReducedTS(double ka) {
            return 1;
         }

         @Override
         public double getBioVolume(double size) {
            return size * size * size;
         }
      };

      PlanktonRectangle planktonRectangle = new PlanktonRectangle();
      planktonRectangle.getSizeHistogram().copyDividers(dividers);

      RangeMap<Float, PlanktonRectangle> rangeMap = new ArrayRangeMap<>();
      rangeMap.put(RangeUtils.ALL_FLOATS, planktonRectangle);

      PlanktonScatterer<BackscatterModel> scatterer = new PlanktonScatterer<>("Test", planktonCategory, model);
      scatterer.setInitialSizeHistogramMap(rangeMap);
      return scatterer;
   }
}
