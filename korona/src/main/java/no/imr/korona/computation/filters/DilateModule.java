package no.imr.korona.computation.filters;

import no.imr.korona.computation.BaseMatrixModule;
import no.imr.korona.computation.BaseMatrixModuleComputation;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.UnionList;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;

/**
 * A dilation filter.<br>
 * Each pixel gets the value of the max value of its neighbors.
 * Size of neighborhood vertically is defined by setting {@code VertResolution},
 * if 1 it is a 3*3 neighborhood with the current pixel in the middle of it.<br>
 * In general a 3* (2*{@code VertResolution}+1) neighborhood. The module buffers up one ping thereby delaying the
 * stream by one ping.<br>
 * Informally the filter has the effect of growing all objects by adding pixels around the object edges.
 */
public final class DilateModule extends BaseMatrixModule {
   private static final int RADIUS_X = 1;
   private static final int LEN_X = 2 * RADIUS_X + 1;

   public final IntParameter vertResolution = new IntParameter(
         new Name("VertResolution", "Vertical resolution"),
         1, Unit.COUNT, ValueConstraints.gte(1),
         "Vertical resolution of filter in samples");

   public DilateModule() {
      super(RADIUS_X);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return new UnionList<>(super.getParameters(), List.of(
            vertResolution
      ));
   }

   @Override
   public BaseMatrixModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new DilateModuleComputation(this, computationContext, pingSource);
   }

   private static final class DilateModuleComputation extends BaseMatrixModuleComputation {
      private final int radiusY;
      private final int lenY;

      private DilateModuleComputation(DilateModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);

         radiusY = module.vertResolution.getIntValue();
         lenY = 2 * radiusY + 1;
      }

      @Override
      protected void doPing(int startDepth, int endDepth, int channelIndex) {
         int start = startDepth + radiusY - 1;
         int end = endDepth - radiusY + 1;
         for (int dep = start; dep < end; dep++) {
            float max = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < lenY; i++) {
               for (int j = 0; j < LEN_X; j++) {
                  max = Math.max(getRawDataFromBuffer(channelIndex, dep + i - radiusY, j - RADIUS_X), max);
               }
            }
            setData(channelIndex, dep, max);
         }
      }
   }
}
