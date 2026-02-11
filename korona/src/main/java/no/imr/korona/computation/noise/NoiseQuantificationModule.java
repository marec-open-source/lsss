package no.imr.korona.computation.noise;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.datagrams.Dep0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.incubator.KoronaIncubatorFeatureToggles;
import no.imr.korona.util.ChannelPredicateParameter;
import no.imr.tools.ImmutableUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ObjectParameterValue;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Produces noise quantification datagrams.
 */
public final class NoiseQuantificationModule extends GeneralPingModule {
   private static List<Function<PingConfiguration, @Nullable BiFunction<Ping, Integer, RangeValues>>> allRangeFunctions = List.of();

   public static final String NOISE_XML = "noise.xml";

   public enum SmoothType implements ObjectParameterValue {
      NONE("none"),
      RUNNING_AVERAGER("running averager"),
      AVERAGER("averager");

      private final String id;

      SmoothType(String id) {
         this.id = id;
      }

      @Override
      public String toString() {
         return id;
      }
   }

   public final ObjectParameter<SmoothType> smooth = new ObjectParameter<>(
         new Name("Smooth"),
         SmoothType.RUNNING_AVERAGER, SmoothType.values(),
         "Type of smoothing");

   public final IntParameter smoothInterval = new IntParameter(
         new Name("SmoothInterval", "Smooth interval"),
         5, Unit.COUNT, ValueConstraints.gte(0),
         "Number of samples to smooth vertically before making histogram");

   public enum MaskType implements ObjectParameterValue {
      NONE("none"),
      DYNAMIC("dynamic");

      private final String id;

      MaskType(String id) {
         this.id = id;
      }

      @Override
      public String toString() {
         return id;
      }
   }

   public final ObjectParameter<MaskType> mask = new ObjectParameter<>(
         new Name("Mask"),
         MaskType.DYNAMIC, MaskType.values(),
         "Type of masking");

   public final FloatParameter minimumQuality = new FloatParameter(
         new Name("MinimumQuality", "Minimum quality"),
         60, Unit.DIMENSIONLESS, ValueConstraints.gteLte(0f, 100f),
         "Minimum quality of sample-location used as input to histogram to estimate noise. Quality below: bottom=100, long_range=100,"
               + " range(no_bottom)=90, bottom(2)=75, range(2)=75, between_range_and_bottom=50, range(3)=25, all_data=0.");

   public final BooleanParameter thresholdMasking = new BooleanParameter(
         new Name("ThresholdMasking", "Threshold masking"),
         false,
         "Do not use samples with Sv values, including reflections below bottom, above the given threshold");

   public final FloatParameter threshold = new FloatParameter(
         new Name("Threshold"),
         -80, Unit.DB,
         "The threshold to use for threshold masking");

   public enum HistogramType implements ObjectParameterValue {
      GEOMETRIC("geometric"),
      LINEAR("linear"),
      CONSTANT("constant");

      private final String id;

      HistogramType(String id) {
         this.id = id;
      }

      @Override
      public String toString() {
         return id;
      }
   }

   public final ObjectParameter<HistogramType> histogram = new ObjectParameter<>(
         new Name("Histogram"),
         HistogramType.GEOMETRIC, HistogramType.values(),
         "Type of histogram");

   public final BooleanParameter centerTimeStepBuffer = new BooleanParameter(
         new Name("UseTimeStepBuffer", "Use time step buffer"),
         true,
         "Try to center noise parameters");

   public final IntParameter timeStepBufferMaxSize = new IntParameter(
         new Name("TimeStepBufferMaxSize", "Time step buffer max size"),
         50, Unit.COUNT,
         "Maximum number of pings in buffer");

   public final IntParameter histogramInitializationCellCount = new IntParameter(
         new Name("HistogramInitializationCellCount", "Histogram initialization cell count"),
         BaseHistogram.DEFAULT_INITIALIZATION_CELL_COUNT, Unit.COUNT, ValueConstraints.gte(1),
         "Number of cells in the histograms at initialization");

   public final IntParameter histogramMaximumCellCount = new IntParameter(
         new Name("HistogramMaximumCellCount", "Histogram maximum cell count"),
         BaseHistogram.DEFAULT_MAXIMUM_CELL_COUNT, Unit.COUNT, ValueConstraints.gte(1),
         "Maximum number of cells in the histograms");

   public final IntParameter histogramInitializationSampleCount = new IntParameter(
         new Name("HistogramInitializationSampleCount", "Histogram initialization sample count"),
         BaseHistogram.DEFAULT_INITIALIZATION_SAMPLE_COUNT, Unit.COUNT, ValueConstraints.gte(1),
         "Minimum count of noise samples at initialization of the histograms");

   public final IntParameter histogramMinimumSampleCount = new IntParameter(
         new Name("HistogramMinimumSampleCount", "Histogram minimum sample count"),
         BaseHistogram.DEFAULT_MINIMUM_SAMPLE_COUNT, Unit.COUNT, ValueConstraints.gte(1),
         "Minimum number of noise samples in the histograms");

   public final BooleanParameter histogramSmooth = new BooleanParameter(
         new Name("HistogramSmooth", "Histogram smooth"),
         BaseHistogram.DEFAULT_SMOOTH,
         "Whether or not to smooth the probability distribution");

   public final FloatParameter histogramSmoothFactor = new FloatParameter(
         new Name("HistogramSmoothFactor", "Histogram smooth factor"),
         BaseHistogram.DEFAULT_SMOOTH_FACTOR, Unit.DIMENSIONLESS, ValueConstraints.gt(0f),
         "Diffusion time step = HistogramSmoothFactor × dx^2");

   public final BooleanParameter sdevMasking = new BooleanParameter(
         new Name("SdevMasking", "Sdev masking"),
         false,
         "Use specified number of standard deviations as high noise limit (NH) instead of system decided standard (approx 2 stdev). Minimum is system decided standard.");

   public final FloatParameter sdev = new FloatParameter(
         new Name("Sdev"),
         10, Unit.NONE, ValueConstraints.gt(0f),
         "No stdev right of noise-pdf top (NT) to be used as high noise (NH). NB! Not used to estimate average noise NE.");

   public final BooleanParameter snMasking = new BooleanParameter(
         new Name("SNMasking", "S/N masking"),
         false,
         "Remove all values less than S/N ratio less than estimated noise");

   public final FloatParameter snRatio = new FloatParameter(
         new Name("SNRatio", "S/N ratio"),
         10, Unit.DIMENSIONLESS, ValueConstraints.gt(0f),
         "Minimum ratio of signal to top of noise pdf-histogram, i.e. signal/NT");

   public final BooleanParameter useFallbackNoiseQuantile = new BooleanParameter(
         new Name("UseFallbackNoiseQuantile", "Use fallback noise quantile"),
         false,
         "Use lower quantile of fallback noise on file instead of calculated noise");

   public final FloatParameter fallbackNoiseQuantile = new FloatParameter(
         new Name("FallbackNoiseQuantile", "Fallback noise quantile"),
         5, Unit.PERCENT, ValueConstraints.gteLte(0f, 100f),
         "Lower quantile of fallback noise on file");

   public final ChannelPredicateParameter fallbackNoiseChannels = new ChannelPredicateParameter(
         new Name("FallbackNoiseChannels", "Fallback noise channels"),
         true);

   public final BooleanParameter writePlotParameters = new BooleanParameter(
         new Name("WritePlotParameters", "Write plot parameters"),
         false,
         "Incubating feature: Writes some parameter values for use in echogram plot");

   private int maxIntervalsInNoiseFile = Integer.MAX_VALUE;

   public NoiseQuantificationModule() {
      smooth.addListenerAndNotify(value -> smoothInterval.setEnabled(value != SmoothType.NONE));
      thresholdMasking.addListenerAndNotify(threshold::setEnabled);
      histogramSmooth.addListenerAndNotify(histogramSmoothFactor::setEnabled);
      sdevMasking.addListenerAndNotify(sdev::setEnabled);
      snMasking.addListenerAndNotify(snRatio::setEnabled);
      useFallbackNoiseQuantile.addListenerAndNotify(useFallback -> {
         fallbackNoiseQuantile.setEnabled(useFallback);
         fallbackNoiseChannels.setEnabled(useFallback);
      });

      writePlotParameters.setVisible(KoronaIncubatorFeatureToggles.PLOT_PARAMETERS);
      writePlotParameters.setPersistable(KoronaIncubatorFeatureToggles.PLOT_PARAMETERS);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            smooth,
            smoothInterval,
            mask,
            minimumQuality,
            thresholdMasking,
            threshold,
            histogram,
            centerTimeStepBuffer,
            timeStepBufferMaxSize,
            histogramInitializationCellCount,
            histogramMaximumCellCount,
            histogramInitializationSampleCount,
            histogramMinimumSampleCount,
            histogramSmooth,
            histogramSmoothFactor,
            sdevMasking,
            sdev,
            snMasking,
            snRatio,
            useFallbackNoiseQuantile,
            fallbackNoiseQuantile,
            fallbackNoiseChannels,
            writePlotParameters
      );
   }

   int getMaxIntervalsInNoiseFile() {
      return maxIntervalsInNoiseFile;
   }

   public void setMaxIntervalsInNoiseFile(int maxIntervals) {
      maxIntervalsInNoiseFile = maxIntervals;
   }

   public static void addRangeFunction(Function<PingConfiguration, @Nullable BiFunction<Ping, Integer, RangeValues>> function) {
      allRangeFunctions = ImmutableUtils.add(allRangeFunctions, function);
   }

   static BiFunction<Ping, Integer, RangeValues> rangeFunctionForPingConfiguration(PingConfiguration pingConfiguration) {
      for (Function<PingConfiguration, @Nullable BiFunction<Ping, Integer, RangeValues>> rangeFunction : allRangeFunctions) {
         BiFunction<Ping, Integer, RangeValues> function = rangeFunction.apply(pingConfiguration);
         if (function != null) {
            return function;
         }
      }
      return NoiseQuantificationModule::getDefaultRangeValues;
   }

   private static RangeValues getDefaultRangeValues(Ping ping, int channel) {
      ChannelData channelData = ping.getChannelData(channel);
      Dep0Datagram dep0Datagram = ping.getDep0Datagram();
      float firstBottomRange;
      if (channelData != null && dep0Datagram != null) {
         firstBottomRange = channelData.depthToRange(dep0Datagram.getDepth());
      } else {
         firstBottomRange = 0;
      }
      return new RangeValues() {
         @Override
         public boolean hasBottomRange() {
            return dep0Datagram != null;
         }

         @Override
         public float getAboveFirstBottomRange() {
            return firstBottomRange;
         }

         @Override
         public float getBelowFirstBottomRange() {
            return firstBottomRange;
         }

         @Override
         public float getChannelBottomRange() {
            return hasBottomRange() ? firstBottomRange : channelData != null ? channelData.getMaxRange() : 0;
         }
      };
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new NoiseQuantificationModuleComputation(this, computationContext, pingSource);
   }

   /**
    * Calculates parameters from a histogram when they are requested.
    */
   static final class NQP {
      private final BaseHistogram histogram;
      private final boolean sdevMasking;
      private final float no_sdev;
      private final boolean snMasking;
      private final float snRatio;
      private @Nullable Float nt, probNT;
      private @Nullable Float nh, probNH;   // High noise NH used to calculate average noise NE. NH is usually the high cutoff noise
      private @Nullable Float nhh, probNHH; // High cutoff noise NHH. Unless user specifies otherwise (sdevMasking = true), NHH = NH.
      private @Nullable Float ne, probNE;
      private @Nullable Float stdev_min;

      private NQP(BaseHistogram histogram, boolean sdevMasking, float no_sdev, boolean snMasking, float snRatio) {
         assert histogram.isOK();
         this.histogram = histogram;
         this.sdevMasking = sdevMasking;
         this.no_sdev = no_sdev;
         this.snMasking = snMasking;
         this.snRatio = snRatio;
      }

      static @Nullable NQP create(BaseHistogram histogram, boolean sdevMasking, float no_sdev, boolean snMasking, float snRatio) {
         if (!histogram.isOK()) {
            return null;
         }
         return new NQP(histogram, sdevMasking, no_sdev, snMasking, snRatio);
      }

      float getNT() {
         Float nt = this.nt;
         if (nt == null) {
            nt = histogram.findMaximum();
            this.nt = nt;
         }
         return nt;
      }

      private float getStdevMin() {
         Float stdev_min = this.stdev_min;
         if (stdev_min == null) {
            try {
               // Noise may not be normal distributed. Find alternative measures of cutoff noise.
               // For normal-distributions, one stdev is at: 0.60653... relative to max  (exp(-1/2), 3 sdev is exp(-9/2)).
               float stdev10 = histogram.findInverse(0.60653f * getProbNT(), getNT()) - getNT();  //1 stdev after top (for normal distr.)
               float stdev20 = histogram.findInverse(0.13534f * getProbNT(), getNT()) - getNT();  //2 stdev after top (for normal distr.)
               float stdev30 = histogram.findInverse(0.01111f * getProbNT(), getNT()) - getNT();  //3 stdev after top (for normal distr.)
               float stdev_stdev10 = stdev10;         // 1 stdev based (alt 1)
               float stdev_stdev20 = stdev20 / 2.0f;  // 1 stdev based (alt 2)
               float stdev_stdev30 = stdev30 / 3.0f;  // 1 stdev based (alt 3)
               stdev_min = Math.min(Math.min(stdev_stdev10, stdev_stdev20), stdev_stdev30); // 1 stdev - minimum stdev of calculations
            } catch (HistogramException _) {
               stdev_min = 0f;
            }
            this.stdev_min = stdev_min;
         }
         return stdev_min;
      }

      float getNH() {
         Float nh = this.nh;
         if (nh == null) {
            if (snMasking) {
               nh = getNT() * snRatio;
            } else {
               //try {
               // Ignore that noise may not be normal distributed. ///Find alternative measures of cutoff noise.

               /*
               // For normal-distributions, one stdev is at: 0.60653... relative to max  (exp(-1/2), 3 sdev is exp(-9/2)).
               float stdev10minus = Math.abs(histogram.findInverse(0.60653f * getProbNT(), 0) - getNT()); //1 stdev before top (n. dist.)
               float stdev10 = histogram.findInverse(0.60653f * getProbNT(), getNT()) - getNT();  //1 stdev after top (for normal distr.)

               // Closeness to normal distribution. (Philosophy: better to remove too little than too much). (NB! Expected distribution is Rayleigh)
               // Skewed: w=0;  Close (enough) to normal: w=1; else: 0<w<1.
               double w = Math.clamp((stdev10minus / stdev10 - 0.4) / 0.6, 0, 1.0);
               float x = getStdevMin() * 2.0f * (1f + (float) w);  // x is 2 - 4 standard deviations. (Skewed=2; ----> ; Normal=4;)
               nh = getNT() + x; //Consider using nh = getNT() + getStdevMin() * 3.0f;
               nh = getNT() + getStdevMin() * 1.0f;
                */
               nh = getNT() + getStdevMin() * 3.0f;

               /*
               if (sdevMasking && no_sdev > 0) {
                  nhh = getNT() + getStdevMin() * no_sdev;  // x is no_sdev standard deviations
               }

               } catch (HistogramException _) {
                  nh = 0f;
               }
                */
            }
            nh = Math.clamp(nh, getNE(), getNHH());
            this.nh = nh;
         }
         return nh;
      }

      float getNHH() {
         Float nhh = this.nhh;
         if (nhh == null) {
            nhh = getNT() + getStdevMin() * 6.0f;        // Default
            if (sdevMasking && no_sdev > 0) {            // SDev masking: NHH no_sdev standard deviations above NT
               nhh = getNT() + getStdevMin() * no_sdev;
            }
            /*
            if (nhh == null || nhh < getNH()) {          // Failsafe: NHH >= NH
               nhh = getNH();
            }
             */
            this.nhh = nhh;
         }
         return nhh;
      }

      float getNE() {
         Float ne = this.ne;
         if (ne == null) {
            //ne = histogram.findAverage(getNH());  //Until 2023.12.11 NH was used to calculate NE
            ne = histogram.findAverage(getNHH());   //From 2023.12.12 NHH is used to calculate NE
            this.ne = ne;
         }
         return ne;
      }

      private float getProbNT() {
         Float probNT = this.probNT;
         if (probNT == null) {
            probNT = histogram.findMaximumProbability();
            this.probNT = probNT;
         }
         return probNT;
      }

      float getProbNH() {
         Float probNH = this.probNH;
         if (probNH == null) {
            probNH = histogram.f(getNH());
            this.probNH = probNH;
         }
         return probNH;
      }

      float getProbNHH() {
         Float probNHH = this.probNHH;
         if (probNHH == null) {
            probNHH = histogram.f(getNHH());
            this.probNHH = probNHH;
         }
         return probNHH;
      }

      float getProbNE() {
         Float probNE = this.probNE;
         if (probNE == null) {
            probNE = histogram.f(getNE());
            this.probNE = probNE;
         }
         return probNE;
      }

      @Override
      public String toString() {
         return "sdev: " + (sdevMasking ? no_sdev : false)
               + ", sn: " + (snMasking ? snRatio : false)
               + ", nt: " + getNT() + " (" + getProbNT() + ")"
               + ", ne: " + getNE() + " (" + getProbNE() + ")"
               + ", nh: " + getNH() + " (" + getProbNH() + ")"
               + ", nhh: " + getNHH() + " (" + getProbNHH() + ")";
      }
   }
}
