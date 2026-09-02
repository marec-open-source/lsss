package no.imr.korona.computation.plankton;

import no.imr.korona.computation.plankton.models.BackscatterModel;
import no.imr.tools.Utils;
import no.imr.tools.math.nnls.ColumnOrderedMatrix;
import no.imr.tools.math.nnls.NNLS;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Solves a plankton inversion problem by finding the single best {@link BackscatterModel}.
 * <p>
 * Reference: SIMFAMI_final_report.pdf
 */
final class SingleModelInverter {
   private final InversionParameters inversionParameters;
   private final float[] frequencies;
   private final List<ScattererInversion> scattererInversions;

   SingleModelInverter(InversionParameters inversionParameters, float[] frequencies,
                       Collection<? extends PlanktonScatterer<?>> planktonScatterers) {
      this.inversionParameters = inversionParameters;
      this.frequencies = frequencies;
      scattererInversions = planktonScatterers.stream()
            .map(ScattererInversion::new)
            .toList();
   }

   /**
    * Finds the best fitting {@link BackscatterModel}.
    *
    * @param sv      measured s<sub>v</sub>
    * @param validSv which values are valid
    * @param depth   the depth for these values
    * @return the result with the smallest residual error, or null if no models fit acceptably
    */
   @Nullable ScattererInversion invert(float[] sv, boolean[] validSv, float depth) {
      ScattererInversion bestScattererInversion = null;
      for (ScattererInversion scattererInversion : scattererInversions) {
         SizeHistogram initialSizeHistogram = scattererInversion.getPlanktonScatterer().getInitialSizeHistogram(depth);
         if (initialSizeHistogram == null) {
            continue;
         }

         scattererInversion.solve(inversionParameters, frequencies, sv, validSv, initialSizeHistogram);
         if (bestScattererInversion == null || scattererInversion.getResidualError() < bestScattererInversion.getResidualError()) {
            bestScattererInversion = scattererInversion;
         }
      }

      if (bestScattererInversion != null && bestScattererInversion.getResidualError() <= inversionParameters.maxResidualErrorThreshold) {
         return bestScattererInversion;
      } else {
         return null;
      }
   }

   /**
    * Parameters controlling the plankton inversion.
    */
   record InversionParameters(
         double minResidualErrorThreshold,
         double maxResidualErrorThreshold,
         double levenbergMarquardtFactor,
         int maxIter
   ) {
   }

   /**
    * Solves the inversion problem for one plankton model.
    */
   static final class ScattererInversion {
      private final PlanktonScatterer<?> planktonScatterer;
      private final SizeHistogram sizeHistogram = new SizeHistogram();
      private final SizeHistogramRedistribution sizeHistogramRedistribution = new SizeHistogramRedistribution();
      private double[] svCalculated = Utils.EMPTY_DOUBLE_ARRAY;
      private double[] svMeasuredExtended = Utils.EMPTY_DOUBLE_ARRAY;
      private ColumnOrderedMatrix matrix = new ColumnOrderedMatrix(0, 0, Utils.EMPTY_DOUBLE_ARRAY);
      private NNLS nnls = new NNLS(matrix);
      private double residualError;

      private ScattererInversion(PlanktonScatterer<?> planktonScatterer) {
         this.planktonScatterer = planktonScatterer;
      }

      PlanktonScatterer<?> getPlanktonScatterer() {
         return planktonScatterer;
      }

      SizeHistogram getSizeHistogram() {
         return sizeHistogram;
      }

      double getResidualError() {
         return residualError;
      }

      private void solve(InversionParameters inversionParameters, float[] frequencies, float[] svMeasured, boolean[] validSvMeasured, SizeHistogram initialSizeHistogram) {
         init(svMeasured, validSvMeasured, initialSizeHistogram);

         for (int i = 1; true; i++) {
            if (sizeHistogram.getAbundances().length == 0) {
               residualError = Double.POSITIVE_INFINITY;
               return;
            }

            defineMatrix(inversionParameters, frequencies, validSvMeasured);
            nnls.solve(svMeasuredExtended, sizeHistogram.getAbundances());
            matrix.multiply(sizeHistogram.getAbundances(), svCalculated);
            residualError = calculateResidualError(svMeasured, svCalculated, validSvMeasured);

            if (Arrays.stream(sizeHistogram.getAbundances()).allMatch(abundance -> abundance == 0)) {
               residualError = Double.POSITIVE_INFINITY;
               return;
            }

            if (residualError <= inversionParameters.minResidualErrorThreshold || i == inversionParameters.maxIter) {
               return;
            }

            if (!sizeHistogramRedistribution.redistribute(sizeHistogram, 1e-8)) {
               return;
            }
         }
      }

      private static int trueCount(boolean[] booleans) {
         int count = 0;
         for (boolean b : booleans) {
            count += b ? 1 : 0;
         }
         return count;
      }

      private void init(float[] svMeasured, boolean[] validSvMeasured, SizeHistogram initialSizeHistogram) {
         int n = initialSizeHistogram.getCenters().length;
         int m = trueCount(validSvMeasured) + n;

         if (svCalculated.length != m) {
            svCalculated = new double[m];
         }
         if (svMeasuredExtended.length != m) {
            svMeasuredExtended = new double[m];
         }
         int i = 0;
         for (int svIndex = 0; svIndex < svMeasured.length; svIndex++) {
            if (validSvMeasured[svIndex]) {
               svMeasuredExtended[i] = svMeasured[svIndex];
               i++;
            }
         }

         sizeHistogram.copyDividers(initialSizeHistogram.getDividers());

         if (m != matrix.getM() || n != matrix.getN()) {
            matrix = new ColumnOrderedMatrix(m, n);
            nnls = new NNLS(matrix);
         }
      }

      private void defineMatrix(InversionParameters inversionParameters, float[] frequencies, boolean[] validSvMeasure) {
         double norm = 0;
         int i = 0;
         for (int fIndex = 0; fIndex < frequencies.length; fIndex++) {
            if (validSvMeasure[fIndex]) {
               float frequency = frequencies[fIndex];
               double[] sizes = sizeHistogram.getCenters();
               for (int j = 0; j < sizes.length; j++) {
                  double size = sizes[j];
                  double x = planktonScatterer.getBackscatterModel().getBackscatter(size, frequency);
                  matrix.set(i, j, x);
                  norm += x * x;
               }
               i++;
            }
         }
         norm = Math.sqrt(norm);

         double normTimesLM = norm * inversionParameters.levenbergMarquardtFactor;
         int F = trueCount(validSvMeasure);
         for (int n = 0; n < matrix.getN(); n++) {
            matrix.set(F + n, n, normTimesLM);
         }
      }

      private static double calculateResidualError(float[] svMeasured, double[] svCalculated, boolean[] validSvMeasure) {
         double p = 0;
         double q = 0;
         int i = 0;
         for (int svIndex = 0; svIndex < svMeasured.length; svIndex++) {
            if (validSvMeasure[svIndex]) {
               double svMeas = svMeasured[svIndex];
               double svCalc = svCalculated[i];
               p += (svCalc - svMeas) * (svCalc - svMeas);
               q += svMeas * svMeas;
               i++;
            }
         }
         return Math.sqrt(p / q);
      }
   }
}
