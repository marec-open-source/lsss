package no.imr.korona.computation.filters;

import no.imr.korona.computation.BaseMatrixModule;
import no.imr.korona.computation.BaseMatrixModuleComputation;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.UnionList;
import no.imr.tools.math.MathUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;

/**
 * Detects edges by using sobel horizontal and vertical filter.
 * Filter can be prolonged vertically by setting vertRes.
 * Value if gradient is 0 is -120, and increases linearly with strength of edge.
 * Value is multiplied with scaleFactor.
 */
public final class EdgeDetectionModule extends BaseMatrixModule {
   /**
    * Sobel filters.
    */
   private static final float[][][] M_LIST = {
         /* Sobel horizontal edge     - */ {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}},
         /* Sobel vertical edge       | */ {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}},
         /* Sobel skew edge(not used) \ */ {{0, -1, -2}, {1, 0, -1}, {2, 1, 0}},
         /* Sobel skew edge(not used) / */ {{-2, -1, 0}, {-1, 0, 1}, {0, 1, 2}}
   };

   public final FloatParameter scaleFactor = new FloatParameter(
         new Name("ScaleFactor", "Scale factor"),
         1, Unit.DIMENSIONLESS,
         "Scale of filter");

   public final IntParameter vertResolution = new IntParameter(
         new Name("VertResolution", "Vertical resolution"),
         1, Unit.COUNT, ValueConstraints.gte(0),
         "Vertical resolution of filter in samples");

   public EdgeDetectionModule() {
      super(1, ValueType.LOG_SV, false);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return new UnionList<>(super.getParameters(), List.of(
            scaleFactor,
            vertResolution
      ));
   }

   @Override
   public BaseMatrixModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new EdgeDetectionModuleComputation(this, computationContext, pingSource);
   }

   private static final class EdgeDetectionModuleComputation extends BaseMatrixModuleComputation {
      private float perPixDep;
      private float perPixLen;
      private final float scaleFact;
      private final int vertRes;

      private EdgeDetectionModuleComputation(EdgeDetectionModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);

         scaleFact = module.scaleFactor.getFloatValue();
         vertRes = module.vertResolution.getIntValue();
      }

      @Override
      protected void doPing(int startDepth, int endDepth, int channelIndex) {
         // instead of prolonging filter vertically to weigh up the difference
         // in horizontal/vertical resolution one can have different weights on
         // the horizontal and the vertical filter corresponding to the resolution.
         // This is done if one uses the two commented lines instead of the value 1.
         perPixDep = 1;//getMeterPerSampleDepth(channelIndex)*(3+(vertRes-1)*2);
         perPixLen = 1;//getMeterPerSampleLength(channelIndex)*3;

         for (int d = startDepth + vertRes; d <= endDepth - vertRes; d++) {
            setData(channelIndex, d, update(channelIndex, d));
         }
      }

      private float update(int channelIndex, int sampleIndexDepth) {
         double resAllMat = 0;
         for (int mat = 0; mat < 2; mat++) {
            double resOneMat = 0;
            for (int j = 0; j < 3; j++) {
               resOneMat += M_LIST[mat][0][j] * (double) getRawDataFromBuffer(channelIndex, sampleIndexDepth - vertRes, j - 1);
               resOneMat += M_LIST[mat][1][j] * (double) getRawDataFromBuffer(channelIndex, sampleIndexDepth, j - 1);
               resOneMat += M_LIST[mat][2][j] * (double) getRawDataFromBuffer(channelIndex, sampleIndexDepth + vertRes, j - 1);
            }
            resAllMat += Math.abs(resOneMat) / (mat == 0 ? perPixDep : perPixLen);
         }
         resAllMat = resAllMat * scaleFact - 120;
         if (resAllMat > 0) {
            return 0;
         }
         return MathUtils.avoidInfinity((float) resAllMat);
      }
   }
}
