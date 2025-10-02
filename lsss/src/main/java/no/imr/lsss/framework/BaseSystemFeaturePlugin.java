package no.imr.lsss.framework;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.LsssDatabaseContent;
import no.imr.lsss.framework.config.application.SubDir;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.framework.export.Exporter;
import no.imr.lsss.modules.ModuleCollection;
import no.imr.lsss.modules.OnStartup;
import no.imr.lsss.modules.Where;
import no.imr.lsss.modules.broadband.calibrationplot.BroadbandCalibrationPlotModule;
import no.imr.lsss.modules.broadband.sv.BroadbandBottomDataExporter;
import no.imr.lsss.modules.broadband.sv.BroadbandSampleDataExporter;
import no.imr.lsss.modules.broadband.sv.BroadbandSvByDepthModule;
import no.imr.lsss.modules.broadband.sv.BroadbandSvExporter;
import no.imr.lsss.modules.broadband.sv.BroadbandSvModule;
import no.imr.lsss.modules.broadband.ts.BroadbandTrackExporter;
import no.imr.lsss.modules.broadband.ts.BroadbandTsEchogramOverlay;
import no.imr.lsss.modules.broadband.ts.BroadbandTsExporter;
import no.imr.lsss.modules.broadband.ts.BroadbandTsHistogramModule;
import no.imr.lsss.modules.broadband.ts.BroadbandTsModule;
import no.imr.lsss.modules.comment.CommentDataModule;
import no.imr.lsss.modules.comment.CommentEchogramOverlay;
import no.imr.lsss.modules.comment.CommentMapOverlay;
import no.imr.lsss.modules.comment.CommentModule;
import no.imr.lsss.modules.ctd.CTDDataModule;
import no.imr.lsss.modules.ctd.CTDMapOverlay;
import no.imr.lsss.modules.ctd.CTDViewModule;
import no.imr.lsss.modules.echogram.BottomEchogramModule;
import no.imr.lsss.modules.echogram.ColorBarModule;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.PelagicEchogramExporter;
import no.imr.lsss.modules.echogram.PelagicEchogramModule;
import no.imr.lsss.modules.echogram.ScrollBarModule;
import no.imr.lsss.modules.echogram.overlays.AccumulatedSaOverlay;
import no.imr.lsss.modules.echogram.overlays.BottomOverlay;
import no.imr.lsss.modules.echogram.overlays.DepthMarkerOverlay;
import no.imr.lsss.modules.echogram.overlays.EchogramImageOverlay;
import no.imr.lsss.modules.echogram.overlays.FileMarkerOverlay;
import no.imr.lsss.modules.echogram.overlays.GridOverlay;
import no.imr.lsss.modules.echogram.overlays.MapPositionOverlay;
import no.imr.lsss.modules.echogram.overlays.MaskingDisplayOverlay;
import no.imr.lsss.modules.echogram.overlays.MaskingEditOverlay;
import no.imr.lsss.modules.echogram.overlays.PingMarkerOverlay;
import no.imr.lsss.modules.echogram.overlays.RegionAddOverlay;
import no.imr.lsss.modules.echogram.overlays.RegionDisplayOverlay;
import no.imr.lsss.modules.echogram.overlays.RegionEditOverlay;
import no.imr.lsss.modules.echogram.overlays.SaOverlay;
import no.imr.lsss.modules.echogram.overlays.TicksOverlay;
import no.imr.lsss.modules.echogram.overlays.VerticalLineOverlay;
import no.imr.lsss.modules.echogram.overlays.ZoomOverlay;
import no.imr.lsss.modules.echogramplot.EchogramPlotExporter;
import no.imr.lsss.modules.echogramplot.EchogramPlotModule;
import no.imr.lsss.modules.filedraw.FileDrawDataModule;
import no.imr.lsss.modules.filedraw.FileDrawOverlay;
import no.imr.lsss.modules.frequencyresponse.FrequencyResponseModule;
import no.imr.lsss.modules.integration.RegionIntegrationModule;
import no.imr.lsss.modules.interpretation.InterpretationModule;
import no.imr.lsss.modules.korona.experimentation.KoronaExperimentationModule;
import no.imr.lsss.modules.korona.region.KoronaRegionEchogramOverlay;
import no.imr.lsss.modules.korona.region.KoronaRegionModule;
import no.imr.lsss.modules.korona.region.SchoolCategoryOverlay;
import no.imr.lsss.modules.korona.tracking.TrackCategoryEchogramOverlay;
import no.imr.lsss.modules.korona.tracking.TrackEchogramOverlay;
import no.imr.lsss.modules.korona.tracking.TrackExporter;
import no.imr.lsss.modules.korona.tracking.TrackInfoModule;
import no.imr.lsss.modules.map.MapModule;
import no.imr.lsss.modules.map.overlays.BackgroundMapOverlay;
import no.imr.lsss.modules.map.overlays.DragOverlay;
import no.imr.lsss.modules.map.overlays.EchogramPositionOverlay;
import no.imr.lsss.modules.map.overlays.FileMarkerMapOverlay;
import no.imr.lsss.modules.map.overlays.LatLonOverlay;
import no.imr.lsss.modules.map.overlays.PanOverlay;
import no.imr.lsss.modules.map.overlays.SaMapOverlay;
import no.imr.lsss.modules.map.overlays.ScaleMapOverlay;
import no.imr.lsss.modules.map.overlays.SelectionMapOverlay;
import no.imr.lsss.modules.map.overlays.SurveyLineOverlay;
import no.imr.lsss.modules.map.overlays.ZoomMapOverlay;
import no.imr.lsss.modules.masking.ConditionalMaskingExtractionModule;
import no.imr.lsss.modules.misc.MagnifierModule;
import no.imr.lsss.modules.misc.NumericalViewModule;
import no.imr.lsss.modules.misc.PingPlotModule;
import no.imr.lsss.modules.misc.SystemInfoModule;
import no.imr.lsss.modules.plankton.PlanktonModule;
import no.imr.lsss.modules.reflog.RefLogDataModule;
import no.imr.lsss.modules.reflog.RefLogEchogramOverlay;
import no.imr.lsss.modules.reflog.RefLogMapOverlay;
import no.imr.lsss.modules.scatterplot.ScatterPlotModule;
import no.imr.lsss.modules.schoolparameter.SchoolParameterExport;
import no.imr.lsss.modules.schoolparameter.SchoolParameterModule;
import no.imr.lsss.modules.sv.SvDistributionModule;
import no.imr.lsss.modules.sv.SvExporter;
import no.imr.lsss.modules.thresholdresponse.ThresholdResponseModule;
import no.imr.lsss.modules.trawl.TrawlModule;
import no.imr.lsss.modules.ts.TSEchogramOverlay;
import no.imr.lsss.modules.ts.TSExporter;
import no.imr.lsss.modules.ts.TSModule;
import no.imr.lsss.modules.ts.TsPositionsModule;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.database.DatabaseContent;
import no.imr.tools.help.HelpSystemHelpSet;
import no.imr.tools.parameter.Name;

import java.nio.file.Path;
import java.util.List;

/**
 * The LSSS base system feature plugin.
 */
public class BaseSystemFeaturePlugin extends FeaturePlugin {
   private final LsssDatabaseContent databaseContent = new LsssDatabaseContent();
   private final BaseSystemWorkFileManager workFileManager;

   protected BaseSystemFeaturePlugin(BaseSystemFeatureService service, LSSS lsss) {
      super(service, lsss);

      workFileManager = new BaseSystemWorkFileManager(lsss);
   }

   @Override
   public void setup() {
      workFileManager.setup();
   }

   @Override
   public HelpSystemHelpSet getHelpSet() {
      return LsssHelp.HELP_SET;
   }

   @Override
   public ModuleCollection<?> getModules() {
      ModuleCollection<BaseSystemFeaturePlugin> moduleCollection = new ModuleCollection<>(this);

      moduleCollection.dataModules()
            .add(new Name("RegionIntegrationModule"),
                  "Computes Sa",
                  RegionIntegrationModule::new)
            .add(new Name("RefLogDataModule"),
                  "Loads reference log data",
                  RefLogDataModule::new)
            .add(new Name("CTDDataModule"),
                  "Loads CTD data",
                  CTDDataModule::new)
            .add(new Name("FileDrawDataModule"),
                  "Loads file draw data",
                  FileDrawDataModule::new)
            .add(new Name("CommentDataModule"),
                  "Loads comments",
                  CommentDataModule::new)
            .add(new Name("KoronaRegionModule"),
                  "Loads detected KORONA regions",
                  KoronaRegionModule::new)
            .add(new Name("ConditionalMaskingExtractionModule"),
                  "Computes the conditional masking",
                  ConditionalMaskingExtractionModule::new)
            .add(new Name("SchoolParameterModule"),
                  "Computes school parameters",
                  SchoolParameterModule::new)
            .add(new Name("TrackInfoModule"),
                  "Loads info about tracks",
                  TrackInfoModule::new);

      moduleCollection.viewModules(Where.UNSPECIFIED)
            .add(new Name("ColorBarModule", "Colorbar"),
                  "The echogram colorbar",
                  OnStartup.ENABLED, ColorBarModule::new)
            .add(new Name("PelagicEchogramModule", "Pelagic echogram"),
                  "Displays the pelagic echogram",
                  OnStartup.ENABLED, PelagicEchogramModule::new)
            .add(new Name("BottomEchogramModule", "Bottom echogram"),
                  "Displays the bottom echogram",
                  OnStartup.ENABLED, BottomEchogramModule::new)
            .add(new Name("ScrollBarModule"),
                  "The horizontal scroll bar for the echogram",
                  OnStartup.ENABLED, ScrollBarModule::new);

      moduleCollection.viewModules(Where.BOTTOM)
            .add(new Name("MapModule", "Map"),
                  "Displays a map together with other types of geographical information",
                  OnStartup.ENABLED, MapModule::new)
            .add(new Name("FrequencyResponseModule", "Frequency response"),
                  "Displays the frequency response curve",
                  OnStartup.ENABLED, FrequencyResponseModule::new)
            .add(new Name("ScatterPlotModule", "Scatter plot"),
                  "Displays a scatter plot with frequency response along the axes",
                  OnStartup.DISABLED, ScatterPlotModule::new)
            .add(new Name("ThresholdResponseModule", "Threshold response"),
                  "Displays a plot of the threshold response",
                  OnStartup.ENABLED, ThresholdResponseModule::new)
            .add(new Name("TSModule", "TS distribution"),
                  "Displays a histogram of detected targets",
                  OnStartup.ENABLED, TSModule::new)
            .add(new Name("TsPositionsModule", "TS positions"),
                  "Displays the angles of detected targets",
                  OnStartup.DISABLED, TsPositionsModule::new)
            //todo: Add when Martha has published paper: .add(new Name("CorrelationModule", "Correlation"),
            //    "Displays the correlation between two frequency response curves computed with slightly different upper thresholds",
            //    OnStartup.DISABLED, CorrelationModule::new)
            .add(new Name("PlanktonModule", "Plankton"),
                  "Displays the results of plankton inversion done during preprocessing",
                  OnStartup.ENABLED, PlanktonModule::new)
            .add(new Name("SvDistributionModule", "Sv distribution"),
                  "Displays the probability density function of Sv values",
                  OnStartup.DISABLED, SvDistributionModule::new)
            .add(new Name("InterpretationModule", "Interpretation"),
                  "For assigning acoustic categories to layers and schools",
                  OnStartup.ENABLED, InterpretationModule::new)
            .add(new Name("CommentModule", "Comments"),
                  "Displays comments in a table",
                  OnStartup.DISABLED, CommentModule::new)
            .add(new Name("BroadbandSvModule", "BB Sv(f)"),
                  "Displays the Sv-frequency spectra",
                  OnStartup.DISABLED, BroadbandSvModule::new)
            .add(new Name("BroadbandSvByDepthModule", "BB Sv(f, depth)"),
                  "Displays a heatmap of Sv as a function of frequency and depth for the current ping",
                  OnStartup.DISABLED, BroadbandSvByDepthModule::new)
            .add(new Name("BroadbandTsModule", "BB TS(f)"),
                  "Displays the TS-frequency spectra for individual targets",
                  OnStartup.DISABLED, BroadbandTsModule::new)
            .add(new Name("BroadbandTsHistogramModule", "BB TS(f) histogram"),
                  "Displays a histogram of targets detected by BB TS(f)",
                  OnStartup.DISABLED, BroadbandTsHistogramModule::new)
            .add(new Name("KoronaExperimentationModule", "KORONA experimentation"),
                  "For experimenting with KORONA preprocessing",
                  OnStartup.DISABLED, KoronaExperimentationModule::new)
            .add(new Name("MagnifierModule", "Magnifier"),
                  "Displays a magnified image of the area around the current mouse position",
                  OnStartup.DISABLED, MagnifierModule::new);

      moduleCollection.viewModules(Where.RIGHT)
            .add(new Name("NumericalViewModule", "Numerical view"),
                  "Displays information about the datagrams in .raw files",
                  OnStartup.DISABLED, NumericalViewModule::new)
            .add(new Name("SystemInfoModule", "System info"),
                  "Displays some basic system information, such as Java version, memory usage, etc.",
                  OnStartup.DISABLED, SystemInfoModule::new)
            .add(new Name("CTDViewModule", "CTD"),
                  "Displays CTD sensor samplings as a function of depth",
                  OnStartup.DISABLED, CTDViewModule::new)
            .add(new Name("TrawlModule", "Trawl"),
                  "Displays catch data from trawl stations",
                  OnStartup.DISABLED, TrawlModule::new)
            .add(new Name("PingPlotModule", "Ping plot"),
                  "Displays an echogram variable against depth for the ping corresponding to the mouse position",
                  OnStartup.DISABLED, PingPlotModule::new)
            .add(new Name("BroadbandCalibrationPlotModule", "BB calibration plot"),
                  "Displays the frequency-dependent broadband calibration",
                  OnStartup.DISABLED, BroadbandCalibrationPlotModule::new);

      moduleCollection.viewModules(Where.BELOW_ECHOGRAM)
            .add(new Name("EchogramPlotModule", "Echogram plot"),
                  "Displays various functions defined from ping to a real number",
                  OnStartup.DISABLED, EchogramPlotModule::new);

      moduleCollection.overlays(EchogramModule.class)
            .add(new Name("EchogramImageOverlay", "Echogram image"),
                  "Displays the echogram image",
                  OnStartup.ENABLED, EchogramImageOverlay::new)
            .add(new Name("RegionEditOverlay", "Edit regions"),
                  "For editing layers and schools",
                  OnStartup.ENABLED, RegionEditOverlay::new)
            .add(new Name("MaskingEditOverlay", "Masking edit"),
                  "For editing frequency-dependent deletion",
                  OnStartup.ENABLED, MaskingEditOverlay::new)
            .add(new Name("ZoomOverlay", "Zoom"),
                  "For zooming the echogram",
                  OnStartup.ENABLED, ZoomOverlay::new)
            .add(new Name("RegionAddOverlay", "Add regions"),
                  "For adding layers and schools",
                  OnStartup.ENABLED, RegionAddOverlay::new)
            .add(new Name("SchoolCategoryOverlay"),
                  "Displays categorization of schools",
                  OnStartup.ENABLED, SchoolCategoryOverlay::new)
            .add(new Name("TrackCategoryEchogramOverlay"),
                  "Displays categorization of tracks",
                  OnStartup.ENABLED, TrackCategoryEchogramOverlay::new)
            .add(new Name("MaskingDisplayOverlay", "Masking"),
                  "Displays several kinds of semi-transparent masks on the echogram, such as database storing, exclusion, etc.",
                  OnStartup.ENABLED, MaskingDisplayOverlay::new)
            .add(new Name("BottomOverlay", "Bottom"),
                  "Displays the bottom",
                  OnStartup.ENABLED, BottomOverlay::new)
            .add(new Name("GridOverlay", "Grid"),
                  "Displays the grid stored to the database",
                  OnStartup.ENABLED, GridOverlay::new)
            .add(new Name("FileMarkerOverlay", "File markers"),
                  "Displays markers at the start of each data file at the bottom of the echogram",
                  OnStartup.ENABLED, FileMarkerOverlay::new)
            .add(new Name("PingMarkerOverlay", "Ping markers"),
                  "Displays the start of each loaded ping at the bottom of the echogram",
                  OnStartup.ENABLED, PingMarkerOverlay::new)
            .add(new Name("DepthMarkerOverlay", "Depth markers"),
                  "Displays equidistant horizontal lines stretching across the echogram",
                  OnStartup.ENABLED, DepthMarkerOverlay::new)
            .add(new Name("VerticalLineOverlay", "Vertical lines"),
                  "Displays equidistant vertical lines and Sa for each region",
                  OnStartup.ENABLED, VerticalLineOverlay::new)
            .add(new Name("TicksOverlay", "Ticks"),
                  "Displays equidistant markers at the top of the echogram",
                  OnStartup.ENABLED, TicksOverlay::new)
            .add(new Name("AccumulatedSaOverlay", "Integration line"),
                  "Displays the integration line for the selected regions",
                  OnStartup.ENABLED, AccumulatedSaOverlay::new)
            .add(new Name("KoronaRegionEchogramOverlay", "Preprocessed regions"),
                  "Displays regions that are automatically detected during preprocessing",
                  OnStartup.ENABLED, KoronaRegionEchogramOverlay::new)
            .add(new Name("TrackEchogramOverlay", "Tracks"),
                  "Displays detected tracks",
                  OnStartup.DISABLED, TrackEchogramOverlay::new)
            .add(new Name("RegionDisplayOverlay", "Regions"),
                  "Displays layers and schools",
                  OnStartup.ENABLED, RegionDisplayOverlay::new)
            .add(new Name("TSEchogramOverlay", "TS locations"),
                  "Displays the locations of the TS detections",
                  OnStartup.DISABLED, TSEchogramOverlay::new)
            .add(new Name("BroadbandTsEchogramOverlay", "BB TS(f) locations"),
                  "Displays the targets detected by BB TS(f)",
                  OnStartup.DISABLED, BroadbandTsEchogramOverlay::new)
            .add(new Name("FileDrawOverlay", "File draw"),
                  "Displays geometric shapes defined in file draw data",
                  OnStartup.ENABLED, FileDrawOverlay::new)
            .add(new Name("CommentEchogramOverlay", "Comments"),
                  "Displays the location of comments",
                  OnStartup.ENABLED, CommentEchogramOverlay::new)
            .add(new Name("RefLogEchogramOverlay", "Reference log"),
                  "Displays information from the reference log data",
                  OnStartup.ENABLED, RefLogEchogramOverlay::new)
            .add(new Name("SaOverlay", "Sa line"),
                  "Displays the sA line for the selected region",
                  OnStartup.DISABLED, SaOverlay::new)
            .add(new Name("MapPositionOverlay", "Map position"),
                  "Displays a red vertical line corresponding to the mouse position in the map",
                  OnStartup.ENABLED, MapPositionOverlay::new);

      moduleCollection.overlays(MapModule.class)
            .add(new Name("BackgroundMapOverlay", "Background map"),
                  "Displays the background map",
                  OnStartup.ENABLED, BackgroundMapOverlay::new)
            .add(new Name("SaMapOverlay", "Sa values"),
                  "Displays circles of different sizes depending on the sA value",
                  OnStartup.DISABLED, SaMapOverlay::new)
            .add(new Name("LatLonOverlay", "Latitudes and longitudes"),
                  "Displays a geographical grid of latitude and longitude lines",
                  OnStartup.ENABLED, LatLonOverlay::new)
            .add(new Name("ScaleMapOverlay", "Length scale"),
                  "Displays a length scale in the lower right corner",
                  OnStartup.ENABLED, ScaleMapOverlay::new)
            .add(new Name("ZoomMapOverlay", "Zoom"),
                  "For zooming the map",
                  OnStartup.ENABLED, ZoomMapOverlay::new)
            .add(new Name("DragOverlay", "Drag"),
                  "For panning the map by dragging the mouse",
                  OnStartup.ENABLED, DragOverlay::new)
            .add(new Name("SelectionMapOverlay", "Selection"),
                  "For drawing a selection rectangle",
                  OnStartup.ENABLED, SelectionMapOverlay::new)
            .add(new Name("PanOverlay", "Pan"),
                  "For panning the map by clicking at the edges",
                  OnStartup.ENABLED, PanOverlay::new)
            .add(new Name("SurveyLineOverlay", "Survey line"),
                  "Displays the vessel line with indications of database storage and exclusions",
                  OnStartup.ENABLED, SurveyLineOverlay::new)
            .add(new Name("FileMarkerMapOverlay", "File markers"),
                  "Displays the beginning of each data file",
                  OnStartup.ENABLED, FileMarkerMapOverlay::new)
            .add(new Name("CommentMapOverlay", "Comments"),
                  "Displays the location of comments",
                  OnStartup.ENABLED, CommentMapOverlay::new)
            .add(new Name("RefLogMapOverlay", "Reference log"),
                  "Displays information from the reference log data",
                  OnStartup.ENABLED, RefLogMapOverlay::new)
            .add(new Name("CTDMapOverlay", "CTD map Overlay"),
                  "Displays locations with CTD data",
                  OnStartup.ENABLED, CTDMapOverlay::new)
            .add(new Name("EchogramPositionOverlay", "Echogram position"),
                  "Displays a red dot corresponding to the mouse position in the echogram",
                  OnStartup.ENABLED, EchogramPositionOverlay::new);

      return moduleCollection;
   }

   @Override
   public List<Exporter> createExporters() {
      return List.of(
            new BroadbandBottomDataExporter(this),
            new BroadbandSampleDataExporter(this),
            new BroadbandSvExporter(this),
            new BroadbandTrackExporter(this),
            new BroadbandTsExporter(this),
            new EchogramPlotExporter(this),
            new PelagicEchogramExporter(this),
            new SchoolParameterExport(this),
            new SvExporter(this),
            new TrackExporter(this),
            new TSExporter(this)
      );
   }

   @Override
   public List<SubDir> getSubDirs() {
      return List.of(
            DataConfLSSS.LSSS_SUB_DIR,
            DataConfLSSS.RAW_SUB_DIR,
            DataConfLSSS.KORONA_SUB_DIR,
            DataConfLSSS.TRAWL_SUB_DIR,
            DataConfLSSS.CTD_SUB_DIR,
            DataConfLSSS.REF_LOG_SUB_DIR,
            DataConfLSSS.FILE_DRAW_SUB_DIR,
            DataConfLSSS.REPORTS_DIR,
            DataConfLSSS.WORK_SUB_DIR,
            DataConfLSSS.EXPORT_SUB_DIR
      );
   }

   @Override
   public DatabaseContent getDatabaseContent() {
      return databaseContent;
   }

   @Override
   public BaseSystemWorkFileManager getWorkFileManager() {
      return workFileManager;
   }

   @Override
   public List<Path> getExcludedDirsForCopying() {
      Path cacheDir = getLSSS().getSurveyManager().getCacheDir();
      return cacheDir != null ? List.of(cacheDir) : List.of();
   }
}
