package no.imr.korona.computation.netcdf;

import com.google.common.html.HtmlEscapers;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.SimplePingModule;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.computation.offset.HorizontalTransducerOffsetsFileService;
import no.imr.korona.computation.offset.VerticalTransducerOffsetsFileService;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.listening.Listener;
import no.imr.tools.netcdf.NetcdfUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.IntCsvListParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.RangeParameter;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ObjectParameterValue;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class NetcdfWriterModule extends SimplePingModule {
   public final StringParameter dirName = new StringParameter(
         new Name("DirName", "Output directory name"),
         "netcdf",
         "The netCDF files are written to this subfolder in the destination directory");

   public final IntCsvListParameter mainFrequency = new IntCsvListParameter(
         new Name("MainFrequency", "Main frequency"),
         List.of(38), Unit.KHZ, ValueConstraints.gt(0),
         """
               A comma-separated list of prioritized candidates for the main frequency.
               If unspecified, the first channel is used.
               The channel with the main frequency is placed first in the output file""");

   public final ObjectParameter<WriterType> writerType = new ObjectParameter<>(
         new Name("WriterType", "Writer type"),
         WriterType.GRIDDED, WriterType.values(),
         "How to structure the output data");

   public final ObjectParameter<GriddedOutputType> griddedOutputType = new ObjectParameter<>(
         new Name("GriddedOutputType", "Gridded output type"),
         GriddedOutputType.SV_AND_ANGLES, GriddedOutputType.values(),
         "Output data in addition to the basic information");

   public final ObjectParameter<ChannelGroupOutputType> channelGroupOutputType = new ObjectParameter<>(
         new Name("ChannelGroupOutputType", "Channel group output type"),
         ChannelGroupOutputType.PULSE_COMPRESSION, ChannelGroupOutputType.values(),
         "Output data in addition to the basic information");

   public final OptionalFloatParameter deltaRange = new OptionalFloatParameter(
         new Name("DeltaRange", "Delta range"),
         Optional.empty(), Unit.METER, ValueConstraints.gt(0f),
         "Leave blank to determine automatically");

   public final OptionalFloatParameter maxRange = new OptionalFloatParameter(
         new Name("MaxRange", "Max range"),
         Optional.empty(), Unit.METER, ValueConstraints.gt(0f),
         "Leave blank to determine automatically");

   public final FloatParameter fftWindowSize = new FloatParameter(
         new Name("FftWindowSize", "FFT window size"),
         2, Unit.NONE, ValueConstraints.gt(0f),
         "Size of the FFT window in units of pulse length");

   public final FloatParameter deltaFrequency = new FloatParameter(
         new Name("DeltaFrequency", "Delta frequency"),
         1, Unit.KHZ, ValueConstraints.gt(0f),
         "Resolution of the broadband frequency array");

   public final BooleanParameter writeAngles = new BooleanParameter(
         new Name("WriteAngels", "Write angles"),
         true,
         "If selected, then the output data will include arrays for alongship and athwartship angles");

   public final BooleanParameter compressSv = new BooleanParameter(
         new Name("CompressSv", "Compress sv"),
         false,
         "Write a compressed log sv variable with reduced resolution, instead of sv");

   public final RangeParameter compressedLogSvRange = new RangeParameter(
         new Name("CompressedLogSvRange", "Compressed log sv range"),
         -82, -20, Unit.DB,
         "The value range for the compressed log sv variable");

   public final FloatParameter compressedLogSvDelta = new FloatParameter(
         new Name("CompressedLogSvDelta", "Compressed log sv delta"),
         0.1f, Unit.DB, ValueConstraints.gt(0f),
         "The value resolution for the compressed log sv variable");

   private final HeaderParameter netcdfInfoHeader = new HeaderParameter("NetCDF info");

   public NetcdfWriterModule() {
      Listener.of(this::updateParameters).addToAndNotify(
            writerType,
            griddedOutputType,
            channelGroupOutputType
      );
      compressSv.addListenerAndNotify(compress -> {
         compressedLogSvRange.setEnabled(compress);
         compressedLogSvDelta.setEnabled(compress);
      });

      NetcdfUtils.logNetcdfCLibraryVersion();
      String netcdfInfoText = NetcdfClibrary.isLibraryPresent()
            ? "NetCDF-C library version " + NetcdfClibrary.getVersion()
            : "NetCDF-C library not found. Please install it first.";
      netcdfInfoHeader.setHtmlContent("<p>" + HtmlEscapers.htmlEscaper().escape(netcdfInfoText) + "</p>");
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            dirName,
            mainFrequency,
            writerType,
            griddedOutputType,
            channelGroupOutputType,
            deltaRange,
            maxRange,
            fftWindowSize,
            deltaFrequency,
            writeAngles,
            compressSv,
            compressedLogSvRange,
            compressedLogSvDelta,
            netcdfInfoHeader
      );
   }

   private void updateParameters() {
      WriterType writer = writerType.getValue();

      GriddedOutputType griddedType = writer == WriterType.GRIDDED ? griddedOutputType.getValue() : null;
      ChannelGroupOutputType channelGroupType = writer == WriterType.CHANNEL_GROUPS ? channelGroupOutputType.getValue() : null;

      griddedOutputType.setVisible(writer == WriterType.GRIDDED);
      channelGroupOutputType.setVisible(writer == WriterType.CHANNEL_GROUPS);

      deltaRange.setVisible(writer == WriterType.GRIDDED
            || channelGroupType == ChannelGroupOutputType.BROADBAND_SV);

      maxRange.setVisible(channelGroupType != ChannelGroupOutputType.EMPTY);

      fftWindowSize.setVisible(griddedType == GriddedOutputType.BROADBAND_SV
            || channelGroupType == ChannelGroupOutputType.BROADBAND_SV);

      deltaFrequency.setVisible(griddedType == GriddedOutputType.BROADBAND_SV
            || channelGroupType == ChannelGroupOutputType.BROADBAND_SV);

      writeAngles.setVisible(griddedType == GriddedOutputType.SV_AND_ANGLES
            || channelGroupType == ChannelGroupOutputType.BROADBAND_SV
            || channelGroupType == ChannelGroupOutputType.PULSE_COMPRESSION);

      compressSv.setVisible(griddedType == GriddedOutputType.SV_AND_ANGLES);
      compressedLogSvRange.setVisible(griddedType == GriddedOutputType.SV_AND_ANGLES);
      compressedLogSvDelta.setVisible(griddedType == GriddedOutputType.SV_AND_ANGLES);
   }

   @Override
   public List<Name> getOptionalConfigFileServiceNames() {
      return List.of(
            HorizontalTransducerOffsetsFileService.NAME,
            VerticalTransducerOffsetsFileService.NAME
      );
   }

   @Override
   public @Nullable SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      if (computationContext.isUsingKoronaPlaybox()) {
         return null;
      }
      return new NetcdfWriterModuleComputation(this, computationContext, pingSource);
   }

   public enum WriterType implements ObjectParameterValue {
      GRIDDED("Gridded", "Output for all channels are placed i a common grid"),
      CHANNEL_GROUPS("Channel groups", "Output for each channel in its own grid in a separate group");

      private final String label;
      private final String tooltip;

      WriterType(String label, String tooltip) {
         this.label = label;
         this.tooltip = tooltip;
      }

      @Override
      public String getDisplayLabel() {
         return label;
      }

      @Override
      public String getTooltip() {
         return tooltip;
      }
   }

   public enum GriddedOutputType implements ObjectParameterValue {
      EMPTY("Empty", "No data in addition to the basic information"),
      SV_AND_ANGLES("Sv and angles", "Sv and optionally angles"),
      PULSE_COMPRESSION("Pulse compression", "Average pulse compressed values"),
      BROADBAND_SV("Broadband Sv(f)", "Sv as function of frequency");

      private final String label;
      private final String tooltip;

      GriddedOutputType(String label, String tooltip) {
         this.label = label;
         this.tooltip = tooltip;
      }

      @Override
      public String getDisplayLabel() {
         return label;
      }

      @Override
      public String getTooltip() {
         return tooltip;
      }
   }

   public enum ChannelGroupOutputType implements ObjectParameterValue {
      EMPTY("Empty", "No data in addition to the basic information"),
      PULSE_COMPRESSION("Pulse compression", "Pulse compressed values for each sector"),
      BROADBAND_SV("Broadband Sv(f)", "Sv as function of frequency");

      private final String label;
      private final String tooltip;

      ChannelGroupOutputType(String label, String tooltip) {
         this.label = label;
         this.tooltip = tooltip;
      }

      @Override
      public String getDisplayLabel() {
         return label;
      }

      @Override
      public String getTooltip() {
         return tooltip;
      }
   }
}
