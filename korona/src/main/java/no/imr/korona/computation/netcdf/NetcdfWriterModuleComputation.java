package no.imr.korona.computation.netcdf;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.computation.ModuleUtils;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.computation.offset.HorizontalTransducerOffsetsFileService;
import no.imr.korona.computation.offset.TransducerParameterManager;
import no.imr.korona.computation.offset.TransducerParameters;
import no.imr.korona.computation.offset.VerticalTransducerOffsetsFileService;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.ThrowingSupplier;
import no.imr.tools.range.FloatRange;
import no.imr.tools.xml.XmlUtils;
import org.jspecify.annotations.Nullable;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

final class NetcdfWriterModuleComputation extends SimplePingModuleComputation {
   private final NcPingWriter ncPingWriter;

   NetcdfWriterModuleComputation(NetcdfWriterModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
      super(module, computationContext, pingSource);

      Path koronaDirectory = computationContext.getAssociatedKoronaDirectory();
      if (koronaDirectory == null) {
         throw new ModuleConfigurationException(module, "No destination directory configured");
      }
      Path ncDir = koronaDirectory.resolve(module.dirName.getValue());
      FileUtils.createDirectories(ncDir);
      String ncFileName = FileUtils.baseName(computationContext.getPingReader().getFile()) + ".nc";
      Path ncFile = ncDir.resolve(ncFileName);

      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      Integer referenceKHz = module.mainFrequency.getValue().orElse(null);
      int referenceChannel = referenceKHz != null
            ? pingConfiguration.getRawFileConfiguration().lastChannelWithKHz(referenceKHz)
            : 1;
      if (referenceChannel <= 0) {
         throw new ModuleConfigurationException(module, "Cannot find channel with " + referenceKHz + " kHz");
      }

      NcOptionalConfig optionalConfig = new NcOptionalConfig();
      Path horizontalOffsetsFile = module.getOptionalConfigFile(HorizontalTransducerOffsetsFileService.NAME);
      optionalConfig.horizontalTransducerParameterManager = horizontalOffsetsFile != null
            ? new TransducerParameterManager(TransducerParameters.ParameterType.HORIZONTAL, XmlUtils.readDocument(horizontalOffsetsFile))
            : null;
      Path verticalOffsetsFile = module.getOptionalConfigFile(VerticalTransducerOffsetsFileService.NAME);
      optionalConfig.verticalTransducerParameterManager = verticalOffsetsFile != null
            ? new TransducerParameterManager(TransducerParameters.ParameterType.VERTICAL, XmlUtils.readDocument(verticalOffsetsFile))
            : null;

      ThrowingSupplier<ChannelData, IOException> referenceChannelData = new ThrowingSupplier<>() {
         private @Nullable ChannelData channelData;

         @Override
         public ChannelData get() throws IOException {
            ChannelData channelData = this.channelData;
            if (channelData == null) {
               channelData = ModuleUtils.getInputChannelData(NetcdfWriterModuleComputation.this, referenceChannel);
               if (channelData == null) {
                  throw new ModuleConfigurationException(module, "Cannot find data on channel " + referenceChannel + " with " + referenceKHz + " kHz");
               }
               this.channelData = channelData;
            }
            return channelData;
         }
      };
      ThrowingSupplier<Float, IOException> deltaRange = () -> {
         Optional<Float> optDeltaRange = module.deltaRange.getValue();
         return optDeltaRange.isPresent() ? optDeltaRange.get() : referenceChannelData.get().getSampleDistance();
      };
      ThrowingSupplier<Float, IOException> maxRange = () -> {
         Optional<Float> optMaxRange = module.maxRange.getValue();
         return optMaxRange.isPresent() ? optMaxRange.get() : referenceChannelData.get().getMaxRange();
      };

      try {
         ncPingWriter = switch (module.writerType.getValue()) {
            case GRIDDED -> {
               GridOutput gridOutput = switch (module.griddedOutputType.getValue()) {
                  case EMPTY -> new GridEmptyOutput();
                  case SV_AND_ANGLES -> new GridSvAndAnglesOutput(module.writeAngles.getBooleanValue());
                  case PULSE_COMPRESSION -> new GridPulseCompressionOutput();
                  case BROADBAND_SV -> new GridBroadbandSvOutput(module.fftWindowSize.getFloatValue(),
                        totalBroadbandFrequencyRange(), module.deltaFrequency.getFloatValue() * 1000);
               };
               yield new NcGridWriter(ncFile, pingConfiguration, referenceChannel, deltaRange.get(), maxRange.get(), gridOutput, optionalConfig);
            }
            case CHANNEL_GROUPS -> {
               ChannelGroupOutput channelGroupOutput = switch (module.channelGroupOutputType.getValue()) {
                  case EMPTY -> new ChannelGroupEmptyOutput();
                  case PULSE_COMPRESSION -> new ChannelGroupPulseCompressionOutput(
                        module.maxRange.getValue().orElse(null),
                        module.writeAngles.getBooleanValue());
                  case BROADBAND_SV -> new ChannelGroupBroadbandSvOutput(
                        module.deltaRange.getValue().orElse(null),
                        module.maxRange.getValue().orElse(null),
                        module.fftWindowSize.getFloatValue(),
                        module.deltaFrequency.getFloatValue() * 1000,
                        module.writeAngles.getBooleanValue());
               };
               Map<Integer, ChannelData> channelToChannelData = ModuleUtils.getInputChannelToChannelData(this);
               yield new NcChannelGroupWriter(ncFile, pingConfiguration, channelToChannelData, referenceChannel, channelGroupOutput, optionalConfig);
            }
         };
      } catch (InvalidRangeException e) {
         throw new IOException(e);
      }
   }

   private FloatRange totalBroadbandFrequencyRange() throws IOException {
      return Utils.getAllOfType(ModuleUtils.getInputChannelToChannelData(this).values(), BroadbandData.class)
            .map(BroadbandData::getFrequencyRange)
            .reduce(FloatRange.EMPTY_RANGE, FloatRange::union);
   }

   @Override
   protected void processPing(Ping ping) throws IOException {
      try {
         ncPingWriter.writePing(ping);
      } catch (InvalidRangeException e) {
         throw new IOException(e);
      }
   }

   @Override
   public void close() throws IOException {
      ncPingWriter.close();
   }
}
