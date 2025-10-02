package no.imr.korona.computation.filters;

import no.imr.korona.computation.BaseMatrixModule;
import no.imr.korona.computation.BaseMatrixModuleComputation;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.UnionList;
import no.imr.tools.math.QuickSelect;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;

/**
 * Sets pixel value to median of its 14 neighbors (3 ping x 5 samples) and itself provided some criteria are fulfilled.
 * The criterion is that the tested pixel should be larger than the 93% percentile + Delta.
 * Has the effect of removing spikes and of smoothing the data whilst preserving edges.
 */
public final class SpotNoiseModule extends BaseMatrixModule {
   public final FloatParameter delta = new FloatParameter(new Name("Delta"),
         20, Unit.DB, ValueConstraints.gte(0f),
         "Spot-noise candidate if center value > 93% percentile + Delta");

   public final BooleanParameter debug = new BooleanParameter(new Name("Debug"),
         false,
         "Sets value to 0 where spikes are detected");

   public SpotNoiseModule() {
      super(1, ValueType.LOG_SV, false);

      automaticDepthRange.setBooleanValue(false);
      endDepth.setFloatValue(2500);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return new UnionList<>(super.getParameters(), List.of(
            delta,
            debug
      ));
   }

   @Override
   public BaseMatrixModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new SpotNoiseModuleComputation(this, computationContext, pingSource);
   }

   private static final class SpotNoiseModuleComputation extends BaseMatrixModuleComputation {
      private final SpotNoiseModule module;
      private final float[] array1 = new float[15];
      private final float[] array2 = new float[15];

      private SpotNoiseModuleComputation(SpotNoiseModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);

         this.module = module;
      }

      @Override
      protected void doPing(int startDepth, int endDepth, int channelIndex) {
         if (endDepth - startDepth < 5) {
            return;
         }
         float deltaValue = module.delta.getFloatValue();
         int dep = startDepth + 2;
         float[] ringBuffer = array1;
         float[] tmpArray = array2;

         // Row 1
         ringBuffer[0] = getRawDataFromBuffer(channelIndex, dep - 2, -1);
         ringBuffer[1] = getRawDataFromBuffer(channelIndex, dep - 2, 0);
         ringBuffer[2] = getRawDataFromBuffer(channelIndex, dep - 2, 1);

         // Row 2
         ringBuffer[3] = getRawDataFromBuffer(channelIndex, dep - 1, -1);
         ringBuffer[4] = getRawDataFromBuffer(channelIndex, dep - 1, 0);
         ringBuffer[5] = getRawDataFromBuffer(channelIndex, dep - 1, 1);

         // Row 3 - current
         ringBuffer[6] = getRawDataFromBuffer(channelIndex, dep, -1);
         ringBuffer[7] = getRawDataFromBuffer(channelIndex, dep, 0);    // Current point
         ringBuffer[8] = getRawDataFromBuffer(channelIndex, dep, 1);

         // Row 4
         ringBuffer[9] = getRawDataFromBuffer(channelIndex, dep + 1, -1);
         ringBuffer[10] = getRawDataFromBuffer(channelIndex, dep + 1, 0);
         ringBuffer[11] = getRawDataFromBuffer(channelIndex, dep + 1, 1);

         int nextRowIndex = 12;
         int currentPointIndex = 7;

         for (; dep < endDepth - 2; dep++) {

            // Row 5
            ringBuffer[nextRowIndex] = getRawDataFromBuffer(channelIndex, dep + 2, -1);
            ringBuffer[nextRowIndex + 1] = getRawDataFromBuffer(channelIndex, dep + 2, 0);
            ringBuffer[nextRowIndex + 2] = getRawDataFromBuffer(channelIndex, dep + 2, 1);

            System.arraycopy(ringBuffer, 0, tmpArray, 0, tmpArray.length);

            float z = QuickSelect.get(tmpArray, 13); // Test value - 93% percentile
            if (ringBuffer[currentPointIndex] > deltaValue + z) {
               // This pixel is accepted as a spike
               float median = QuickSelect.get(tmpArray, 7);
               float replacementValue = module.debug.getBooleanValue() ? 0 : median;
               setData(channelIndex, dep, replacementValue);  // Switch spike with replacementValue
            }

            nextRowIndex = (nextRowIndex + 3) % 15;
            currentPointIndex = (currentPointIndex + 3) % 15;
         }
      }
   }
}
