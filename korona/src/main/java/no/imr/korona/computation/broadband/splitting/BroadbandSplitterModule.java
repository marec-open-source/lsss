package no.imr.korona.computation.broadband.splitting;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFiltersFileService;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class BroadbandSplitterModule extends ConcurrentPingModule {
   private final HeaderParameter splitBandsHeader = new HeaderParameter("Split bands");

   public final BooleanParameter autoBands = new BooleanParameter(
         new Name("AutoSplit", "Auto split"),
         false,
         "Split into equally large frequency bands");

   public final IntParameter splitCount = new IntParameter(
         new Name("SplitCount", "Split count"),
         3, Unit.COUNT, ValueConstraints.gte(1),
         "Number of bands per broadband channel");

   public final OptionalFloatParameter splitBandwidth = new OptionalFloatParameter(
         new Name("SplitBandwidth", "Split bandwidth"),
         Optional.empty(), Unit.KHZ, ValueConstraints.gt(0f),
         "Bandwidth of the auto-split bands. The default is channel bandwidth / split count");

   public final FloatParameter stopBandDistance = new FloatParameter(
         new Name("StopBandDistance", "Stopband distance"),
         1, Unit.KHZ,
         "Distance between pass- and stopband");

   public final FloatParameter minBandwidth = new FloatParameter(
         new Name("MinBandwidth", "Min bandwidth"),
         10, Unit.KHZ,
         "Ignore bands smaller than minimal bandwidth");

   public enum DownsamplingMethod {
      NONE, FACTOR, SAMPLE_SIZE
   }

   private final HeaderParameter dataReductionHeader = new HeaderParameter("Data reduction");

   public final ObjectParameter<DownsamplingMethod> downsamplingMethod = new ObjectParameter<>(
         new Name("Downsampling"),
         DownsamplingMethod.FACTOR, DownsamplingMethod.values(),
         "Downsampling method");

   public final IntParameter downsamplingFactor = new IntParameter(
         new Name("DownsamplingFactor", "Downsampling factor"),
         1, Unit.DIMENSIONLESS, ValueConstraints.gte(1),
         "Downsampling factor");

   public final FloatParameter downsamplingSampleSize = new FloatParameter(
         new Name("DownsamplingSampleSize", "Downsampling sample size"),
         0.01f, Unit.METER, ValueConstraints.gt(0f),
         "Downsampling factor is set to give approximately this sample size");

   public final BooleanParameter computeAngles = new BooleanParameter(
         new Name("ComputeAngles", "Compute angles"),
         true);

   private final HeaderParameter computationalMethodHeader = new HeaderParameter("Computational method");

   public enum ComputationalMethod {
      BANDPASS_FILTERING, GLIDING_FFT_WINDOW
   }

   public final ObjectParameter<ComputationalMethod> computationalMethod = new ObjectParameter<>(
         new Name("ComputationalMethod", "Computational method"),
         ComputationalMethod.BANDPASS_FILTERING, ComputationalMethod.values(),
         "Method for computing sv for each split band");

   public final FloatParameter fftWindowSize = new FloatParameter(
         new Name("FftWindowSize", "FFT window size"),
         2, Unit.NONE, ValueConstraints.gt(0f),
         "Size of the FFT window in units of pulse length");

   public BroadbandSplitterModule() {
      autoBands.addListenerAndNotify(auto -> {
         splitCount.setEnabled(auto);
         splitBandwidth.setEnabled(auto);
      });
      downsamplingMethod.addListenerAndNotify(method -> {
         downsamplingFactor.setVisible(method == DownsamplingMethod.FACTOR);
         downsamplingSampleSize.setVisible(method == DownsamplingMethod.SAMPLE_SIZE);
      });
      computationalMethod.addListenerAndNotify(method -> {
         fftWindowSize.setVisible(method == ComputationalMethod.GLIDING_FFT_WINDOW);
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            splitBandsHeader,
            autoBands,
            splitCount,
            splitBandwidth,
            stopBandDistance,
            minBandwidth,
            //---
            dataReductionHeader,
            downsamplingMethod,
            downsamplingFactor,
            downsamplingSampleSize,
            computeAngles,
            //---
            computationalMethodHeader,
            computationalMethod,
            fftWindowSize
      );
   }

   @Override
   public List<Name> getRequiredConfigFileServiceNames() {
      return List.of(BroadbandSplitterBandsFileService.NAME);
   }

   @Override
   public List<Name> getOptionalConfigFileServiceNames() {
      return List.of(BroadbandNotchFiltersFileService.NAME);
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new BroadbandSplitterModuleComputation(this, computationContext, pingSource);
   }
}
