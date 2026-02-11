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
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.range.FloatRange;
import no.imr.tools.xml.XmlUtils;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

final class NetcdfWriterModuleComputation extends SimplePingModuleComputation {
   private final NcGridWriter ncGridWriter;

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
      int referenceChannel = ModuleUtils.getMainChannelOrThrow(this, module.mainFrequency.getValue());

      NcOptionalConfig optionalConfig = new NcOptionalConfig();
      Path horizontalOffsetsFile = module.getOptionalConfigFile(HorizontalTransducerOffsetsFileService.NAME);
      optionalConfig.horizontalTransducerParameterManager = horizontalOffsetsFile != null
            ? new TransducerParameterManager(TransducerParameters.ParameterType.HORIZONTAL, XmlUtils.readDocument(horizontalOffsetsFile))
            : null;
      Path verticalOffsetsFile = module.getOptionalConfigFile(VerticalTransducerOffsetsFileService.NAME);
      optionalConfig.verticalTransducerParameterManager = verticalOffsetsFile != null
            ? new TransducerParameterManager(TransducerParameters.ParameterType.VERTICAL, XmlUtils.readDocument(verticalOffsetsFile))
            : null;

      try {
         LogSvCompressor logSvCompressor = module.compressSv.getBooleanValue()
               ? new LogSvCompressor(module.compressedLogSvRange.getValue(), module.compressedLogSvDelta.getFloatValue())
               : null;
         ncGridWriter = switch (module.writerType.getValue()) {
            case GRIDDED -> {
               CommonGridOutput commonGridOutput = switch (module.griddedOutputType.getValue()) {
                  case EMPTY -> new CommonGridEmptyOutput();
                  case SV_AND_ANGLES -> new CommonGridSvAndAnglesOutput(module.writeAngles.getBooleanValue(), logSvCompressor);
                  case PULSE_COMPRESSION -> new CommonGridPulseCompressionOutput();
                  case BROADBAND_SV -> new CommonGridBroadbandSvOutput(module.fftWindowSize.getFloatValue(),
                        totalBroadbandFrequencyRange(), module.deltaFrequency.getFloatValue() * 1000);
               };

               Optional<Float> optDeltaRange = module.deltaRange.getValue();
               float deltaRange = optDeltaRange.isPresent()
                     ? optDeltaRange.get()
                     : ModuleUtils.getInputChannelDataOrThrow(this, referenceChannel).getSampleDistance();

               Optional<Float> optMaxRange = module.maxRange.getValue();
               float maxRange = optMaxRange.isPresent()
                     ? optMaxRange.get()
                     : ModuleUtils.getInputChannelDataOrThrow(this, referenceChannel).getMaxRange();

               CommonGridConfig commonGridConfig = new CommonGridConfig(commonGridOutput, deltaRange, maxRange);
               yield new NcGridWriter(ncFile, pingConfiguration, referenceChannel, commonGridConfig, null, optionalConfig);
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
               ChannelGroupConfig channelGroupConfig = new ChannelGroupConfig(channelGroupOutput, ModuleUtils.getInputChannelToChannelData(this));
               yield new NcGridWriter(ncFile, pingConfiguration, referenceChannel, null, channelGroupConfig, optionalConfig);
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
         ncGridWriter.writePing(ping);
      } catch (InvalidRangeException e) {
         throw new IOException(e);
      }
   }

   @Override
   public void close() throws IOException {
      ncGridWriter.close();
   }
}
