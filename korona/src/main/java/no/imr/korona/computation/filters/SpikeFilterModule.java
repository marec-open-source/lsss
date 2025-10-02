package no.imr.korona.computation.filters;

import no.imr.korona.computation.BaseMatrixModule;
import no.imr.korona.computation.BaseMatrixModuleComputation;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.data.datagrams.subdatagrams.plot.PlotParameterUtils;
import no.imr.korona.data.datagrams.subdatagrams.plot.pojo.PlotParameterConfig;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.incubator.KoronaIncubatorFeatureToggles;
import no.imr.tools.UnionList;
import no.imr.tools.math.Median;
import no.imr.tools.math.RunningMedian;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;

/**
 * A median-based spike filter.
 */
public abstract class SpikeFilterModule extends BaseMatrixModule {
   private static final int NX = 2;

   public final FloatParameter totalDelta = new FloatParameter(
         new Name("TotalDelta", "Total delta"),
         15, Unit.DB, ValueConstraints.gte(0f),
         "Minimum difference of current sample to search window median to be a spike candidate");

   public final FloatParameter verticalDelta = new FloatParameter(
         new Name("VerticalDelta", "Vertical delta"),
         15, Unit.DB, ValueConstraints.gte(0f),
         "Minimum difference of search column median to (most) neighbouring pings to be a spike candidate");

   public final BooleanParameter debug = new BooleanParameter(
         new Name("Debug"),
         false,
         "Sets value to 0 where spikes are detected");

   public final ObjectParameter<VerticalUnit> verticalUnit = new ObjectParameter<>(
         new Name("VerticalUnit", "Vertical unit"),
         VerticalUnit.DURATION, VerticalUnit.values(),
         "Unit for the height of the search window and the search columns");

   public final IntParameter verticalMedianSearchHeight = new IntParameter(
         new Name("VerticalMedianSearchHeight", "Vertical median search height"),
         7, Unit.COUNT, ValueConstraints.gte(0),
         "Half the height of the search column in number of samples");

   public final IntParameter windowMedianSearchHeight = new IntParameter(
         new Name("WindowMedianSearchHeight", "Window median search height"),
         35, Unit.COUNT, ValueConstraints.gte(0),
         "Half the height of the search window in number of samples");

   public final FloatParameter verticalMedianSearchDuration = new FloatParameter(
         new Name("VerticalMedianSearchDuration", "Vertical median search duration"),
         0.9f, Unit.MILLISECONDS, ValueConstraints.gte(0f),
         "Half the height of the search column in milliseconds");

   public final FloatParameter windowMedianSearchDuration = new FloatParameter(
         new Name("WindowMedianSearchDuration", "Window median search duration"),
         4.4f, Unit.MILLISECONDS, ValueConstraints.gte(0f),
         "Half the height of the search window in milliseconds");

   public final FloatParameter verticalMedianSearchDistance = new FloatParameter(
         new Name("VerticalMedianSearchDistance", "Vertical median search distance"),
         1.3f, Unit.METER, ValueConstraints.gte(0f),
         "Half the height of the search column in meters");

   public final FloatParameter windowMedianSearchDistance = new FloatParameter(
         new Name("WindowMedianSearchDistance", "Window median search distance"),
         6.6f, Unit.METER, ValueConstraints.gte(0f),
         "Half the height of the search window in meters");

   public final BooleanParameter writePlotParameters = new BooleanParameter(
         new Name("WritePlotParameters", "Write plot parameters"),
         false,
         "Incubating feature: Writes some values for use in echogram plot");

   protected SpikeFilterModule() {
      super(NX, ValueType.LOG_SV, false);

      // Let default behaviour be to remove spikes from all channels.
      onlyLast.setBooleanValue(false);

      automaticDepthRange.setBooleanValue(false);
      endDepth.setFloatValue(2500);

      verticalUnit.addListenerAndNotify(unit -> {
         verticalMedianSearchHeight.setVisible(unit == VerticalUnit.SAMPLES);
         windowMedianSearchHeight.setVisible(unit == VerticalUnit.SAMPLES);

         verticalMedianSearchDuration.setVisible(unit == VerticalUnit.DURATION);
         windowMedianSearchDuration.setVisible(unit == VerticalUnit.DURATION);

         verticalMedianSearchDistance.setVisible(unit == VerticalUnit.DISTANCE);
         windowMedianSearchDistance.setVisible(unit == VerticalUnit.DISTANCE);
      });

      writePlotParameters.setVisible(KoronaIncubatorFeatureToggles.PLOT_PARAMETERS);
      writePlotParameters.setPersistable(KoronaIncubatorFeatureToggles.PLOT_PARAMETERS);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return new UnionList<>(super.getParameters(), List.of(
            totalDelta,
            verticalDelta,
            debug,
            verticalUnit,
            verticalMedianSearchHeight,
            windowMedianSearchHeight,
            verticalMedianSearchDuration,
            windowMedianSearchDuration,
            verticalMedianSearchDistance,
            windowMedianSearchDistance,
            writePlotParameters
      ));
   }

   protected abstract boolean isCenterValueDifferent(float centerData, float median);

   protected abstract float computeTestValue(float centerVerticalMedian);

   protected abstract boolean spikeTest(float testValue, float verticalMedian);

   @Override
   public BaseMatrixModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new SpikeFilterModuleComputation(this, computationContext, pingSource);
   }

   private static final class SpikeFilterModuleComputation extends BaseMatrixModuleComputation {
      private final SpikeFilterModule module;
      private int nzColumn;
      private int nzWindow;
      private final RunningMedian[] verticalRunningMedians = new RunningMedian[2 * NX + 1];
      private float[] windowValues = new float[0];
      private final Name spikeCountName;

      private SpikeFilterModuleComputation(SpikeFilterModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);

         this.module = module;
         createVerticalRunningMedians();
         createWindowValues();

         spikeCountName = new Name(module.getPersistentName() + ".spikeCount", module.getDisplayName() + ": Spike count");
         if (module.writePlotParameters.getBooleanValue()) {
            PlotParameterUtils.getOrCreateConfigSubDatagram(pingSource.getPingConfiguration())
                  .addConfig(new PlotParameterConfig(spikeCountName, Unit.NONE, true));
         }
      }

      private RunningMedian getVerticalRunningMedian(int offset) {
         return verticalRunningMedians[offset + NX];
      }

      @Override
      protected void doPing(int startDepth, int endDepth, int channelIndex) {
         PowerData powerData = getPing(0).getPowerData(channelIndex + 1);
         if (powerData == null) {
            return;
         }

         updateSizeParameters(powerData);

         for (RunningMedian verticalRunningMedian : verticalRunningMedians) {
            verticalRunningMedian.clear();
         }

         int begin = startDepth + Math.max(nzColumn, nzWindow);
         int end = endDepth - Math.max(nzColumn, nzWindow);

         for (int j = -NX; j <= NX; j++) {
            RunningMedian verticalRunningMedian = getVerticalRunningMedian(j);
            for (int i = -nzColumn - 1; i < nzColumn; i++) {
               verticalRunningMedian.add(getRawDataFromBuffer(channelIndex, begin + i, j));
            }
         }

         int previousSpikeDep = Integer.MIN_VALUE;
         int spikeCount = 0;
         for (int dep = begin; dep < end; dep++) {
            boolean isSpike = doPixel(dep, channelIndex);
            if (isSpike) {
               if (dep > previousSpikeDep + 2 * nzColumn) {
                  spikeCount++;
               }
               previousSpikeDep = dep;
            }
         }

         if (module.writePlotParameters.getBooleanValue()) {
            PlotParameterUtils.getOrCreateValueSubDatagram(getPing(0))
                  .putPerChannel(spikeCountName, channelIndex + 1, spikeCount);
         }
      }

      private void updateSizeParameters(PowerData powerData) {
         switch (module.verticalUnit.getValue()) {
            case SAMPLES -> {
               setNzColumn(module.verticalMedianSearchHeight.getIntValue());
               setNzWindow(module.windowMedianSearchHeight.getIntValue());
            }
            case DURATION -> {
               float sampleIntervalMilliseconds = powerData.getSampleInterval() * 1000;
               setNzColumn(Math.round(module.verticalMedianSearchDuration.getFloatValue() / sampleIntervalMilliseconds));
               setNzWindow(Math.round(module.windowMedianSearchDuration.getFloatValue() / sampleIntervalMilliseconds));
            }
            case DISTANCE -> {
               float sampleDistance = powerData.getSampleDistance();
               setNzColumn(Math.round(module.verticalMedianSearchDistance.getFloatValue() / sampleDistance));
               setNzWindow(Math.round(module.windowMedianSearchDistance.getFloatValue() / sampleDistance));
            }
         }
      }

      private void setNzColumn(int newNzColumn) {
         if (nzColumn != newNzColumn) {
            nzColumn = newNzColumn;
            createVerticalRunningMedians();
         }
      }

      private void setNzWindow(int newNzWindow) {
         if (nzWindow != newNzWindow) {
            nzWindow = newNzWindow;
            createWindowValues();
         }
      }

      private void createVerticalRunningMedians() {
         for (int i = 0; i < verticalRunningMedians.length; i++) {
            verticalRunningMedians[i] = new RunningMedian(2 * nzColumn + 1);
         }
      }

      private void createWindowValues() {
         windowValues = new float[(2 * NX + 1) * (2 * nzWindow + 1)];
      }

      private boolean doPixel(int dep, int channelIndex) {
         for (int j = -NX; j <= NX; j++) {
            float oldValue = getRawDataFromBuffer(channelIndex, dep - nzColumn - 1, j);
            float newValue = getRawDataFromBuffer(channelIndex, dep + nzColumn, j);
            getVerticalRunningMedian(j).replace(oldValue, newValue);
         }

         if (isCenterPingDifferent()) {
            float median = computeWindowMedian(dep, channelIndex);
            float centerData = getRawDataFromBuffer(channelIndex, dep, 0);
            if (module.isCenterValueDifferent(centerData, median)) {
               // This pixel is accepted as a spike.
               float value = module.debug.getBooleanValue() ? 0 : median;
               setData(channelIndex, dep, value);
               return true;
            }
         }
         return false;
      }

      private float computeWindowMedian(int dep, int channelIndex) {
         float[] windowValues = this.windowValues;

         int ix = 0;
         for (int i = -nzWindow; i <= nzWindow; i++) {
            for (int j = -NX; j <= NX; j++) {
               windowValues[ix++] = getRawDataFromBuffer(channelIndex, dep + i, j);
            }
         }

         return Median.quickSelect(windowValues);
      }

      private boolean isCenterPingDifferent() {
         float centerVerticalMedian = getVerticalRunningMedian(0).getMedian();
         float testValue = module.computeTestValue(centerVerticalMedian);

         int aboveCount = 0;
         int maxAboveCount = 1;
         for (int j = -NX; j <= NX; j++) {
            if (j != 0 && module.spikeTest(testValue, getVerticalRunningMedian(j).getMedian())) {
               aboveCount++;
               if (aboveCount > maxAboveCount) {
                  // Too many other pings above test value => this is not accepted as a spike.
                  return false;
               }
            }
         }
         return true;
      }
   }

   public enum VerticalUnit {
      SAMPLES, DURATION, DISTANCE
   }
}
