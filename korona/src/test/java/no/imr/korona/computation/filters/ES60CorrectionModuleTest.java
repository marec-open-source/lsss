package no.imr.korona.computation.filters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ES60CorrectionModuleTest {
   @Test
   void testMeanHistogram() {
      ES60CorrectionModule.ES60CorrectionModuleJob.MeanHistogram meanHist = new ES60CorrectionModule.ES60CorrectionModuleJob.MeanHistogram();
      for (int i = 0; i < 100; i++) {
         meanHist.updateHist(i, 20f);
      }
      assertEquals(20f, meanHist.getAverage());
   }

   @Test
   void testDeviationHistogram() {
      int waveNo = 1265;
      ES60CorrectionModule.ES60CorrectionModuleJob.DeviationHistogram devHist = new ES60CorrectionModule.ES60CorrectionModuleJob.DeviationHistogram(waveNo);

      double sum = 0;
      for (int i = 0; i < 100; i++) {
         double waveAdj = ES60CorrectionModule.ES60CorrectionModuleJob.waveAdjustment(waveNo + i);
         double adj = ES60CorrectionModule.NUM_SAMPLES * waveAdj;
         double dev = 20 - adj;
         sum += dev * dev;
         devHist.updateHist(i, 20f, 20f, 20f);
      }
      assertEquals(sum, devHist.getSum());
   }
}
