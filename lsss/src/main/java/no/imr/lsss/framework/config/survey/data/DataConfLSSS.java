package no.imr.lsss.framework.config.survey.data;

import com.google.common.html.HtmlEscapers;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.FileOpenRequest;
import no.imr.korona.data.util.DiscardZeroDepthSegmentData;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.InterpretationZSettings;
import no.imr.lsss.framework.NavigationHistory;
import no.imr.lsss.framework.config.application.SubDir;
import no.imr.lsss.framework.config.survey.SurveyDirectoryParameter;
import no.imr.lsss.framework.config.survey.data.extra.ExtraDataConf;
import no.imr.lsss.framework.config.survey.data.remote.RemoteDataConf;
import no.imr.lsss.framework.config.survey.misc.SurveyMiscConf;
import no.imr.lsss.framework.config.survey.preprocessing.OnTheFlySetup;
import no.imr.lsss.framework.packages.LsssAction;
import no.imr.lsss.resources.LsssIcons;
import no.imr.tools.Max;
import no.imr.tools.UnionList;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.ToolTipManagerState;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.svg.SvgIcon;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class DataConfLSSS extends DataConf {
   public static final SubDir LSSS_SUB_DIR = new SubDir(new Name("MainConfigDir"), new Name("ConfigDir"), "lsss");
   public static final SubDir RAW_SUB_DIR = new SubDir(new Name("MainDataDir"), new Name("DataDir"), "Raw");
   public static final SubDir KORONA_SUB_DIR = new SubDir(new Name("MainKoronaDataDir"), new Name("KoronaDataDir"), "KORONA");
   public static final SubDir TRAWL_SUB_DIR = new SubDir(new Name("MainTrawlDataDir"), new Name("TrawlDataDir"), "Trawl");
   public static final SubDir CTD_SUB_DIR = new SubDir(new Name("MainCTDDataDir"), new Name("CTDDataDir"), "CTD");
   public static final SubDir REF_LOG_SUB_DIR = new SubDir(new Name("MainRefLogDataDir"), new Name("RefLogDataDir"), "RefLog");
   public static final SubDir FILE_DRAW_SUB_DIR = new SubDir(new Name("MainFileDrawDataDir"), new Name("FileDrawDataDir"), "FileDraw");
   public static final SubDir REPORTS_DIR = new SubDir(new Name("MainReportsDir"), new Name("ReportsDir"), "Reports");
   public static final SubDir WORK_SUB_DIR = new SubDir(new Name("MainWorkDir"), new Name("WorkDir"), "Work");
   public static final SubDir EXPORT_SUB_DIR = new SubDir(new Name("MainExportDir"), new Name("ExportDir"), "Export");

   private final ExtraDataConf extraDataConf;
   private final RemoteDataConf remoteDataConf;

   private final RawDataDirParameter dataDir = new RawDataDirParameter("Select directory for data files", RAW_SUB_DIR, getLSSS());
   private final ProcessedDataDirParameter koronaDataDir = new ProcessedDataDirParameter("Select directory for KORONA data files", KORONA_SUB_DIR, getLSSS());

   private final DataConfEK500 dataConfEK500 = new DataConfEK500(this);

   private boolean koronaButtonTooltipAlwaysOn;

   public DataConfLSSS(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("DataConf", "Data files"),
            plugin.getLSSS().getDataSetManager(), new DefaultSegmentHandleFactory(plugin.getLSSS()));

      addDataFileDirectoryParameter(dataDir);
      addDataFileDirectoryParameter(koronaDataDir);
      addDirectoryParameter(new SurveyDirectoryParameter("Select directory for trawl data files", TRAWL_SUB_DIR, getLSSS()));
      addDirectoryParameter(new SurveyDirectoryParameter("Select directory for CTD data files", CTD_SUB_DIR, getLSSS()));
      addDirectoryParameter(new SurveyDirectoryParameter("Select directory for reference log files", REF_LOG_SUB_DIR, getLSSS()));
      addDirectoryParameter(new SurveyDirectoryParameter("Select directory for draw files", FILE_DRAW_SUB_DIR, getLSSS()));
      addDirectoryParameter(new SurveyDirectoryParameter("Select directory for generated reports", REPORTS_DIR, getLSSS()));
      addDirectoryParameter(new SurveyDirectoryParameter("Select directory for work files", WORK_SUB_DIR, getLSSS()));
      addDirectoryParameter(new SurveyDirectoryParameter("Select directory for export", EXPORT_SUB_DIR, getLSSS()));

      extraDataConf = new ExtraDataConf(plugin);
      remoteDataConf = new RemoteDataConf(plugin);

      SwingUtilities.invokeLater(() -> {
         getButtonPanel().add(dataConfEK500.getButton());

         getKoronaButton().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
               if (koronaButtonTooltipAlwaysOn) {
                  ToolTipManagerState.ALWAYS_ON.apply();
               }
            }

            @Override
            public void mouseExited(MouseEvent e) {
               ToolTipManagerState.DEFAULT.apply();
            }
         });
      });
   }

   public void afterPluginsAddedConfiguration() {
      addSubConfigurationUnit(extraDataConf);
      addSubConfigurationUnit(remoteDataConf);
   }

   @Override
   public void setup() {
      super.setup();

      OnTheFlySetup onTheFlySetup = getPreprocessingConf().getOnTheFlySetup();
      if (onTheFlySetup != null) {
         onTheFlySetup.getEditOkChangeManager().addListener(() -> {
            if (getConfigurationManager().isShowing()) {
               triggerReloadDataOnApply();
            } else {
               getLSSS().getInterpretationSettings().reloadData();
            }
         });
      }

      getFileTableChangeManager().addListener(() -> {
         dataConfEK500.check(getAllOriginalSegmentHandles());
      });
   }

   public ExtraDataConf getExtraDataConf() {
      return extraDataConf;
   }

   public RemoteDataConf getRemoteDataConf() {
      return remoteDataConf;
   }

   @Override
   public SurveyDirectoryParameter getRawDir() {
      return dataDir;
   }

   @Override
   public SurveyDirectoryParameter getProcessedDir() {
      return koronaDataDir;
   }

   @Override
   public List<SurveyDirectoryParameter> getAllPreprocessingTargetDirs() {
      return List.of(koronaDataDir);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return new UnionList<>(super.getParameters(), getAllDirectoryParameters());
   }

   @Override
   boolean useMissingBot() {
      return true;
   }

   @Override
   public boolean useConfigurableOnTheFlyProcessing() {
      return true;
   }

   @Override
   public FileOpenRequest.OnTheFlyProcessing getBuiltInOnTheFlyProcessing() {
      SurveyMiscConf surveyMiscConf = getConfigurationManager().getSurveyMiscConf();
      BooleanSupplier discard = () -> !surveyMiscConf.pelagicMode.getBooleanValue() && !surveyMiscConf.seabedMounted.getBooleanValue();
      return (segmentData, segmentHandle) -> new DiscardZeroDepthSegmentData(segmentData, discard);
   }

   @Override
   protected void updateSelectedDataFiles(DataType dataType) {
      getLSSS().getInterpretationSettings().getNavigationHistory().doWithNoAddCheckPoint(() -> {
         NavigationHistory.NavigationState currentState = getLSSS().getInterpretationSettings().getNavigationHistory().getCurrentState();
         getLSSS().getInterpretationSettings().cancelAll();
         getDataSetManager().updateSelectedDataFiles(dataType);
         getLSSS().getInterpretationSettings().dataFilesUpdated();
         getLSSS().getInterpretationSettings().doNonInteractively(currentState::apply);
      });
   }

   @Override
   protected boolean canUpdateSelectedDataFilesWithIncompatiblePingConfiguration() {
      return true;
   }

   @Override
   protected void updateSelectedDataFilesWithIncompatibility(DataType dataType) {
      getLSSS().getInterpretationSettings().reloadData();
      if (dataType != getDataSetManager().getSelectedDataType()) {
         // The user pressed cancel in save dialog before reloading data.
         getKoronaButton().setSelected(getDataSetManager().getSelectedDataType() == DataType.PROCESSED);
      }
   }

   @Override
   protected void updateKoronaButton(boolean selectionUsable, DataFileSet.Compatibility compatibility, boolean hasKoronaFiles) {
      JToggleButton koronaButton = getKoronaButton();
      koronaButton.setEnabled(selectionUsable && hasKoronaFiles);
      if (compatibility == DataFileSet.Compatibility.OK || !hasKoronaFiles) {
         koronaButtonTooltipAlwaysOn = false;
         LsssIcons.KORONA.on(koronaButton);
         koronaButton.setToolTipText(getDataSetToggleButtonTooltip());
      } else if (compatibility == DataFileSet.Compatibility.INCOMPATIBLE_PING_CONFIGURATION) {
         koronaButtonTooltipAlwaysOn = true;
         LsssIcons.KORONA_ERROR.on(koronaButton);
         koronaButton.setToolTipText("<html>" + getDataSetToggleButtonTooltip() + """
               <br><span style='color: red'>
               <br>The KORONA data files are not compatible with the original data.
               <br>Switching to / from KORONA data will trigger reloading work files
               <br>which can cause changes to the interpretation.
               </span>
               """);
      } else {
         koronaButtonTooltipAlwaysOn = true;
         LsssIcons.KORONA_ERROR.on(koronaButton);
         koronaButton.setToolTipText("<html>" + getDataSetToggleButtonTooltip() + """
               <br><span style='color: red'>
               <br>The KORONA data files contain different selection of pings
               <br>and cannot use the same work files as the original data.
               </span>
               """);
      }
   }

   @Override
   protected void installNewDataFiles(DataSetLoader dataSetLoader, DataType dataType) {
      getLSSS().getInterpretationSettings().getWorkFilesLoading().setValue(true);

      ProgressView preparingProgressView = new ProgressView("Preparing to use new data files...", 1000)
            .hideMainProgressLabel();
      new WorkerDialog(getLSSS().getReferenceComponent(), preparingProgressView.getComponent())
            .startWithoutCancel(() -> {
               getLSSS().getInterpretationSettings().dataFilesAboutToChange();
               getDataSetManager().installNewDataFiles(dataSetLoader, dataType);
               getDataSetManager().getDataFileSet().getMaxDepth(preparingProgressView.getMainProgressHandler());
               getLSSS().getInterpretationSettings().dataFilesChanged();
            });

      ProgressView progressView = new ProgressView("Loading work files", getDataSetManager().getDataFileSet().getDataFiles().size());
      new WorkerDialog(getLSSS().getReferenceComponent(), progressView.getComponent())
            .startWithoutCancel(() -> {
               ((BaseSystemFeaturePlugin) getPlugin()).getWorkFileManager().load(progressView.getMainProgressHandler());

               if (!getLSSS().getInterpretationSettings().getDataFileSet().getDataConfiguration().isSeabedMounted()) {
                  float maxDepth = (float) Math.ceil(getMaxLayerDepth() * InterpretationZSettings.DEFAULT_MAX_DEPTH_FACTOR);
                  InterpretationZSettings.Pelagic pelagicZSettings = getLSSS().getInterpretationSettings().getPelagicZSettings();
                  FloatRange maxZRange = pelagicZSettings.getMaxZRange();
                  if (maxZRange.max() < maxDepth) {
                     pelagicZSettings.setMaxZ(maxZRange.min(), maxDepth);
                     pelagicZSettings.zoomOut();
                  }
               }
            });

      getLSSS().getInterpretationSettings().getWorkFilesLoading().setValue(false);
      getLSSS().getInterpretationSettings().resetNavigation();
   }

   private double getMaxLayerDepth() {
      return getLSSS().getRegionManager().getLayerManager().getLayers().parallelStream()
            .flatMap(layer -> layer.getLowerCurveBoundaries().stream())
            .filter(curveBoundary -> curveBoundary.getLayers().size() == 1)
            .mapToDouble(curveBoundary -> Max.of(curveBoundary.getCurve().getDepths()))
            .max()
            .orElse(0);
   }

   @Override
   public LsssAction getStartPreprocessingAction() {
      return getLSSS().getActions().startPreprocessing;
   }

   @Override
   protected SvgIcon getDataSetToggleButtonIcon() {
      return LsssIcons.KORONA;
   }

   @Override
   protected String getDataSetToggleButtonTooltip() {
      return "Use preprocessed echosounder data";
   }

   @Override
   public JComponent getComponent() {
      JPanel mainPanel = new JPanel(new BorderLayout());

      JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      topPanel.setBorder(BorderFactory.createEtchedBorder());
      topPanel.add(new JLabel("Survey file: "));
      Path surveyFile = getLSSS().getSurveyManager().getSurveyFile();
      if (surveyFile != null) {
         Path dir = surveyFile.getParent();
         JTextPane fileComponent = GuiUtils.labelLikeHtmlTextPane("<a href=dir>" + HtmlEscapers.htmlEscaper().escape(dir.toString()) + "</a>"
                     + File.separatorChar + HtmlEscapers.htmlEscaper().escape(surveyFile.getFileName().toString()),
               href -> {
                  switch (href) {
                     case "dir" -> GuiUtils.desktopBrowse(dir.toUri(), mainPanel);
                     default -> {
                     }
                  }
               });
         topPanel.add(fileComponent);
      } else {
         topPanel.add(new JLabel("<None>"));
      }

      mainPanel.add(topPanel, BorderLayout.NORTH);
      mainPanel.add(super.getComponent());
      return mainPanel;
   }
}
