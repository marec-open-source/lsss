package no.imr.korona.computation.filters;

import no.imr.korona.computation.BaseMatrixModule;
import no.imr.korona.computation.BaseMatrixModuleComputation;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.UnionList;
import no.imr.tools.logging.Log;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.math.MathUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;

import java.util.List;

/**
 * Class implementing a 3*3 filter.
 * Either choose a filter from the dropdown menu or specify one manually by setting all 3*3 values.
 */
public final class Filter3X3Module extends BaseMatrixModule {
   private static final float[][][] M_LIST = {
         /* smooth1 */ {{1, 2, 1}, {2, 1, 2}, {1, 2, 1}},
         /* smooth2 */ {{1, 1, 1}, {1, 1, 1}, {1, 1, 1}},
         /* lowpass */ {{1, 2, 1}, {2, 4, 2}, {1, 2, 1}},
         /* sharpen */ {{-1, -1, -1}, {-1, 9, -1}, {-1, -1, -1}},
         /* Laplacian edge (Deltasquared) */ {{1, -2, 1}, {-2, 4, -2}, {1, -2, 1}},
         /* Laplacian edge1)      */ {{1, 1, 1}, {1, -8, 1}, {1, 1, 1}},
         /* Laplacian edge1 minus */ {{-1, -1, -1}, {-1, 8, -1}, {-1, -1, -1}},
         /* Sobel horizontal edge - */ {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}},
         /* Sobel vertical edge   | */ {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}},
         /* Sobel skew edge       \ */ {{0, -1, -2}, {1, 0, -1}, {2, 1, 0}},
         /* Sobel skew edge       / */ {{-2, -1, 0}, {-1, 0, 1}, {0, 1, 2}}
   };

   private static final List<String> ALL_FILTERS = List.of(
         "User defined",
         "/*smooth1*/{{1,2,1},{2,1,2},{1,2,1}}",
         "/*smooth2*/{{1,1,1},{1,1,1},{1,1,1}}",
         "/*lowpass*/{{1,2,1},{2,4,2},{1,2,1}}",
         "/*sharpen*/{{-1,-1,-1},{-1,9,-1},{-1,-1,-1}}",
         "/*Laplacian edge (Deltasquared)*/{{1,-2,1},{-2,4,-2},{1,-2,1}}",
         "/*Laplacian edge1)*/{{1,1,1},{1,-8,1},{1,1,1}}",
         "/*Laplacian edge1 minus*/{{-1,-1,-1},{-1,8,-1},{-1,-1,-1}}",
         "/*Sobel horizontal edge -*/{{-1,-2,-1},{0,0,0},{1,2,1}}",
         "/*Sobel vertical edge |*/{{-1,0,1},{-2,0,2},{-1,0,1}}",
         "/*Sobel skew edge*/{{0,-1,-2},{1,0,-1},{2,1,0}}}",
         "/*Sobel skew edge  / */{{-2,-1,0},{-1,0,1},{0,1,2}}}"
   );

   public final ObjectParameter<String> filterType = new ObjectParameter<>(
         new Name("FilterType", "Filter type"),
         ALL_FILTERS.getFirst(), ALL_FILTERS,
         "Specifies what filter to use");

   public final FloatParameter m00 = new FloatParameter(new Name("00"), 1, Unit.DIMENSIONLESS);
   public final FloatParameter m01 = new FloatParameter(new Name("01"), 1, Unit.DIMENSIONLESS);
   public final FloatParameter m02 = new FloatParameter(new Name("02"), 1, Unit.DIMENSIONLESS);
   public final FloatParameter m10 = new FloatParameter(new Name("10"), 1, Unit.DIMENSIONLESS);
   public final FloatParameter m11 = new FloatParameter(new Name("11"), 1, Unit.DIMENSIONLESS);
   public final FloatParameter m12 = new FloatParameter(new Name("12"), 1, Unit.DIMENSIONLESS);
   public final FloatParameter m20 = new FloatParameter(new Name("20"), 1, Unit.DIMENSIONLESS);
   public final FloatParameter m21 = new FloatParameter(new Name("21"), 1, Unit.DIMENSIONLESS);
   public final FloatParameter m22 = new FloatParameter(new Name("22"), 1, Unit.DIMENSIONLESS);

   private final List<FloatParameter> allParameters = List.of(
         m00, m01, m02,
         m10, m11, m12,
         m20, m21, m22
   );

   public Filter3X3Module() {
      super(1, ValueType.LOG_SV, false);

      filterType.addListenerAndNotify(type -> {
         boolean userDefined = type.equals(ALL_FILTERS.getFirst());
         for (FloatParameter parameter : allParameters) {
            parameter.setEnabled(userDefined);
            parameter.setPersistable(userDefined);
         }
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return new UnionList<>(super.getParameters(), List.of(
            filterType,
            m00, m01, m02,
            m10, m11, m12,
            m20, m21, m22
      ));
   }

   @Override
   public BaseMatrixModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new Filter3X3ModuleComputation(this, computationContext, pingSource);
   }

   private static final class Filter3X3ModuleComputation extends BaseMatrixModuleComputation {
      private final Filter3X3Module module;
      private final float[][] matrix = new float[3][3];

      private Filter3X3ModuleComputation(Filter3X3Module module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);

         this.module = module;

         int filterNumber = ALL_FILTERS.indexOf(module.filterType.getValue());
         if (filterNumber == 0) {
            setUserDefinedMatrix();
         } else {
            setMatrix(M_LIST[filterNumber - 1]);
         }
         float sum = sum(matrix);
         if (sum != 0) {
            divide(matrix, sum);
         }
      }

      @Override
      protected void doPing(int startDepth, int endDepth, int channelIndex) {
         for (int d = startDepth; d <= endDepth; d++) {
            setData(channelIndex, d, update(channelIndex, d));
         }
      }

      private float update(int channelIndex, int sampleIndexDepth) {
         double res = 0;
         for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
               res += matrix[i][j] * (double) getRawDataFromBuffer(channelIndex, sampleIndexDepth + i - 1, j - 1);
            }
         }

         float returnValue = (float) res;
         if (Float.isInfinite(returnValue)) {
            returnValue = MathUtils.avoidInfinity(returnValue);
            Log.global.warning("Filter3x3Module: Clamped infinite value.");
         }
         return returnValue;
      }

      private static float sum(float[][] m) {
         double sum = 0;
         for (float[] row : m) {
            sum += ArrayMath.sum(row);
         }
         return (float) sum;
      }

      private static void divide(float[][] m, float divisor) {
         for (float[] row : m) {
            ArrayMath.divide(row, divisor);
         }
      }

      private void setMatrix(float[][] m) {
         for (int i = 0; i < 3; i++) {
            System.arraycopy(m[i], 0, matrix[i], 0, 3);
         }
      }

      private void setUserDefinedMatrix() {
         for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
               matrix[i][j] = module.allParameters.get(i * 3 + j).getFloatValue();
            }
         }
      }
   }
}
