package no.imr.korona.computation;

import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFilterModule;
import no.imr.korona.computation.broadband.pulsecompressionfilter.PulseCompressionFilterModule;
import no.imr.korona.computation.broadband.splitting.BroadbandSplitterModule;
import no.imr.korona.computation.categorization.CategorizationModule;
import no.imr.korona.computation.categorization.IsolationModule;
import no.imr.korona.computation.categorization.RegionCategorizationModule;
import no.imr.korona.computation.categorization.netcdf.CategorizationNetcdfReaderModule;
import no.imr.korona.computation.categorization.netcdf.CategorizationNetcdfWriterModule;
import no.imr.korona.computation.convolution.SmootherModule;
import no.imr.korona.computation.dataquality.DataQualityModule;
import no.imr.korona.computation.datareduction.AngleDeletionModule;
import no.imr.korona.computation.datareduction.ChannelDataRemovalModule;
import no.imr.korona.computation.datareduction.ChannelRemovalModule;
import no.imr.korona.computation.datareduction.ComplexToRealModule;
import no.imr.korona.computation.datareduction.DataReductionModule;
import no.imr.korona.computation.datareduction.DownsamplingModule;
import no.imr.korona.computation.datareduction.EchoLineCompressionModule;
import no.imr.korona.computation.datareduction.EmptyPingRemovalModule;
import no.imr.korona.computation.datareduction.PingThinningModule;
import no.imr.korona.computation.display.DisplayModule;
import no.imr.korona.computation.expression.ExpressionModule;
import no.imr.korona.computation.filters.BubbleSpikeFilterModule;
import no.imr.korona.computation.filters.DepthDependentResamplingModule;
import no.imr.korona.computation.filters.DilateModule;
import no.imr.korona.computation.filters.ES60CorrectionModule;
import no.imr.korona.computation.filters.EdgeDetectionModule;
import no.imr.korona.computation.filters.ErodeLowValuesModule;
import no.imr.korona.computation.filters.ErodeModule;
import no.imr.korona.computation.filters.FillMissingDataModule;
import no.imr.korona.computation.filters.Filter3X3Module;
import no.imr.korona.computation.filters.MedianModule;
import no.imr.korona.computation.filters.NormalSpikeFilterModule;
import no.imr.korona.computation.filters.PingCollapsingModule;
import no.imr.korona.computation.filters.RemoveBottomModule;
import no.imr.korona.computation.filters.SpotNoiseModule;
import no.imr.korona.computation.filters.ThresholdAllChannelsModule;
import no.imr.korona.computation.filters.ThresholdModule;
import no.imr.korona.computation.io.WriterModule;
import no.imr.korona.computation.misc.CdsViewerModule;
import no.imr.korona.computation.misc.CombinationModule;
import no.imr.korona.computation.misc.CommentModule;
import no.imr.korona.computation.misc.DepthModule;
import no.imr.korona.computation.misc.GroupEndModule;
import no.imr.korona.computation.misc.RescaleModule;
import no.imr.korona.computation.misc.TemporaryComputationsBeginModule;
import no.imr.korona.computation.misc.TemporaryComputationsEndModule;
import no.imr.korona.computation.misc.TimeIntervalModule;
import no.imr.korona.computation.netcdf.NetcdfWriterModule;
import no.imr.korona.computation.noise.NoiseAcceptanceModule;
import no.imr.korona.computation.noise.NoiseMedianQuantificationModule;
import no.imr.korona.computation.noise.NoiseQuantificationModule;
import no.imr.korona.computation.noise.NoiseRemoverModule;
import no.imr.korona.computation.noise.NoiseVisualizationModule;
import no.imr.korona.computation.offset.HorizontalOffsetCorrectionModule;
import no.imr.korona.computation.offset.VerticalOffsetCorrectionModule;
import no.imr.korona.computation.plankton.PlanktonInversionModule;
import no.imr.korona.computation.plugin.PluginModule;
import no.imr.korona.computation.region.SchoolDetectionModule;
import no.imr.korona.computation.towfish.TowfishModule;
import no.imr.korona.computation.tracking.TrackFilterModule;
import no.imr.korona.computation.tracking.TrackingModule;
import no.imr.korona.computation.ts.TsDetectionModule;
import no.imr.korona.plugins.ModulePlugin;
import no.imr.korona.resources.KoronaHelp;
import no.imr.tools.help.HelpSystemHelpSet;
import no.imr.tools.parameter.Name;

import java.util.EnumSet;

public final class KoronaModulePlugin extends ModulePlugin {
   KoronaModulePlugin() {
      super(new Name("Korona", "KORONA"));
   }

   @Override
   public HelpSystemHelpSet getHelpSet() {
      return KoronaHelp.HELP_SET;
   }

   @Override
   public void addModuleInfos(ModuleInfoCollector moduleInfoCollector) {
      moduleInfoCollector.group("Broadband")
            .add(BroadbandSplitterModule.class, new Name("BroadbandSplitterModule", "Broadband splitter"),
                  EnumSet.of(ModuleCategory.MODIFIES_CHANNELS, ModuleCategory.MODIFIES_DATA),
                  "Splits broadband data into multiple single frequency channels")
            .add(BroadbandNotchFilterModule.class, new Name("BroadbandNotchFilterModule", "Broadband notch filter"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Applies notch filter(s) to broadband data")
            .add(PulseCompressionFilterModule.class, new Name("PulseCompressionFilterModule", "Pulse compression filter"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Applies pulse compression filter(s) to broadband data");

      moduleInfoCollector.group("Categorization")
            .add(CategorizationModule.class, new Name("CategorizationModule", "Categorization"),
                  EnumSet.of(ModuleCategory.ADDS_DATAGRAM),
                  "Categorizes each pixel using the configured training data")
            .add(IsolationModule.class, new Name("IsolationModule", "Isolation"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Isolates one category by zeroing out all pixels of different categories")
            .add(RegionCategorizationModule.class, new Name("SchoolCategorizationModule", "School categorization"),
                  EnumSet.of(ModuleCategory.ADDS_DATAGRAM),
                  "Determines whether a school consists of one or more species, and categorizes the whole school<br>" +
                        "if the schools is a single species school");

      moduleInfoCollector.group("Data reduction")
            .add(AngleDeletionModule.class, new Name("AngleDeletionModule", "Angle deletion"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Converts split beam data to single beam data")
            .add(ChannelDataRemovalModule.class, new Name("ChannelDataRemovalModule", "Channel data removal"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Removes data on specified channels")
            .add(ChannelRemovalModule.class, new Name("ChannelRemovalModule", "Channel removal"),
                  EnumSet.of(ModuleCategory.MODIFIES_CHANNELS),
                  "Removes specified channels")
            .add(ComplexToRealModule.class, new Name("ComplexToRealModule", "Complex to real"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Converts complex data to real data. (Many modules require real data as input.)")
            .add(DataReductionModule.class, new Name("DataReductionModule", "Data reduction"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Removes data below configured transducer range")
            .add(DownsamplingModule.class, new Name("DownsamplingModule", "Downsampling"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Downsampling to reduce vertical resolution")
            .add(EchoLineCompressionModule.class, new Name("EchoLineCompressionModule", "Echo line compression"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Keeps only data above a given threshold")
            .add(PingThinningModule.class, new Name("PingThinningModule", "Ping thinning"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Deletes pings")
            .add(EmptyPingRemovalModule.class, new Name("EmptyPingRemovalModule", "Empty ping removal"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Deletes empty pings");

      moduleInfoCollector.group("Display")
            .add(DisplayModule.class, new Name("FiskViewDisplayModule", "Display"),
                  EnumSet.of(ModuleCategory.NO_MODIFICATION),
                  "Displays an echogram for each channel");

      moduleInfoCollector.group("Filters")
            .add(DepthDependentResamplingModule.class, new Name("DepthDependentResamplingModule", "Depth dependent resampling"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Depth dependent resampling")
            .add(DilateModule.class, new Name("DilateModule", "Dilate"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "A dilation filter.<br>" +
                        "Each pixel gets the value of the max value of its neighbors")
            .add(EdgeDetectionModule.class, new Name("EdgeDetectionModule", "Edge detection"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Detects edges by using Sobel horizontal and vertical filter")
            .add(ErodeLowValuesModule.class, new Name("ErodeLowValuesModule", "Erode low values"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Erode values in a spatial surrounding (given by vertical extent) - often used after top thresholding by means of Thresholding module") //RK: see line 189
            .add(ErodeModule.class, new Name("ErodeModule", "Erode"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "An erode filter.<br>" +
                        "Each pixel gets the value of the min value of its neighbors")
            .add(ES60CorrectionModule.class, new Name("ES60CorrectionModule", "ES60 correction"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Removes the ES60 triangle noise")
            .add(FillMissingDataModule.class, new Name("FillMissingDataModule", "Fill missing data"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Adds missing channel data")
            .add(Filter3X3Module.class, new Name("Filter3X3Module", "Filter 3×3"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Applies a 3×3 filter")
            .add(MedianModule.class, new Name("MedianModule", "Median"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Sets the pixel value to median of its 8 neighbors and itself")
            .add(RemoveBottomModule.class, new Name("RemoveBottomModule", "Remove bottom"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Assigns a specified value to samples close to the detected bottom")
            .add(PingCollapsingModule.class, new Name("PingCollapsingModule", "Ping collapsing"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Collapses sequential pinging")
            .add(ThresholdModule.class, new Name("ThresholdModule", "Thresholding"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Masks out values in a specified range on individual channels")
            .add(ThresholdAllChannelsModule.class, new Name("ThresholdAllChannelsModule", "Thresholding all channels"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Masks out values on all channels if some channels are too weak or too strong");

      moduleInfoCollector.group("I/O")
            .add(ModuleInfo.State.BETA, CategorizationNetcdfReaderModule.class, new Name("CategorizationNetcdfReaderModule", "Categorization NetCDF reader (beta)"),
                  EnumSet.of(ModuleCategory.NO_MODIFICATION),
                  "Reads categorization data from a .nc file")
            .add(ModuleInfo.State.BETA, CategorizationNetcdfWriterModule.class, new Name("CategorizationNetcdfWriterModule", "Categorization NetCDF writer (beta)"),
                  EnumSet.of(ModuleCategory.NO_MODIFICATION),
                  "Writes categorization data to a .nc file")
            .add(ModuleInfo.State.BETA, NetcdfWriterModule.class, new Name("NetcdfWriterModule", "NetCDF writer (beta)"),
                  EnumSet.of(ModuleCategory.NO_MODIFICATION),
                  "Writes to a .nc file")
            .add(WriterModule.class, new Name("WriterModule", "Writer"),
                  EnumSet.of(ModuleCategory.NO_MODIFICATION),
                  "Writes to a .raw file");

      moduleInfoCollector.group("Misc")
            .add(CdsViewerModule.class, new Name("CdsViewerModule", "Cds viewer"),
                  EnumSet.of(ModuleCategory.NO_MODIFICATION),
                  "For viewing the module configuration stored in a processed file")
            .add(ModuleInfo.State.BETA, DataQualityModule.class, new Name("DataQualityModule", "Data quality (beta)"),
                  EnumSet.of(ModuleCategory.NO_MODIFICATION),
                  "Computes various data quality indicators and writes the results to a netCDF file")
            .add(RescaleModule.class, new Name("RescaleModule", "Rescale"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Rescales some channels to fit into the logarithmic Sv range")
            .add(TimeIntervalModule.class, new Name("TimeIntervalModule", "Time interval selection"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Processes only the pings in a specified time interval");

      moduleInfoCollector.group("Noise")
            .add(BubbleSpikeFilterModule.class, new Name("BubblSpikeFilterModule", "Bubble spike filter"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Filters bubble spikes")
            .add(NoiseQuantificationModule.class, new Name("NoiseQuantificationModule", "Noise quantification"),
                  EnumSet.of(ModuleCategory.ADDS_DATAGRAM),
                  "Produces noise quantification datagrams")
            .add(NoiseRemoverModule.class, new Name("NoiseRemoverModule", "Noise remover"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Removes noise based on parameters in noise quantification datagrams")
            .add(NoiseVisualizationModule.class, new Name("NoiseVisualizationModule", "Noise visualization"),
                  EnumSet.of(ModuleCategory.NO_MODIFICATION),
                  "Visualizes noise parameters in a separate window")
            .add(NormalSpikeFilterModule.class, new Name("SpikeFilterModule", "Spike filter"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Filters spikes")
            .add(SpotNoiseModule.class, new Name("SpotNoiseModule", "Spot noise filter"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Sets the pixel value to median of its 14 neighbors and itself provided original value is large compared to most neighbours")
            .add(NoiseMedianQuantificationModule.class, new Name("NoiseMedianQuantificationModule", "Median noise quantification"),
                  EnumSet.of(ModuleCategory.ADDS_DATAGRAM),
                  "Produces noise quantification datagrams using the median of selected sample values")
            .add(NoiseAcceptanceModule.class, new Name("NoiseAcceptanceModule", "Median noise acceptance"),
                  EnumSet.of(ModuleCategory.ADDS_DATAGRAM),
                  "Compares detected noise with values recorded in files");

      moduleInfoCollector.group("Offset")
            .add(HorizontalOffsetCorrectionModule.class, new Name("HorizontalOffsetCorrectionModule", "Horizontal offset correction"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Performs horizontal offset correction")
            .add(VerticalOffsetCorrectionModule.class, new Name("VerticalOffsetCorrectionModule", "Vertical offset correction"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Performs vertical offset correction");

      moduleInfoCollector.group("Plankton")
            .add(PlanktonInversionModule.class, new Name("PlanktonInversionModule", "Plankton inversion"),
                  EnumSet.of(ModuleCategory.ADDS_DATAGRAM),
                  "Performs plankton inversion");

      moduleInfoCollector.group("School (region)")
            .add(SchoolDetectionModule.class, new Name("SchoolDetectionModule", "School detection"),
                  EnumSet.of(ModuleCategory.ADDS_DATAGRAM),
                  "Detects schools on a specified channel");

      moduleInfoCollector.group("Smooth (convolution)")
            .add(SmootherModule.class, new Name("SmootherModule", "Smoother"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Performs smoothing by convolution");

      moduleInfoCollector.group("Towfish")
            .add(TowfishModule.class, new Name("TowfishModule", "Towfish merging"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "Merges towfish data with main echosounder data");

      moduleInfoCollector.group("Tracking")
            .add(TrackingModule.class, new Name("TrackingModule", "Tracking"),
                  EnumSet.of(ModuleCategory.ADDS_DATAGRAM),
                  "Tracks single targets")
            .add(TrackFilterModule.class, new Name("TrackFilterModule", "Track filter"),
                  EnumSet.of(ModuleCategory.REMOVES_DATAGRAM),
                  "Filters rejected tracks");

      moduleInfoCollector.group("TS")
            .add(TsDetectionModule.class, new Name("TsDetectionModule", "TS detection"),
                  EnumSet.of(ModuleCategory.ADDS_DATAGRAM),
                  "Detects single targets");

      moduleInfoCollector.top()
            .add(CombinationModule.class, new Name("CombinationModule", "Combination"),
                  EnumSet.of(ModuleCategory.MODIFIES_CHANNELS),
                  "Generates combination echograms")
            .add(CommentModule.class, new Name("CommentModule", "Comment"),
                  EnumSet.of(ModuleCategory.NO_MODIFICATION),
                  "For writing a comment")
            .add(DepthModule.class, new Name("DepthModule", "Bottom detection"),
                  EnumSet.of(ModuleCategory.ADDS_DATAGRAM),
                  "Detects the bottom depth")
            .add(ExpressionModule.class, new Name("ExpressionModule", "Expression"),
                  EnumSet.of(ModuleCategory.MODIFIES_CHANNELS),
                  "Generates combination echograms by a specified expression")
            .add(GroupEndModule.class, new Name("GroupEndModule", "Group end"),
                  EnumSet.of(ModuleCategory.NO_MODIFICATION),
                  "End of a module group")
            .add(ModuleInfo.State.BETA, PluginModule.class, new Name("PluginModule", "Plugin (beta)"),
                  EnumSet.of(ModuleCategory.MODIFIES_DATA),
                  "For writing Java code that implements an API interface for KORONA module computations")
            .add(TemporaryComputationsBeginModule.class, new Name("TemporaryComputationsBeginModule", "Temporary computations begin"),
                  EnumSet.of(ModuleCategory.TEMPORARY_COMPUTATIONS),
                  "Start of temporary computations")
            .add(TemporaryComputationsEndModule.class, new Name("TemporaryComputationsEndModule", "Temporary computations end"),
                  EnumSet.of(ModuleCategory.TEMPORARY_COMPUTATIONS),
                  "End of temporary computations");
   }
}
