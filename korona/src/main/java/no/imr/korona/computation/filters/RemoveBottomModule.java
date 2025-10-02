package no.imr.korona.computation.filters;

import no.imr.korona.computation.BaseMatrixModule;
import no.imr.korona.computation.BaseMatrixModuleComputation;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.data.datagrams.Dep0Datagram;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.UnionList;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;

/**
 * Sets samples around bottom to configured fill value.
 */
public final class RemoveBottomModule extends BaseMatrixModule {
   public final BooleanParameter useNeighbours = new BooleanParameter(
         new Name("UseNeighbours", "Use neighbours"),
         false,
         "Use minimum depth from current ping and neighbours");

   public final IntParameter above = new IntParameter(
         new Name("Above"),
         0, Unit.COUNT, ValueConstraints.gte(0),
         "Upper bottom limit is this many samples above bottom");

   public final IntParameter below = new IntParameter(
         new Name("Below"),
         100, Unit.COUNT, ValueConstraints.gte(0),
         "Lower bottom limit is this many samples below bottom");

   public final BooleanParameter removeAboveBottom = new BooleanParameter(
         new Name("RemoveAboveBottom", "Remove above bottom"),
         false,
         "Remove values above upper bottom limit");

   public final BooleanParameter removeBottom = new BooleanParameter(
         new Name("RemoveBottom", "Remove bottom"),
         true,
         "Remove values between upper and lower bottom limits");

   public final BooleanParameter removeBelowBottom = new BooleanParameter(
         new Name("RemoveBelowBottom", "Remove below bottom"),
         false,
         "Remove values below lower bottom limit");

   public final FloatParameter pixelValue = new FloatParameter(
         new Name("PixelValue", "Fill value"),
         -120, Unit.DB,
         "Value to set pixels to");

   public RemoveBottomModule() {
      super(1);

      automaticDepthRange.addListenerAndNotify(automaticDepthRange -> {
         above.setEnabled(automaticDepthRange);
         below.setEnabled(automaticDepthRange);
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return new UnionList<>(super.getParameters(), List.of(
            useNeighbours,
            above,
            below,
            removeAboveBottom,
            removeBottom,
            removeBelowBottom,
            pixelValue
      ));
   }

   @Override
   public BaseMatrixModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new RemoveBottomModuleComputation(this, computationContext, pingSource);
   }

   private static final class RemoveBottomModuleComputation extends BaseMatrixModuleComputation {
      private final RemoveBottomModule module;
      private final float fillValue;
      private float previousBottomDepth = Float.POSITIVE_INFINITY;

      private RemoveBottomModuleComputation(RemoveBottomModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);

         this.module = module;
         fillValue = valueType.getValueFromLogSv(module.pixelValue.getFloatValue());
      }

      @Override
      protected void doPing(int startDepth, int endDepth, int channelIndex) {
         int sampleCount = getSampleCount(0, channelIndex);
         int beginSampleIndex;
         int endSampleIndex;
         if (module.automaticDepthRange.getBooleanValue()) {
            int bottomSampleIndex;
            if (module.useNeighbours.getBooleanValue()) {
               bottomSampleIndex = findMinNeighbourBottomSampleIndex(channelIndex);
            } else {
               bottomSampleIndex = endDepth;
            }
            beginSampleIndex = Math.clamp(bottomSampleIndex - module.above.getIntValue(), 0, sampleCount);
            endSampleIndex = Math.clamp(bottomSampleIndex + module.below.getIntValue(), 0, sampleCount);
         } else {
            beginSampleIndex = startDepth;
            endSampleIndex = endDepth;
         }

         if (module.removeAboveBottom.getBooleanValue()) {
            for (int sampleIndex = 0; sampleIndex < beginSampleIndex; sampleIndex++) {
               setData(channelIndex, sampleIndex, fillValue);
            }
         }
         if (module.removeBottom.getBooleanValue()) {
            for (int sampleIndex = beginSampleIndex; sampleIndex < endSampleIndex; sampleIndex++) {
               setData(channelIndex, sampleIndex, fillValue);
            }
         }
         if (module.removeBelowBottom.getBooleanValue()) {
            for (int sampleIndex = endSampleIndex; sampleIndex < sampleCount; sampleIndex++) {
               setData(channelIndex, sampleIndex, fillValue);
            }
         }
      }

      /**
       * Finds the minimum depth of neighbours and this ping.
       *
       * @param channelIndex a channel index
       * @return minimum depth index of neighbours and this ping
       */
      private int findMinNeighbourBottomSampleIndex(int channelIndex) {
         Dep0Datagram currentDep0Datagram = getPing(0).getDep0Datagram();
         if (currentDep0Datagram == null) {
            previousBottomDepth = Float.POSITIVE_INFINITY;
            return getSampleCount(0, channelIndex) - 1;
         } else {
            float minDepth = Math.min(previousBottomDepth, currentDep0Datagram.getMinimumDepth());

            Dep0Datagram nextDep0Datagram = getPing(1).getDep0Datagram();
            if (nextDep0Datagram != null) {
               minDepth = Math.min(minDepth, nextDep0Datagram.getMinimumDepth());
            }

            previousBottomDepth = currentDep0Datagram.getMinimumDepth();
            return depthMeterToSampleIndex(minDepth, channelIndex);
         }
      }
   }
}
