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
import no.imr.tools.parameter.Name;
import no.imr.tools.range.FloatRange;
import no.imr.tools.xml.XmlUtils;
import org.jspecify.annotations.Nullable;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

      NcConfig ncConfig = new NcConfig(pingConfiguration, referenceChannel);

      List<PingByPingOutput> pingByPingOutputs = new ArrayList<>();
      pingByPingOutputs.add(new BasicInfoOutput(
            transducerParameterManager(TransducerParameters.ParameterType.HORIZONTAL, HorizontalTransducerOffsetsFileService.NAME),
            transducerParameterManager(TransducerParameters.ParameterType.VERTICAL, VerticalTransducerOffsetsFileService.NAME)
      ));

      try {
         switch (module.writerType.getValue()) {
            case GRIDDED -> {
               List<CommonGridOutput> commonGridOutputs = new ArrayList<>();
               switch (module.griddedOutputType.getValue()) {
                  case EMPTY -> {
                  }
                  case SV_AND_ANGLES -> {
                     LogSvCompressor logSvCompressor = module.compressSv.getBooleanValue()
                           ? new LogSvCompressor(module.compressedLogSvRange.getValue(), module.compressedLogSvDelta.getFloatValue())
                           : null;
                     commonGridOutputs.add(new CommonGridSvOutput(logSvCompressor));
                     if (module.writeAngles.getBooleanValue()) {
                        commonGridOutputs.add(new CommonGridAnglesOutput());
                     }
                  }
                  case PULSE_COMPRESSION -> commonGridOutputs.add(new CommonGridPulseCompressionOutput());
                  case BROADBAND_SV -> commonGridOutputs.add(new CommonGridBroadbandSvOutput(module.fftWindowSize.getFloatValue(),
                        totalBroadbandFrequencyRange(), module.deltaFrequency.getFloatValue() * 1000));
               }

               Optional<Float> optDeltaRange = module.deltaRange.getValue();
               float deltaRange = optDeltaRange.isPresent()
                     ? optDeltaRange.get()
                     : ModuleUtils.getInputChannelDataOrThrow(this, referenceChannel).getSampleDistance();

               Optional<Float> optMaxRange = module.maxRange.getValue();
               float maxRange = optMaxRange.isPresent()
                     ? optMaxRange.get()
                     : ModuleUtils.getInputChannelDataOrThrow(this, referenceChannel).getMaxRange();

               pingByPingOutputs.add(new CommonGridWriter(commonGridOutputs, deltaRange, maxRange));
            }
            case CHANNEL_GROUPS -> {
               List<ChannelGroupOutput> channelGroupOutputs = new ArrayList<>();
               switch (module.channelGroupOutputType.getValue()) {
                  case EMPTY -> {
                  }
                  case PULSE_COMPRESSION -> channelGroupOutputs.add(new ChannelGroupPulseCompressionOutput(
                        module.maxRange.getValue().orElse(null),
                        module.writeAngles.getBooleanValue()));
                  case BROADBAND_SV -> channelGroupOutputs.add(new ChannelGroupBroadbandSvOutput(
                        module.deltaRange.getValue().orElse(null),
                        module.maxRange.getValue().orElse(null),
                        module.fftWindowSize.getFloatValue(),
                        module.deltaFrequency.getFloatValue() * 1000,
                        module.writeAngles.getBooleanValue()));
               }
               Map<Integer, ChannelData> channelToChannelData = ModuleUtils.getInputChannelToChannelData(this);
               pingByPingOutputs.add(new EnvironmentGroupOutput());
               pingByPingOutputs.add(new ChannelGroupWriter(channelGroupOutputs, channelToChannelData));
            }
         }
         ncGridWriter = new NcGridWriter(ncFile, pingByPingOutputs, ncConfig);
      } catch (InvalidRangeException e) {
         throw new IOException(e);
      }
   }

   private @Nullable TransducerParameterManager transducerParameterManager(TransducerParameters.ParameterType type, Name configFileName) throws IOException {
      Path file = getModule().getOptionalConfigFile(configFileName);
      return file != null ? new TransducerParameterManager(type, XmlUtils.readDocument(file)) : null;
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
