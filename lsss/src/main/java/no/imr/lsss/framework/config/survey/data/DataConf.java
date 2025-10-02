package no.imr.lsss.framework.config.survey.data;

import com.google.common.html.HtmlEscapers;
import no.imr.korona.apps.relay.KoronaRelay;
import no.imr.korona.apps.relay.KoronaRelayUpdate;
import no.imr.korona.apps.relay.KoronaRelayUpdateChecker;
import no.imr.korona.apps.relay.KoronaRelayUtils;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.config.ConfigFileSettingsUtils;
import no.imr.korona.data.DataException;
import no.imr.korona.data.datagrams.Cds0Datagram;
import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.FileOpenRequest;
import no.imr.korona.data.datamanager.labelling.DataFileLabel;
import no.imr.korona.data.datamanager.labelling.DataFileLabelEditor;
import no.imr.korona.data.datamanager.labelling.DataFileLabelUtils;
import no.imr.korona.data.datamanager.labelling.DataFileLabelling;
import no.imr.korona.data.formats.ek60.CreateMissingBotGUI;
import no.imr.korona.data.formats.ek60.CreateMissingIdxGUI;
import no.imr.korona.data.formats.ek60.calibration.CalibrationFile;
import no.imr.korona.data.formats.ek60.calibration.CalibrationGui;
import no.imr.korona.data.ping.PingIndexCorrectionOptions;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.korona.data.util.ProcessingSegmentData;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.SurveyManager;
import no.imr.lsss.framework.config.application.DirectoryConf;
import no.imr.lsss.framework.config.application.SubDir;
import no.imr.lsss.framework.config.application.SurveyDirStructure;
import no.imr.lsss.framework.config.survey.SurveyDirectoryParameter;
import no.imr.lsss.framework.config.survey.preprocessing.OnTheFlySetup;
import no.imr.lsss.framework.config.survey.preprocessing.PreprocessingConf;
import no.imr.lsss.framework.packages.LsssAction;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.plugins.WorkFileManager;
import no.imr.lsss.util.LsssUtils;
import no.imr.lsss.util.ProcessingSetupDialog;
import no.imr.lsss.viewer.Shortcuts;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.concurrent.SerialExecutor;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.gui.ConfigurableGUI;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.IntRange;
import no.imr.tools.range.Range;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.FileListTransferHandler;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MenuUtils;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.ScrollablePanel;
import no.imr.tools.swing.SwingDelayer;
import no.imr.tools.swing.ToolTipManagerState;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.svg.SvgIcon;
import no.imr.tools.swing.table.TableUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Configurations of data files.
 */
public abstract class DataConf extends SurveyDirectoryConf {
   protected final class RawDataDirParameter extends SurveyDirectoryParameter {
      private @Nullable Path previousFile;

      public RawDataDirParameter(String dialogTitle, SubDir subDir, LSSS lsss) {
         super(dialogTitle, subDir, lsss);

         subscribe(__ -> {
            Path file = getFile();
            Path parentFile = FileUtils.getParent(file);
            if (parentFile != null && !parentFile.equals(FileUtils.getParent(previousFile))) {
               possiblyChangeOtherDirectories(this);
            }
            rawUpdateChecker = file != null ? new KoronaRelayUpdateChecker(file) : null;
            checkForUpdatesFromKoronaRelay(rawUpdateChecker, false);
            previousFile = file;
         });
      }

      @Override
      public List<FileFilter> getFileFilters() {
         return getLSSS().getKorona().getDataFormatManager().getFileFilters();
      }

      @Override
      public void customizeFileChooser(JFileChooser fileChooser) {
         super.customizeFileChooser(fileChooser);
         ConfigFileSettingsUtils.installTooltip(fileChooser);
      }
   }

   protected final class ProcessedDataDirParameter extends SurveyDirectoryParameter {
      public ProcessedDataDirParameter(String dialogTitle, SubDir subDir, LSSS lsss) {
         super(dialogTitle, subDir, lsss);

         subscribe(__ -> {
            Path file = getFile();
            processedUpdateChecker = file != null ? new KoronaRelayUpdateChecker(file) : null;
            checkForUpdatesFromKoronaRelay(processedUpdateChecker, false);
         });
      }

      @Override
      public List<FileFilter> getFileFilters() {
         return getLSSS().getKorona().getDataFormatManager().getFileFilters();
      }

      @Override
      public void customizeFileChooser(JFileChooser fileChooser) {
         super.customizeFileChooser(fileChooser);
         ConfigFileSettingsUtils.installTooltip(fileChooser);
      }
   }

   private final List<SurveyDirectoryParameter> allDirectoryParameters = new ArrayList<>();
   private final Map<SubDir, SurveyDirectoryParameter> subDirToDirectoryParameter = new LinkedHashMap<>();

   final BooleanParameter sortByTime = new BooleanParameter(new Name("SortByTime"),
         false);

   final ObjectParameter<TimeGrouping> timeGrouping = new ObjectParameter<>(new Name("TimeGrouping"),
         TimeGrouping.DAYS, TimeGrouping.values());

   private final StringParameter firstSelectedFile = new StringParameter(new Name("FirstSelectedFile"));
   private final StringParameter lastSelectedFile = new StringParameter(new Name("LastSelectedFile"));

   private boolean doingFromXml;

   private @Nullable DataFileLabelling dataFileLabelling;

   private final Executor backgroundExecutor = new SerialExecutor(Exec.CACHED_THREAD_POOL);
   private final Executor rawBackgroundExecutor = new SerialExecutor(backgroundExecutor);
   private AsyncHandle rawBackgroundAsyncHandle = new AsyncHandle();
   private final Executor processedBackgroundExecutor = new SerialExecutor(backgroundExecutor);
   private AsyncHandle processedBackgroundAsyncHandle = new AsyncHandle();

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private final View view; // todo: Only access via viewHolder

   private final DataSetManager dataSetManager;
   private final SegmentHandleFactory segmentHandleFactory;
   private final DataSetLoader dataSetLoader;
   private boolean dataFileTableAutoScroll;

   private boolean forceReloadOnApply;
   private @Nullable KoronaRelayUpdateChecker rawUpdateChecker;
   private @Nullable KoronaRelayUpdateChecker processedUpdateChecker;
   private boolean isCheckingForKoronaUpdates;

   private final ArgChangeManager<Range<Long>> ntDateSelectionChangeManager = new ArgChangeManager<>();
   private final ChangeManager fileTableChangeManager = new ChangeManager();

   protected DataConf(FeaturePlugin plugin, Name name, DataSetManager dataSetManager, SegmentHandleFactory segmentHandleFactory) {
      super(plugin, name, LsssUtils.infoText("Selection of which", plugin, "data files to open"));

      sortByTime.setVisible(false);
      timeGrouping.setVisible(false);
      firstSelectedFile.setVisible(false);
      lastSelectedFile.setVisible(false);

      this.dataSetManager = dataSetManager;
      this.segmentHandleFactory = segmentHandleFactory;

      dataSetLoader = new DataSetLoader(dataSetManager.getDataManager().getDataConfiguration());

      view = GuiUtils.getNowOrWait(viewHolder::getView);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(sortByTime, timeGrouping, firstSelectedFile, lastSelectedFile);
   }

   @Override
   public void prepareForSaveDefault() {
      super.prepareForSaveDefault();
      selectNothing();
   }

   public SegmentHandlesAndAttributes createSegmentHandles(@Nullable Path dir) {
      if (dir == null) {
         return new SegmentHandlesAndAttributes(SegmentHandlesAndAttributes.DirListingStatus.NOT_SPECIFIED);
      }
      AtomicReference<SegmentHandlesAndAttributes> result = new AtomicReference<>(new SegmentHandlesAndAttributes(SegmentHandlesAndAttributes.DirListingStatus.CANCELLED));
      new WorkerDialog(getLSSS().getReferenceComponent(), "Searching for files in directory\n" + dir)
            .setWaitUntilFinishedIfCancelled(false)
            .setOnError(e -> {
               result.set(new SegmentHandlesAndAttributes(SegmentHandlesAndAttributes.DirListingStatus.ERROR));
               getLSSS().showError("Error reading " + dir, e);
            })
            .start(asyncHandle -> {
               result.set(segmentHandleFactory.createSegmentHandlesAndAttributes(dir, asyncHandle));
            });
      return result.get();
   }

   public JPanel getButtonPanel() {
      return view.buttonPanel;
   }

   public DataSetManager getDataSetManager() {
      return dataSetManager;
   }

   public SegmentHandleFactory getSegmentHandleFactory() {
      return segmentHandleFactory;
   }

   @Override
   public void setup() {
      super.setup();

      GuiListeners.later(view.dataFileTableModel::updateSa).addTo(
            getLSSS().getInterpretationSummary().getChangeManager(),
            getConfigurationManager().getSurveyMiscConf().mainFrequency
      );

      FileParameter workDirParameter = getConfigurationManager().getDataConf().getDir(DataConfLSSS.WORK_SUB_DIR);
      workDirParameter.subscribe(__ -> reloadDataFileLabelling());

      GuiUtils.invokeNowOrWait(() -> viewHolder.getView().setup());
   }

   protected void onMissingIdxFileButtonPress(JComponent referenceComponent, Collection<? extends SegmentHandle> segmentHandles) {
      String speed = "Speed (recommended)";
      String pos = "Position";

      ObjectParameter<String> method = new ObjectParameter<>(
            new Name("Method", "Method for computing sailed distance"),
            speed, List.of(speed, pos));

      BooleanParameter useVesselDistance = new BooleanParameter(
            new Name("UseVesselDistance", "Use vessel distance in raw files"),
            true);

      BooleanParameter collapse = new BooleanParameter(
            new Name("Collapse", "Collapse sequential pinging"),
            false);

      List<BaseParameter<?>> parameters = List.of(method, useVesselDistance, collapse);
      ParameterCollection configurable = new ParameterCollection(parameters);
      boolean ok = new ConfigurableGUIDialog(referenceComponent, "Missing .idx files", configurable)
            .setTopText("<html><br><p>Some files lack index files in directory<br>" + getRawDir().getFile() + "<br><br>Create missing index files?</p><br>")
            .setGUI(new ConfigurableGUI().createComponent(configurable))
            .show();
      if (ok) {
         PingIndexCorrectionOptions pingIndexCorrectionOptions = new PingIndexCorrectionOptions(
               method.getValue().equals(pos),
               useVesselDistance.getBooleanValue());
         new CreateMissingIdxGUI(getLSSS().getKorona().getDatagramTypeManager(), segmentHandles, pingIndexCorrectionOptions, collapse.getBooleanValue())
               .start(referenceComponent);
      }
   }

   private static void onMissingBotFileButtonPress(JComponent referenceComponent, Collection<? extends SegmentHandle> segmentHandles) {
      int answer = GuiUtils.showOptionDialog(referenceComponent, "Missing .bot files", "Create missing .bot files from .xyz files?",
            new String[]{"Create", "Cancel"});
      if (answer == 0) {
         new CreateMissingBotGUI(segmentHandles).start(referenceComponent);
      }
   }

   public ArgChangeManager<Range<Long>> getNTDateSelectionChangeManager() {
      return ntDateSelectionChangeManager;
   }

   public ChangeManager getFileTableChangeManager() {
      return fileTableChangeManager;
   }

   private DirectoryConf getDirectoryConf() {
      return getConfigurationManager().getApplicationConfiguration().getDirectoryConf();
   }

   public FileParameter getDir(SubDir subDir) {
      return subDirToDirectoryParameter.get(subDir);
   }

   @Override
   public List<SurveyDirectoryParameter> getAllDirectoryParameters() {
      return allDirectoryParameters;
   }

   protected void addDirectoryParameter(SurveyDirectoryParameter directoryParameter) {
      subDirToDirectoryParameter.put(directoryParameter.getSubDir(), directoryParameter);
      allDirectoryParameters.add(directoryParameter);
   }

   protected void addDataFileDirectoryParameter(SurveyDirectoryParameter parameter) {
      addDirectoryParameter(parameter);
      parameter.setPopupMenuExtender(menu -> {
         menu.addSeparator();
         JMenuItem reCreateStatusXmlItem = menu.add("Re-create status.xml");
         Path dir = parameter.getFile();
         if (dir != null) {
            reCreateStatusXmlItem.addActionListener(e -> {
               KoronaRelayUtils.showReCreateStatusXmlDialog(viewHolder.getComponent(), dir.resolve(KoronaRelay.STATUS_FILENAME));
            });
         } else {
            reCreateStatusXmlItem.setEnabled(false);
         }
      });
   }

   /**
    * Other directories suggested being parallel to the directory changed manually.
    *
    * @param referenceDirectoryParameter the directory the other directories should be parallel to
    */
   private void possiblyChangeOtherDirectories(SurveyDirectoryParameter referenceDirectoryParameter) {
      if (doingFromXml) {
         return;
      }
      if (!(getLSSS().getInterpretationSettings().isInteractiveMode() && isShowing())) {
         return;
      }

      SurveyDirStructure surveyDirStructure = getDirectoryConf().getSelectedSurveyDirStructure();
      int nameCount = Path.of(surveyDirStructure.getRelativePath(referenceDirectoryParameter.getSubDir())).getNameCount();
      Path mainDir = referenceDirectoryParameter.getFile();
      for (int i = 0; i < nameCount; i++) {
         mainDir = FileUtils.getParent(mainDir);
      }
      if (mainDir == null) {
         return;
      }

      JPanel gridBagPanel = new ScrollablePanel(new GridBagLayout());
      gridBagPanel.setBorder(GuiUtils.DEFAULT_MARGIN);
      GridBag gridBag = new GridBag(gridBagPanel);
      gridBag.getConstraints().anchor = GridBagConstraints.WEST;
      gridBag.getConstraints().gridwidth = 1;

      gridBag.add(new JLabel("Change other directories than " + referenceDirectoryParameter.getDisplayName() + " ?  "));
      gridBag.addWithLineBreak(new JLabel(referenceDirectoryParameter.getFile().toString()));

      gridBag.addWithLineBreak(Box.createVerticalStrut(15));

      Map<SurveyDirectoryParameter, JCheckBox> checkBoxes = new HashMap<>();

      for (SurveyDirectoryParameter directoryParameter : allDirectoryParameters) {
         if (directoryParameter == referenceDirectoryParameter) {
            continue;
         }
         addCheckBox(directoryParameter, mainDir, surveyDirStructure, gridBag, checkBoxes);
      }

      for (SurveyDirectoryConf surveyDirectoryConf : getAllSurveyDirectoryConfs()) {
         if (surveyDirectoryConf == this) {
            continue;
         }
         gridBag.addWithLineBreak(Box.createVerticalStrut(15));
         gridBag.activateHorizontalFill();
         gridBag.addWithLineBreak(new JSeparator());
         gridBag.deactivateFill();
         gridBag.addWithLineBreak(Box.createVerticalStrut(15));
         gridBag.addWithLineBreak(new JLabel(surveyDirectoryConf.getDisplayName()));
         gridBag.addWithLineBreak(Box.createVerticalStrut(5));

         for (SurveyDirectoryParameter directoryParameter : surveyDirectoryConf.getAllDirectoryParameters()) {
            addCheckBox(directoryParameter, mainDir, surveyDirStructure, gridBag, checkBoxes);
         }
      }

      JDialog dialog = new JDialog(GuiUtils.windowForComponent(view.mainPanel), "Change other directories?", Dialog.ModalityType.DOCUMENT_MODAL);

      AtomicBoolean changeDirectories = new AtomicBoolean();

      JButton yesButton = new JButton("Yes");
      yesButton.setMnemonic(KeyEvent.VK_Y);
      yesButton.addActionListener(e -> {
         changeDirectories.set(true);
         dialog.dispose();
      });
      JButton noButton = new JButton("No");
      noButton.setMnemonic(KeyEvent.VK_N);
      noButton.addActionListener(e -> dialog.dispose());
      GuiUtils.setAccelerator(noButton, Shortcuts.ESCAPE);

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      buttonPanel.add(yesButton);
      buttonPanel.add(noButton);

      JPanel dialogPanel = new JPanel(new BorderLayout());
      JScrollPane scrollPane = new JScrollPane(gridBagPanel);
      dialogPanel.add(scrollPane);
      dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      dialog.getRootPane().setDefaultButton(yesButton);
      dialog.add(dialogPanel);
      dialog.pack();
      GuiUtils.expandSizeWith(dialog, scrollPane.getVerticalScrollBar().getPreferredSize().width, 0);
      dialog.setLocationRelativeTo(view.mainPanel);
      GuiUtils.clampToScreen(dialog);
      dialog.setVisible(true);

      if (changeDirectories.get()) {
         for (Map.Entry<SurveyDirectoryParameter, JCheckBox> entry : checkBoxes.entrySet()) {
            JCheckBox checkBox = entry.getValue();
            SurveyDirectoryParameter directoryParameter = entry.getKey();

            if (checkBox.isSelected()) {
               Path dir = mainDir.resolve(surveyDirStructure.getRelativePath(directoryParameter.getSubDir()));
               directoryParameter.setFile(dir);
            }
         }
      }
   }

   private static void addCheckBox(SurveyDirectoryParameter directoryParameter, Path mainDir, SurveyDirStructure surveyDirStructure, GridBag gridBag, Map<SurveyDirectoryParameter, JCheckBox> checkBoxes) {
      JCheckBox checkBox = new JCheckBox(directoryParameter.getDisplayName() + ":", true);
      gridBag.add(checkBox);

      Path dir = mainDir.resolve(surveyDirStructure.getRelativePath(directoryParameter.getSubDir()));
      JLabel dirName = new JLabel(dir.toString());
      gridBag.addWithLineBreak(dirName);

      checkBoxes.put(directoryParameter, checkBox);
   }

   public List<DataConf> getAllDataConfs() {
      return getConfigurationManager().getDataConf().getAllUnitsRecursively(DataConf.class)
            .toList();
   }

   public List<SurveyDirectoryConf> getAllSurveyDirectoryConfs() {
      return getConfigurationManager().getDataConf().getAllUnitsRecursively(SurveyDirectoryConf.class)
            .toList();
   }

   public PreprocessingConf getPreprocessingConf() {
      return getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getAllUnitsRecursively(PreprocessingConf.class)
            .filter(preprocessingConf -> preprocessingConf.getDataConf() == this)
            .findFirst()
            .orElseThrow();
   }

   @Override
   public JComponent getComponent() {
      updateParameterPanel();

      SwingUtilities.invokeLater(() -> {
         checkForUpdates(false);
      });

      return view.getComponent();
   }

   public boolean isShowing() {
      return view.mainPanel.isShowing();
   }

   protected @Nullable Component getReferenceComponent() {
      return isShowing() ? view.mainPanel : getLSSS().getReferenceComponent();
   }

   protected void checkForUpdatesFromKoronaRelay(boolean forceCheck) {
      checkForUpdatesFromKoronaRelay(rawUpdateChecker, forceCheck);
      checkForUpdatesFromKoronaRelay(processedUpdateChecker, forceCheck);
   }

   private void checkForUpdatesFromKoronaRelay(@Nullable KoronaRelayUpdateChecker koronaRelayUpdateChecker, boolean forceCheck) {
      if (koronaRelayUpdateChecker == null) {
         return;
      }
      if (isCheckingForKoronaUpdates) {
         return;
      }
      try (KoronaRelayUpdateChecker.UpdateLoader updateLoader = koronaRelayUpdateChecker.createUpdateLoader(forceCheck)) {
         isCheckingForKoronaUpdates = true;

         List<KoronaRelayUpdate> updates = updateLoader.getUpdates();
         if (updates.isEmpty()) {
            return;
         }

         Set<Path> destinationFiles = updates.stream()
               .map(KoronaRelayUpdate::getDestinationFile)
               .collect(Collectors.toSet());
         DataType dataType = koronaRelayUpdateChecker == rawUpdateChecker ? DataType.RAW : DataType.PROCESSED;
         boolean fileOpen = dataSetLoader.getDataFileSet(dataType).getDataFiles().stream()
               .flatMap(dataFile -> dataFile.getSegmentHandle().getFiles().stream())
               .anyMatch(destinationFiles::contains);

         int a = JOptionPane.showConfirmDialog(getReferenceComponent(),
               "There are new KORONA processed files available in\n" +
                     koronaRelayUpdateChecker.getDirectory() +
                     "\n\nDo you want to use these files now?",
               "File update",
               JOptionPane.YES_NO_OPTION,
               JOptionPane.QUESTION_MESSAGE);
         if (a == JOptionPane.YES_OPTION) {
            if (fileOpen && !getLSSS().getSurveyManager().isWorkUnmodifiedOrUserApproved(getPlugin())) {
               //do nothing, user pressed cancel when asked if survey should be saved.
            } else {
               String firstFile = firstSelectedFile.getValue();
               String lastFile = lastSelectedFile.getValue();

               selectNothing();

               rawBackgroundAsyncHandle.waitUntilFinished();
               processedBackgroundAsyncHandle.waitUntilFinished();
               dataSetLoader.waitUntilFinished();

               if (fileOpen) {
                  installNewDataFiles(dataSetLoader, dataType);
               }

               ProgressView progressView = new ProgressView(LsssUtils.infoText("Moving", getPlugin(), "processed files"), updates.size())
                     .showRemainingTime();
               new WorkerDialog(getReferenceComponent(), progressView.getComponent())
                     .start(asyncHandle -> {
                        for (KoronaRelayUpdate update : updates) {
                           progressView.incrementMainProgress("");
                           if (asyncHandle.isCancelled()) {
                              return;
                           }
                           try {
                              update.move();
                           } catch (IOException e) {
                              updateLoader.saveStatus();
                              int answer = GuiUtils.getNowOrWait(() -> {
                                 return JOptionPane.showConfirmDialog(getReferenceComponent(), "Error: " + e.getMessage()
                                             + "\nDo you want to try to copy the rest of the files? ",
                                       "Error copying processed files", JOptionPane.YES_NO_OPTION);
                              });
                              if (answer != JOptionPane.YES_OPTION) {
                                 break;
                              }
                           }
                        }
                     });

               view.dataFileTableModel.update();
               selectFiles(firstFile, lastFile);
            }
         }
      } catch (IOException e) {
         if (GuiUtils.fileExists(koronaRelayUpdateChecker.getStatusFile(), getLSSS().getReferenceComponent())) {
            SwingUtilities.invokeLater(() -> {
               KoronaRelayUtils.showStatusFileErrorDialog(getReferenceComponent(), e, koronaRelayUpdateChecker.getStatusFile());
            });
         }
      } finally {
         isCheckingForKoronaUpdates = false;
      }
   }

   private void checkForUpdatesFromCalibrationXml(boolean forceCheck) {
      if (!forceCheck && forceReloadOnApply) {
         return;
      }
      checkForUpdatesFromCalibrationXml(getRawDir().getFile(), DataType.RAW, forceCheck);
      if (getProcessedDir() != null) {
         checkForUpdatesFromCalibrationXml(getProcessedDir().getFile(), DataType.PROCESSED, forceCheck);
      }
   }

   private void checkForUpdatesFromCalibrationXml(@Nullable Path dir, DataType dataType, boolean forceCheck) {
      if (dir == null) {
         return;
      }
      DataFileSet dataFileSet = dataSetLoader.getDataFileSet(dataType);
      if (dataFileSet.isEmpty()) {
         return;
      }
      CalibrationFile calibrationFile = dataFileSet.getRawFileConfiguration().getCalibrationFile();
      if (!dir.equals(calibrationFile.getDir())) {
         return;
      }
      if (!calibrationFile.isModified(forceCheck)) {
         return;
      }
      if (getLSSS().getInterpretationSettings().isInteractiveMode()) {
         int answer = GuiUtils.showOptionDialog(getLSSS().getReferenceComponent(), "Calibration updated",
               "Calibration is modified in\n" + dir + "\n",
               new String[]{"Apply changes", "Ignore"});
         if (answer != 0) {
            return;
         }
      }
      triggerReloadDataOnApply();
   }

   void cancelDataSetLoader() {
      dataSetLoader.cancel();
   }

   public void triggerReloadDataOnApply() {
      String firstFile = firstSelectedFile.getValue();
      String lastFile = lastSelectedFile.getValue();

      selectNothing();

      rawBackgroundAsyncHandle.waitUntilFinished();
      processedBackgroundAsyncHandle.waitUntilFinished();
      dataSetLoader.waitUntilFinished();

      selectFiles(firstFile, lastFile);

      forceReloadOnApply = true;
   }

   public FileListTransferHandler getDataFileTransferHandler() {
      return view.dataFileTransferHandler;
   }

   private void importFiles(List<Path> files) {
      if (files.size() == 1 && files.getFirst().toString().endsWith(SurveyManager.SURVEY_FILE_SUFFIX)) {
         if (getLSSS().getSurveyManager().isUnmodifiedOrUserApproved()) {
            getLSSS().getSurveyManager().open(files.getFirst());
         }
         return;
      }

      // Must do this later so that configuration manager can create a backup of the configuration first,
      // else cancel button doesn't work as it should
      SwingUtilities.invokeLater(() -> selectFiles(files));
      getConfigurationManager().showDialog(this);
   }

   private void updateParameterPanel() {
      GuiUtils.replaceContent(view.parameterPanel, createParameterEditor().getEditorComponent());
   }

   public void refresh() {
      checkForUpdates(true);
      reloadDataFileLabelling();
      updateParameterPanel();
      updateTableModel();
      forceReloadOnApply = true;
   }

   public void checkForUpdates(boolean forceCheck) {
      checkForUpdatesFromCalibrationXml(forceCheck);
      checkForUpdatesFromKoronaRelay(forceCheck);
   }

   protected void updateTableModel() {
      String firstFile = firstSelectedFile.getValue();
      String lastFile = lastSelectedFile.getValue();
      view.dataFileTableModel.update();
      SwingUtilities.invokeLater(() -> selectFiles(firstFile, lastFile));
   }

   void dataFileTableModelUpdated() {
      view.updateFilePanel();
      view.createIdxButton.setVisible(false);
      view.createBotButton.setVisible(false);
      view.updateCalibrateButton();
      fileTableChangeManager.notifyListeners();
   }

   void foundMissingIdx() {
      SwingUtilities.invokeLater(() -> view.createIdxButton.setVisible(true));
   }

   boolean useMissingBot() {
      return false;
   }

   void foundMissingBotWithXyz() {
      if (useMissingBot()) {
         SwingDelayer.invokeLater(view.createBotButton, () -> view.createBotButton.setVisible(true));
      }
   }

   @Override
   public void fromConfigurationXml(Element configurationElement) {
      doingFromXml = true;
      try {
         super.fromConfigurationXml(configurationElement);
      } finally {
         doingFromXml = false;
      }
      updateTableSelection();
      TableUtils.scrollToSelectedRows(view.dataFileTable);
   }

   void updateTableSelection() {
      selectFiles(firstSelectedFile.getValue(), lastSelectedFile.getValue());
   }

   private void selectFiles(String firstFile, String lastFile) {
      firstSelectedFile.setValue(firstFile);
      lastSelectedFile.setValue(lastFile);

      view.updateTableSelection(firstFile, lastFile);

      updateSelectedRows();
   }

   private void updateSelectedRows() {
      updateSelectedRawRows();
      updateSelectedProcessedRows();
   }

   private @Nullable SegmentInfo getSegmentInfo(int beginIndex, int lastIndex) {
      int delta = lastIndex > beginIndex ? 1 : -1;
      int end = lastIndex + delta;
      List<DataFileTableModel.FileRow> fileRows = view.dataFileTableModel.getFileRows();
      for (int i = beginIndex; i != end; i += delta) {
         if (i < 0 || i >= fileRows.size()) {
            return null;
         }
         DataFileTableModel.FileRow fileRow = fileRows.get(i);
         SegmentInfo segmentInfo = fileRow.getSegmentInfo();
         if (segmentInfo == null) {
            try {
               segmentInfo = fileRow.getRawSegmentHandle().createSegmentInfo();
            } catch (IOException e) {
               // Ignore and try next row instead
               continue;
            }
         }
         if (!segmentInfo.getPingRange().isEmpty()) {
            return segmentInfo;
         }
      }
      return null;
   }

   public Range<Long> getSelectedNTDateRange() {
      IntRange indexes = getSelectedFileRowIndexes();

      if (indexes.isEmpty()) {
         return new DefaultRange<>(0L, 0L);
      }

      SegmentInfo firstSegmentInfo = getSegmentInfo(indexes.begin(), indexes.end() - 1);
      SegmentInfo lastSegmentInfo = getSegmentInfo(indexes.end() - 1, indexes.begin());

      if (firstSegmentInfo == null || lastSegmentInfo == null) {
         return new DefaultRange<>(0L, 0L);
      }

      long beginNTDate = firstSegmentInfo.getPingRange().begin().getNTDate();
      long endNTDate = lastSegmentInfo.getPingRange().end().getNTDate();
      if (beginNTDate > endNTDate) {
         return new DefaultRange<>(0L, 0L);
      }

      return new DefaultRange<>(beginNTDate, endNTDate);
   }

   protected boolean getCheckCompatibility() {
      return true;
   }

   public boolean useConfigurableOnTheFlyProcessing() {
      return false;
   }

   public FileOpenRequest.@Nullable OnTheFlyProcessing getBuiltInOnTheFlyProcessing() {
      return null;
   }

   private void updateSelectedRawRows() {
      dataSetLoader.getRawAsyncHandle().cancel();
      List<SegmentHandle> selectedOriginalSegmentHandles = getSelectedOriginalSegmentHandles();
      rawBackgroundAsyncHandle.cancel();
      rawBackgroundAsyncHandle = new AsyncHandle();
      rawBackgroundExecutor.execute(rawBackgroundAsyncHandle.createManagedRunnable(() -> {
         dataSetLoader.getRawAsyncHandle().cancel();
         dataSetLoader.getRawAsyncHandle().waitUntilFinished();

         DataFileTableModel.FileOpenRegistration fileOpenRegistration = view.dataFileTableModel.newRegisterLater(DataType.RAW);

         FileOpenRequest fileOpenRequest = new FileOpenRequest(selectedOriginalSegmentHandles, getCheckCompatibility(), new FileOpenRequest.RequestObserver() {
            @Override
            public void progressReportAfterOpen(SegmentHandle segmentHandle, @Nullable DataFile dataFile) {
               fileOpenRegistration.dataFile(segmentHandle, dataFile);
               dataSetLoader.getRawCounter().incrementAndGet();
               autoScrollLater(fileOpenRegistration);
            }

            @Override
            public void handleDataException(SegmentHandle segmentHandle, DataException dataException) {
               fileOpenRegistration.dataException(segmentHandle, dataException);
            }

            @Override
            public void handleIncompatibleDataFile(SegmentHandle segmentHandle, String incompatibilityReason) {
               fileOpenRegistration.dataFileIncompatibility(segmentHandle, incompatibilityReason);
            }
         });
         if (useConfigurableOnTheFlyProcessing()) {
            OnTheFlySetup onTheFlySetup = getPreprocessingConf().getOnTheFlySetup();
            if (onTheFlySetup != null) {
               ModuleContainer moduleContainer = onTheFlySetup.createModuleContainer();
               if (moduleContainer != null) {
                  List<ConcurrentPingModule> applicableModules = ProcessingSegmentData.getApplicableModules(moduleContainer);
                  if (!applicableModules.isEmpty()) {
                     moduleContainer.clear();
                     applicableModules.forEach(moduleContainer::addModule);
                     fileOpenRequest.setOnTheFlyProcessing((segmentData, segmentHandle) -> {
                        boolean notProcessed = segmentData.getPingConfiguration().getConfigurationItem(Cds0Datagram.class) == null;
                        if (notProcessed || onTheFlySetup.evenIfProcessed.getBooleanValue()) {
                           return new ProcessingSegmentData(segmentHandle, segmentData, moduleContainer);
                        }
                        return segmentData;
                     });
                  }
               }
            }
         }
         FileOpenRequest.OnTheFlyProcessing builtInOnTheFlyProcessing = getBuiltInOnTheFlyProcessing();
         if (builtInOnTheFlyProcessing != null) {
            fileOpenRequest.setOnTheFlyProcessing(fileOpenRequest.getOnTheFlyProcessing().andThen(builtInOnTheFlyProcessing));
         }

         dataSetLoader.asyncOpenFiles(DataType.RAW, fileOpenRequest);

         ntDateSelectionChangeManager.notifyListeners(getSelectedNTDateRange());
      }));
   }

   /**
    * Gets the selected files and performs some tasks when the file is loaded:
    * sets a DataFile object in the correct row
    * updates the KORONA button
    * checks for exceptions
    * indicates incompatible files.
    */
   void updateSelectedProcessedRows() {
      dataSetLoader.getProcessedAsyncHandle().cancel();
      List<SegmentHandle> selectedKoronaSegmentHandles = getSelectedKoronaSegmentHandles();
      processedBackgroundAsyncHandle.cancel();
      processedBackgroundAsyncHandle = new AsyncHandle();
      processedBackgroundExecutor.execute(processedBackgroundAsyncHandle.createManagedRunnable(() -> {
         dataSetLoader.getProcessedAsyncHandle().cancel();
         dataSetLoader.getProcessedAsyncHandle().waitUntilFinished();

         DataFileTableModel.FileOpenRegistration fileOpenRegistration = view.dataFileTableModel.newRegisterLater(DataType.PROCESSED);

         FileOpenRequest fileOpenRequest = new FileOpenRequest(selectedKoronaSegmentHandles, getCheckCompatibility(), new FileOpenRequest.RequestObserver() {
            @Override
            public void progressReportAfterOpen(SegmentHandle segmentHandle, @Nullable DataFile dataFile) {
               fileOpenRegistration.dataFile(segmentHandle, dataFile);
               dataSetLoader.getProcessedCounter().incrementAndGet();
               if (dataSetLoader.getRawAsyncHandle().isFinished()) {
                  // Only scroll here if original data is done scrolling, otherwise the scrolling could end up
                  // jumping back and forth if original and processed data loading comes too much out of sync.
                  autoScrollLater(fileOpenRegistration);
               }
            }

            @Override
            public void handleDataException(SegmentHandle segmentHandle, DataException dataException) {
               fileOpenRegistration.dataException(segmentHandle, dataException);
            }

            @Override
            public void handleIncompatibleDataFile(SegmentHandle segmentHandle, String incompatibilityReason) {
               fileOpenRegistration.dataFileIncompatibility(segmentHandle, incompatibilityReason);
            }
         });
         FileOpenRequest.OnTheFlyProcessing builtInOnTheFlyProcessing = getBuiltInOnTheFlyProcessing();
         if (builtInOnTheFlyProcessing != null) {
            fileOpenRequest.setOnTheFlyProcessing(fileOpenRequest.getOnTheFlyProcessing().andThen(builtInOnTheFlyProcessing));
         }

         dataSetLoader.asyncOpenFiles(DataType.PROCESSED, fileOpenRequest);
      }));
   }

   private void autoScrollLater(DataFileTableModel.FileOpenRegistration fileOpenRegistration) {
      if (!dataFileTableAutoScroll) {
         return;
      }
      SwingDelayer.invokeLater(fileOpenRegistration, () -> {
         int fileRowIndex = fileOpenRegistration.getLastFileRowIndex();
         if (fileRowIndex != -1) {
            int i = view.dataFileTableModel.fileRowIndexToTableRowIndex(fileRowIndex);
            TableUtils.scrollToRows(view.dataFileTable, i, i);
         }
      });
   }

   private DataFileSet.Compatibility getSelectionCompatibility() {
      DataFileSet rawDataFileSet = dataSetLoader.getDataFileSet(DataType.RAW);
      DataFileSet processedDataFileSet = dataSetLoader.getDataFileSet(DataType.PROCESSED);
      return rawDataFileSet.isCompatibleWith(processedDataFileSet);
   }

   public abstract SurveyDirectoryParameter getRawDir();

   public abstract @Nullable SurveyDirectoryParameter getProcessedDir();

   public abstract List<SurveyDirectoryParameter> getAllPreprocessingTargetDirs();

   protected abstract void installNewDataFiles(DataSetLoader dataSetLoader, DataType dataType);

   protected abstract void updateSelectedDataFiles(DataType dataType);

   protected boolean canUpdateSelectedDataFilesWithIncompatiblePingConfiguration() {
      return false;
   }

   protected void updateSelectedDataFilesWithIncompatibility(DataType dataType) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean apply() {
      prepareApply();

      DataFileSet.Compatibility compatibility = getSelectionCompatibility();
      boolean selectionUsable = compatibility == DataFileSet.Compatibility.OK
            || compatibility == DataFileSet.Compatibility.INCOMPATIBLE_PING_CONFIGURATION && canUpdateSelectedDataFilesWithIncompatiblePingConfiguration();
      boolean hasKoronaFiles = !dataSetLoader.getDataFileSet(DataType.PROCESSED).isEmpty();
      if (!selectionUsable || !hasKoronaFiles) {
         view.koronaButton.setSelected(false);
      }
      DataType dataType = view.koronaButton.isSelected() ? DataType.PROCESSED : DataType.RAW;

      boolean dataSetChanged = isDataSetChanged();
      if (forceReloadOnApply || dataSetChanged) {
         forceReloadOnApply = false;

         if (getLSSS().getSurveyManager().isWorkUnmodifiedOrUserApproved(getPlugin())) {
            DataFileSet dataFileSet = dataSetLoader.getDataFileSet(dataSetManager.getSelectedDataType());
            if (dataSetChanged || !dataFileSet.isEmpty()) {
               PingRange totalRange = dataFileSet.getTotalRange();
               Log.global.info(LsssUtils.infoText("Opening " + dataFileSet.getDataFiles().size(), getPlugin(),
                     Utils.format("data files: %d pings, %.3f nmi, %s", totalRange.getPingCount(), totalRange.getVesselDistance(), totalRange.getDurationString())));
            }
            installNewDataFiles(dataSetLoader, dataType);
         } else {
            return false;
         }
      }

      updateKoronaButton(selectionUsable, compatibility, hasKoronaFiles);

      return true;
   }

   protected boolean isDataSetChanged() {
      return !isEqual(dataSetLoader.getDataFileSet(DataType.RAW), dataSetManager.getDataFileSet(DataType.RAW)) ||
            !isEqual(dataSetLoader.getDataFileSet(DataType.PROCESSED), dataSetManager.getDataFileSet(DataType.PROCESSED));
   }

   void setForceWorkReload() {
      forceReloadOnApply = true;
   }

   @Override
   public boolean prepareApply() {
      checkForUpdatesFromCalibrationXml(true);
      dataFileTableAutoScroll = true;
      int total = dataSetLoader.getRawSegmentHandles().size() + dataSetLoader.getProcessedSegmentHandles().size();
      ProgressView progressView = new ProgressView(LsssUtils.infoText("Reading", getPlugin(), "index files..."), total);
      WorkerDialog.Result result = new WorkerDialog(getLSSS().getReferenceComponent(), progressView.getComponent())
            .start(asyncHandle -> {
               while (!asyncHandle.isCancelled()) {
                  progressView.setMainProgress(dataSetLoader.getRawCounter().get() + dataSetLoader.getProcessedCounter().get(), "");
                  if (rawBackgroundAsyncHandle.isFinished()
                        && processedBackgroundAsyncHandle.isFinished()
                        && dataSetLoader.getRawAsyncHandle().isFinished()
                        && dataSetLoader.getProcessedAsyncHandle().isFinished()) {
                     return;
                  }
                  asyncHandle.sleep(10);
               }
            });
      dataFileTableAutoScroll = false;
      return result.success();
   }

   public static boolean isEqual(DataFileSet dataFileSetA, DataFileSet dataFileSetB) {
      List<DataFile> dataFilesA = dataFileSetA.getDataFiles();
      List<DataFile> dataFilesB = dataFileSetB.getDataFiles();

      if (dataFilesA.size() != dataFilesB.size()) {
         return false;
      }

      for (int i = 0; i < dataFilesA.size(); i++) {
         DataFile dataFileA = dataFilesA.get(i);
         DataFile dataFileB = dataFilesB.get(i);

         if (!dataFileA.getSegmentHandle().equals(dataFileB.getSegmentHandle())
               || !dataFileA.getPingRange().equals(dataFileB.getPingRange())) {
            return false;
         }
      }

      return true;
   }

   public JToggleButton getKoronaButton() {
      return view.koronaButton;
   }

   private void dataFileLabellingChanged() {
      viewHolder.ifView(view -> view.dataFileTable.getDataFileTableModel().fireTableDataChanged());
      saveDataFileLabelling();
   }

   private void saveDataFileLabelling() {
      Path file = getDataFileLabellingFile();
      if (file != null && dataFileLabelling != null) {
         dataFileLabelling.save(file);
      }
   }

   public @Nullable Path getDataFileLabellingFile() {
      WorkFileManager workFileManager = getPlugin().getWorkFileManager();
      if (workFileManager == null) {
         return null;
      }
      Path workDir = workFileManager.getWorkDir();
      if (workDir == null) {
         return null;
      }
      return workDir.resolve(workFileManager.getDataFileLabelsFileName());
   }

   private void reloadDataFileLabelling() {
      Path file = getDataFileLabellingFile();
      dataFileLabelling = file != null ? new DataFileLabelling(file) : null;
   }

   public @Nullable DataFileLabelling getDataFileLabelling() {
      return dataFileLabelling;
   }

   public abstract @Nullable LsssAction getStartPreprocessingAction();

   protected abstract SvgIcon getDataSetToggleButtonIcon();

   protected abstract String getDataSetToggleButtonTooltip();

   public void setSelectDataType(DataType dataType) {
      if (!view.koronaButton.isEnabled()) {
         return;
      }
      if (dataSetManager.getSelectedDataType() == dataType) {
         return;
      }
      view.koronaButton.setSelected(dataType == DataType.PROCESSED);
      switch (getSelectionCompatibility()) {
         case OK -> updateSelectedDataFiles(dataType);
         case INCOMPATIBLE_PING_CONFIGURATION -> updateSelectedDataFilesWithIncompatibility(dataType);
         case UNUSABLE -> throw new UnsupportedOperationException();
      }
   }

   protected void updateKoronaButton(boolean selectionUsable, DataFileSet.Compatibility compatibility, boolean hasKoronaFiles) {
      view.koronaButton.setEnabled(selectionUsable && hasKoronaFiles);
   }

   public List<DataFileTableModel.FileRow> getFileRows() {
      return view.dataFileTableModel.getFileRows();
   }

   public List<SegmentHandle> getAllOriginalSegmentHandles() {
      return view.dataFileTableModel.getSegmentHandles();
   }

   public List<SegmentInfo> getAllOriginalSegmentInfos() {
      List<SegmentInfo> segmentInfos = new ArrayList<>();
      for (DataFileTableModel.FileRow fileRow : view.dataFileTableModel.getFileRows()) {
         SegmentInfo segmentInfo = fileRow.getSegmentInfo();
         if (segmentInfo != null) {
            segmentInfos.add(segmentInfo);
         }
      }
      return segmentInfos;
   }

   public @Nullable SegmentHandle ntDateToOriginalSegmentHandle(long ntDate) {
      for (DataFileTableModel.FileRow fileRow : view.dataFileTableModel.getFileRows()) {
         SegmentInfo segmentInfo = fileRow.getSegmentInfo();
         if (segmentInfo != null && segmentInfo.getPingRange().containsNTDate(ntDate)) {
            return fileRow.getRawSegmentHandle();
         }
      }
      return null;
   }

   public List<SegmentHandle> getSelectedOriginalSegmentHandles() {
      return getSelectedSegmentHandles(DataType.RAW, true);
   }

   private List<SegmentHandle> getSelectedKoronaSegmentHandles() {
      return getSelectedSegmentHandles(DataType.PROCESSED, true);
   }

   private IntRange getSelectedFileRowIndexes() {
      int i0 = view.dataFileTableModel.getFileRowIndex(DataType.RAW, firstSelectedFile.getValue());
      int i1 = view.dataFileTableModel.getFileRowIndex(DataType.RAW, lastSelectedFile.getValue());
      if (i0 == -1 || i1 == -1) {
         return new IntRange(-1, -1);
      }
      return i0 <= i1 ? new IntRange(i0, i1 + 1) : new IntRange(i1, i0 + 1);
   }

   public List<SegmentHandle> getSelectedSegmentHandles(DataType dataType, boolean emptyListIfAnyMissing) {
      IntRange indexes = getSelectedFileRowIndexes();
      List<SegmentHandle> segmentHandles = new ArrayList<>(indexes.getSize());
      for (int i = indexes.begin(); i < indexes.end(); i++) {
         SegmentHandle segmentHandle = view.dataFileTableModel.getFileRows().get(i).getSegmentHandle(dataType);
         if (segmentHandle == null) {
            if (emptyListIfAnyMissing) {
               return List.of();
            }
         } else {
            segmentHandles.add(segmentHandle);
         }
      }
      return segmentHandles;
   }

   public void addAllConfigurationItems(Collection<PingItem> configurationItems) {
      for (DataFileTableModel.FileRow fileRow : view.dataFileTableModel.getFileRows()) {
         DataFile rawDataFile = fileRow.getDataFile(DataType.RAW);
         if (rawDataFile != null) {
            configurationItems.addAll(rawDataFile.getPingConfiguration().getConfigurationItems());
         }
         DataFile processedDataFile = fileRow.getDataFile(DataType.PROCESSED);
         if (processedDataFile != null) {
            configurationItems.addAll(processedDataFile.getPingConfiguration().getConfigurationItems());
         }
      }
   }

   protected DataFileTable getDataFileTable() {
      return view.dataFileTable;
   }

   public void selectNothing() {
      selectFiles("", "");
   }

   public void selectAll() {
      List<DataFileTableModel.FileRow> fileRows = view.dataFileTableModel.getFileRows();
      if (!fileRows.isEmpty()) {
         selectFiles(fileRows.getFirst().getRawSegmentHandle().getBaseName(), fileRows.getLast().getRawSegmentHandle().getBaseName());
      }
   }

   public void selectNTDateRange(Range<Long> ntDateRange) {
      String first = "";
      String last = "";

      List<DataFileTableModel.FileRow> fileRows = view.dataFileTableModel.getFileRows();
      for (int i = 0; i < fileRows.size(); i++) {
         SegmentInfo segmentInfo = getSegmentInfo(i, i);
         if (segmentInfo != null && segmentInfo.getPingRange().toNTDateRange().intersects(ntDateRange)) {
            String baseName = fileRows.get(i).getRawSegmentHandle().getBaseName();
            if (first.isEmpty()) {
               first = baseName;
            }
            last = baseName;
         }
      }

      selectFiles(first, last);
   }

   public void selectFiles(List<Path> files) {
      if (files.isEmpty()) {
         selectNothing();
      } else if (files.size() == 1 && Files.isDirectory(files.getFirst())) {
         getRawDir().setFile(files.getFirst());
      } else {
         Path firstFile = files.stream().min(Comparator.naturalOrder()).orElseThrow();
         Path lastFile = files.stream().max(Comparator.naturalOrder()).orElseThrow();
         getRawDir().setFile(firstFile.getParent());
         SegmentHandle first = view.dataFileTableModel.getSegmentHandle(DataType.RAW, firstFile);
         SegmentHandle last = view.dataFileTableModel.getSegmentHandle(DataType.RAW, lastFile);
         if (first == null || last == null) {
            selectNothing();
         } else {
            selectFiles(first.getBaseName(), last.getBaseName());
         }
      }
   }

   public void selectSegmentHandles(List<SegmentHandle> segmentHandles) {
      if (segmentHandles.isEmpty()) {
         selectNothing();
      } else {
         getRawDir().setFile(segmentHandles.getFirst().getMainFile().getParent());
         selectFiles(segmentHandles.getFirst().getBaseName(), segmentHandles.getLast().getBaseName());
      }
   }

   public void selectNextFiles() {
      IntRange indexes = getSelectedFileRowIndexes();
      int index = indexes.isEmpty() ? 0 : indexes.end();
      int count = indexes.isEmpty() ? 1 : indexes.getSize();
      List<DataFileTableModel.FileRow> fileRows = view.dataFileTableModel.getFileRows();
      if (index < fileRows.size()) {
         String first = fileRows.get(index).getRawSegmentHandle().getBaseName();
         String last = fileRows.get(Math.min(index + count - 1, fileRows.size() - 1)).getRawSegmentHandle().getBaseName();
         selectFiles(first, last);
      }
   }

   public List<FileSelection> getFileSelections() {
      List<FileSelection> fileSelections = new ArrayList<>();

      List<SegmentHandle> rawSegmentHandles = getSelectedSegmentHandles(DataType.RAW, false);
      fileSelections.add(new FileSelection(getRawDir(), rawSegmentHandles));

      SurveyDirectoryParameter processedDir = getProcessedDir();
      if (processedDir != null) {
         List<SegmentHandle> processedSegmentHandles = getSelectedSegmentHandles(DataType.PROCESSED, false);
         fileSelections.add(new FileSelection(processedDir, processedSegmentHandles));
      }

      return fileSelections;
   }

   public static final class FileSelection {
      private final SurveyDirectoryParameter dir;
      private final List<SegmentHandle> segmentHandles;

      public FileSelection(SurveyDirectoryParameter dir, List<SegmentHandle> segmentHandles) {
         this.dir = dir;
         this.segmentHandles = segmentHandles;
      }
   }

   private static final class View implements ViewHolder.View {
      private final DataConf dataConf;
      private final FileListTransferHandler dataFileTransferHandler;

      private final JPanel mainPanel = new JPanel(new BorderLayout());
      private final JPanel parameterPanel = new JPanel(new BorderLayout());

      private final JButton createIdxButton = new JButton("Create idx...");
      private final JButton createBotButton = new JButton("Create bot...");
      private final JButton calibrationButton = new JButton("Calibration...");
      private final JLabel calibrationWarning = new JLabel();

      private final JToggleButton koronaButton = new JToggleButton();

      private final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      private final JPanel filePanel = new JPanel(new BorderLayout());

      private final DataFileTableModel dataFileTableModel;
      private final DataFileTable dataFileTable;
      private boolean updatingTableSelection;

      private View(DataConf dataConf) {
         this.dataConf = dataConf;

         dataFileTransferHandler = new FileListTransferHandler(dataConf::importFiles);

         parameterPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), GuiUtils.DEFAULT_MARGIN));

         buttonPanel.add(makeButtonPanelMenu());

         JButton refreshButton = MiscIcons.REFRESH.on(new JButton());
         refreshButton.setToolTipText("Refresh file list");
         refreshButton.addActionListener(e -> dataConf.refresh());
         GuiUtils.setAccelerator(refreshButton, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
         buttonPanel.add(refreshButton);

         createIdxButton.setVisible(false);
         createIdxButton.setToolTipText("Create missing index files");
         createIdxButton.addActionListener(e -> {
            dataConf.onMissingIdxFileButtonPress(createIdxButton, dataConf.getAllOriginalSegmentHandles());
            dataConf.refresh();
         });
         buttonPanel.add(createIdxButton);

         createBotButton.setVisible(false);
         createBotButton.setToolTipText("Create missing .bot files from .xyz files");
         createBotButton.addActionListener(e -> {
            onMissingBotFileButtonPress(createBotButton, dataConf.getAllOriginalSegmentHandles());
            dataConf.refresh();
         });
         buttonPanel.add(createBotButton);

         calibrationButton.setVisible(false);
         calibrationButton.setToolTipText("Show menu with calibration functionality");
         GuiUtils.addPopupMenuToButton(calibrationButton, this::calibrationButtonPopupMenu);
         buttonPanel.add(calibrationButton);

         calibrationWarning.setVisible(false);
         calibrationWarning.setBorder(BorderFactory.createLineBorder(Color.RED, 5));

         koronaButton.setEnabled(false);
         dataConf.getDataSetToggleButtonIcon().on(koronaButton);
         koronaButton.setToolTipText(dataConf.getDataSetToggleButtonTooltip());
         koronaButton.addActionListener(e1 -> dataConf.setSelectDataType(koronaButton.isSelected() ? DataType.PROCESSED : DataType.RAW));

         JPanel eastButtons = new JPanel(new FlowLayout());
         JButton hideParameterPanelButton = MiscIcons.STEP_UP.on(new JButton());
         hideParameterPanelButton.setToolTipText("Hide directory parameters");
         hideParameterPanelButton.addActionListener(e -> {
            boolean visible = !parameterPanel.isVisible();
            parameterPanel.setVisible(visible);
            (visible ? MiscIcons.STEP_UP : MiscIcons.STEP_DOWN).on(hideParameterPanelButton);
            hideParameterPanelButton.setToolTipText(visible ? "Hide directory parameters" : "Show directory parameters");
         });
         eastButtons.add(hideParameterPanelButton);

         JPanel buttonPanelContainer = new JPanel(new BorderLayout());
         buttonPanelContainer.add(eastButtons, BorderLayout.EAST);
         buttonPanelContainer.add(buttonPanel);
         buttonPanelContainer.add(calibrationWarning, BorderLayout.SOUTH);

         JPanel upperPanel = new JPanel(new BorderLayout());
         upperPanel.add(parameterPanel);
         upperPanel.add(buttonPanelContainer, BorderLayout.SOUTH);

         mainPanel.add(upperPanel, BorderLayout.NORTH);
         mainPanel.add(filePanel);

         filePanel.setTransferHandler(dataFileTransferHandler);

         dataFileTableModel = new DataFileTableModel(dataConf);
         dataFileTable = new DataFileTable(dataFileTableModel);
         dataFileTable.setTransferHandler(dataFileTransferHandler);
         dataFileTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || dataFileTableModel.isFiringTableDataChanged() || updatingTableSelection) {
               return;
            }
            int i0 = dataFileTableModel.tableRowIndexToFileRowIndex(dataFileTable.getSelectionModel().getMinSelectionIndex(), true);
            int i1 = dataFileTableModel.tableRowIndexToFileRowIndex(dataFileTable.getSelectionModel().getMaxSelectionIndex(), false);
            String first = i0 == -1 ? "" : dataFileTableModel.getFileRows().get(i0).getRawSegmentHandle().getBaseName();
            String last = i1 == -1 ? "" : dataFileTableModel.getFileRows().get(i1).getRawSegmentHandle().getBaseName();
            dataConf.selectFiles(first, last);
         });

         dataFileTable.addMouseListener(new PopupMenuMouseListener(e -> createFileTablePopupMenu()));
         dataFileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
               ToolTipManagerState.DEFAULT.apply();
            }
         });
      }

      @Override
      public JComponent getComponent() {
         return mainPanel;
      }

      private void setup() {
         dataFileTableModel.setup();
         updateFilePanel();
      }

      private JMenuBar makeButtonPanelMenu() {
         JMenuBar menuBar = new JMenuBar();

         JMenu menu = menuBar.add(MiscIcons.MENU.on(new JMenu()));
         menu.setToolTipText("Additional settings and actions");

         GuiUtils.autoCreateContentMenu(menu, () -> {
            JMenuItem sortByTimeItem = MiscIcons.checkBox(dataConf.sortByTime.getBooleanValue()).on(menu.add("Sort files by time"));
            sortByTimeItem.addActionListener(e -> {
               String firstFile = dataConf.firstSelectedFile.getValue();
               String lastFile = dataConf.lastSelectedFile.getValue();
               dataConf.sortByTime.toggle();
               dataConf.selectFiles(firstFile, lastFile);
            });

            JMenu timeGroupingMenu = new JMenu("Time grouping");
            menu.add(timeGroupingMenu);
            timeGroupingMenu.setEnabled(dataConf.sortByTime.getBooleanValue());
            for (TimeGrouping timeGrouping : TimeGrouping.values()) {
               JMenuItem item = MiscIcons.check(dataConf.timeGrouping.getValue() == timeGrouping).on(timeGroupingMenu.add(timeGrouping.label));
               item.addActionListener(e -> dataConf.timeGrouping.setValue(timeGrouping));
            }

            JMenuItem collapseAllItem = MiscIcons.VERTICAL_COLLAPSE.on(menu.add("Collapse all dates"));
            collapseAllItem.setEnabled(dataConf.sortByTime.getBooleanValue());
            collapseAllItem.addActionListener(e -> dataFileTableModel.setAllExpanded(false));

            JMenuItem expandAllItem = MiscIcons.VERTICAL_EXPAND.on(menu.add("Expand all dates"));
            expandAllItem.setEnabled(dataConf.sortByTime.getBooleanValue());
            expandAllItem.addActionListener(e -> dataFileTableModel.setAllExpanded(true));

            menu.addSeparator();

            JMenuItem visualizerDialogItem = MiscIcons.SCATTER_PLOT.on(menu.add("Data file visualizer dialog"));
            visualizerDialogItem.addActionListener(e -> new DataFileVisualizerDialog(dataConf));
         });

         return menuBar;
      }

      private JPopupMenu createFileTablePopupMenu() {
         JPopupMenu popupMenu = new JPopupMenu();

         DataFileLabelling dataFileLabelling = dataConf.getDataFileLabelling();
         if (dataFileLabelling != null) {
            popupMenu.add(labelMenu(dataFileLabelling));
            popupMenu.addSeparator();
         }

         JMenuItem selectAllItem = popupMenu.add("Select all files");
         selectAllItem.addActionListener(e -> dataConf.selectAll());

         JMenuItem scrollToSelectionItem = popupMenu.add("Scroll to selected files");
         scrollToSelectionItem.setEnabled(!dataFileTable.getSelectionModel().isSelectionEmpty());
         scrollToSelectionItem.addActionListener(e -> TableUtils.scrollToSelectedRows(dataFileTable));

         for (DataConf otherDataConf : dataConf.getAllDataConfs()) {
            if (otherDataConf == dataConf) {
               continue;
            }
            popupMenu.addSeparator();

            SvgIcon icon = dataConf instanceof DataConfLSSS ? null : dataConf.getPlugin().getIcon();
            SvgIcon otherIcon = otherDataConf instanceof DataConfLSSS ? null : otherDataConf.getPlugin().getIcon();

            addSelectItem(popupMenu, otherDataConf, dataConf, icon != null ? icon : MiscIcons.EMPTY);
            addSelectItem(popupMenu, dataConf, otherDataConf, otherIcon != null ? otherIcon : MiscIcons.EMPTY);
         }

         List<FileSelection> fileSelections = dataConf.getFileSelections();
         if (!fileSelections.isEmpty()) {
            popupMenu.addSeparator();

            for (FileSelection fileSelection : fileSelections) {
               JMenuItem showPreprocessingSetupItem = popupMenu.add("Show processing setup of selected files in " + fileSelection.dir.getDisplayName());
               showPreprocessingSetupItem.setEnabled(!fileSelection.segmentHandles.isEmpty());
               Path dir = fileSelection.dir.getFile();
               if (dir != null) {
                  showPreprocessingSetupItem.addActionListener(e -> {
                     new ProcessingSetupDialog(dataConf.getLSSS(), dir, fileSelection.segmentHandles, dataConf.getLSSS().getReferenceComponent());
                  });
               }
            }
         }

         return popupMenu;
      }

      private JMenu labelMenu(DataFileLabelling dataFileLabelling) {
         JMenu menu = MiscIcons.LABEL.on(new JMenu("Data file labels"));
         menu.setMnemonic(KeyEvent.VK_L);
         menu.setDisplayedMnemonicIndex(10);

         Collection<DataFileLabel> allLabels = Utils.sorted(dataFileLabelling.getAllLabels());
         List<SegmentHandle> selectedSegmentHandles = dataConf.getSelectedSegmentHandles(DataType.RAW, false);
         Set<DataFileLabel> selectedLabels = selectedSegmentHandles.stream()
               .flatMap(file -> dataFileLabelling.getLabels(file).stream())
               .collect(Collectors.toCollection(TreeSet::new));

         JMenu addMenu = MiscIcons.ADD.on(MenuUtils.addMenu(menu, "Add label to selected", KeyEvent.VK_A));
         JMenu removeMenu = MiscIcons.DELETE.on(MenuUtils.addMenu(menu, "Remove label from selected", KeyEvent.VK_R));
         JMenu selectMenu = MenuUtils.addMenu(menu, "Select by label", KeyEvent.VK_S);
         menu.addSeparator();
         JMenu editMenu = MiscIcons.EDIT.on(MenuUtils.addMenu(menu, "Edit label", KeyEvent.VK_E));
         JMenu deleteMenu = MiscIcons.DELETE.on(MenuUtils.addMenu(menu, "Delete label", KeyEvent.VK_R));

         addMenu.setEnabled(!selectedSegmentHandles.isEmpty());
         removeMenu.setEnabled(!selectedLabels.isEmpty());
         selectMenu.setEnabled(!allLabels.isEmpty());
         editMenu.setEnabled(!allLabels.isEmpty());
         deleteMenu.setEnabled(!allLabels.isEmpty());

         if (!allLabels.isEmpty()) {
            for (DataFileLabel label : allLabels) {
               addMenu.add(DataFileLabelUtils.menuItem(label, e -> {
                  dataFileLabelling.addLabel(label, selectedSegmentHandles);
                  dataConf.dataFileLabellingChanged();
               }));
            }
            addMenu.addSeparator();
         }
         MenuUtils.addItem(addMenu, "New label...", KeyEvent.VK_N, e -> {
            DataFileLabel emptyLabel = new DataFileLabel("", "", DataFileLabelling.DEFAULT_COLOR);
            DataFileLabel newLabel = new DataFileLabelEditor(dataFileLabelling, emptyLabel)
                  .show(mainPanel, "New label")
                  .getEditedLabel();
            if (newLabel != null) {
               dataFileLabelling.addNewLabel(newLabel);
               dataFileLabelling.addLabel(newLabel, selectedSegmentHandles);
               dataConf.dataFileLabellingChanged();
            }
         });

         for (DataFileLabel label : allLabels) {
            selectMenu.add(DataFileLabelUtils.menuItem(label, e -> {
               List<SegmentHandle> segmentHandles = dataFileTableModel.getFileRows().stream()
                     .map(DataFileTableModel.FileRow::getRawSegmentHandle)
                     .filter(segmentHandle -> dataFileLabelling.getLabels(segmentHandle).contains(label))
                     .toList();
               dataConf.selectSegmentHandles(segmentHandles);
            }));
         }

         for (DataFileLabel label : selectedLabels) {
            removeMenu.add(DataFileLabelUtils.menuItem(label, e -> {
               dataFileLabelling.removeLabel(label, selectedSegmentHandles);
               dataConf.dataFileLabellingChanged();
            }));
         }
         removeMenu.addSeparator();
         MenuUtils.addItem(removeMenu, "All labels", KeyEvent.VK_A, e -> {
            dataFileLabelling.removeAllLabels(selectedSegmentHandles);
            dataConf.dataFileLabellingChanged();
         });

         for (DataFileLabel label : allLabels) {
            editMenu.add(DataFileLabelUtils.menuItem(label, e -> {
               DataFileLabel editedLabel = new DataFileLabelEditor(dataFileLabelling, label)
                     .show(mainPanel, "Edit label")
                     .getEditedLabel();
               if (editedLabel != null) {
                  dataFileLabelling.replaceLabel(label, editedLabel);
                  dataConf.dataFileLabellingChanged();
               }
            }));
         }

         for (DataFileLabel label : allLabels) {
            deleteMenu.add(DataFileLabelUtils.menuItem(label, e -> {
               int answer = GuiUtils.showOptionDialog(mainPanel, "Delete label",
                     "<html>Delete label " + label.toHtml() + "?", new String[]{"Delete", "Cancel"});
               if (answer == 0) {
                  dataFileLabelling.replaceLabel(label, null);
                  dataConf.dataFileLabellingChanged();
               }
            }));
         }

         return menu;
      }

      private static void addSelectItem(JPopupMenu popupMenu, DataConf sourceDataConf, DataConf targetDataConf, SvgIcon icon) {
         JMenuItem item = icon.on(popupMenu.add("Select '" + targetDataConf.getDisplayName() + "' files from '" + sourceDataConf.getDisplayName() + "' selection"));
         item.addActionListener(e -> {
            targetDataConf.selectNTDateRange(sourceDataConf.getSelectedNTDateRange());
            TableUtils.scrollToSelectedRows(targetDataConf.view.dataFileTable);
         });
      }

      private void updateCalibrateButton() {
         Path dir = dataConf.getRawDir().getFile();
         if (dir == null) {
            calibrationButton.setVisible(false);
            calibrationWarning.setVisible(false);
            return;
         }
         Exec.CACHED_THREAD_POOL.execute(() -> {
            CalibrationFile calibrationFile = CalibrationFile.forDirectory(dir);

            SwingUtilities.invokeLater(() -> {
               calibrationButton.setVisible(true);
               calibrationButton.setBackground(null);
               calibrationWarning.setText(null);
               calibrationWarning.setVisible(false);
               if (calibrationFile.exists()) {
                  String error = calibrationFile.getError();
                  if (error != null) {
                     calibrationWarning.setText("Error in " + CalibrationFile.FILE_NAME + ": " + error);
                     calibrationWarning.setVisible(true);
                  } else {
                     calibrationButton.setBackground(ColorUtils.LIGHTGREEN);
                  }
               }
            });
         });
      }

      private void calibrationButtonPopupMenu(JPopupMenu popupMenu) {
         Path rawDir = dataConf.getRawDir().getFile();
         if (rawDir == null) {
            popupMenu.add("Data directory not configured").setEnabled(false);
            return;
         }

         List<SegmentHandle> selectedSegmentHandles = dataConf.getSelectedOriginalSegmentHandles();
         List<SegmentHandle> allSegmentHandles = dataConf.getAllOriginalSegmentHandles();

         Path calibrationXmlFile = rawDir.resolve(CalibrationFile.FILE_NAME);
         if (Files.exists(calibrationXmlFile)) {
            JMenuItem editItem = MiscIcons.EDIT.on(popupMenu.add("Edit " + CalibrationFile.FILE_NAME));
            editItem.setToolTipText("Edit " + calibrationXmlFile);
            editItem.addActionListener(e -> CalibrationGui.edit(rawDir, mainPanel));

            JMenuItem generateFromSelectedItem = popupMenu.add("Extend " + CalibrationFile.FILE_NAME
                  + " with values from " + selectedSegmentHandles.size() + " selected data files");
            generateFromSelectedItem.setToolTipText("<html>Extend " + HtmlEscapers.htmlEscaper().escape(calibrationXmlFile.toString())
                  + "<br>Existing entries will not be changed.");
            generateFromSelectedItem.addActionListener(e -> CalibrationGui.generate(selectedSegmentHandles, mainPanel));

            JMenuItem generateFromAllItem = popupMenu.add("Extend " + CalibrationFile.FILE_NAME
                  + " with values from all " + allSegmentHandles.size() + " data files");
            generateFromAllItem.setToolTipText("<html>Extend " + HtmlEscapers.htmlEscaper().escape(calibrationXmlFile.toString())
                  + "<br>Existing entries will not be changed.");
            generateFromAllItem.addActionListener(e -> CalibrationGui.generate(allSegmentHandles, mainPanel));
         } else {
            JMenuItem createEmptyItem = MiscIcons.ADD.on(popupMenu.add("Create empty " + CalibrationFile.FILE_NAME));
            createEmptyItem.setToolTipText("Create " + calibrationXmlFile);
            createEmptyItem.addActionListener(e -> CalibrationGui.createEmptyOrEdit(rawDir, mainPanel));

            JMenuItem generateFromSelectedItem = popupMenu.add("Generate " + CalibrationFile.FILE_NAME
                  + " with values from " + selectedSegmentHandles.size() + " selected data files");
            generateFromSelectedItem.setToolTipText("Create " + calibrationXmlFile);
            generateFromSelectedItem.addActionListener(e -> CalibrationGui.generate(selectedSegmentHandles, mainPanel));

            JMenuItem generateFromAllItem = popupMenu.add("Generate " + CalibrationFile.FILE_NAME
                  + " with values from all " + allSegmentHandles.size() + " data files");
            generateFromAllItem.setToolTipText("Create " + calibrationXmlFile);
            generateFromAllItem.addActionListener(e -> CalibrationGui.generate(allSegmentHandles, mainPanel));
         }
      }

      private void updateFilePanel() {
         JComponent content = dataFileTableModel.getRowCount() == 0
               ? new JLabel("<html>" + noDataHtmlText(), JLabel.CENTER)
               : dataFileTable;
         GuiUtils.replaceContent(filePanel, new JScrollPane(content));
      }

      private String noDataHtmlText() {
         String dir = HtmlEscapers.htmlEscaper().escape(String.valueOf(dataConf.getRawDir().getFile()));
         return switch (dataFileTableModel.getRawDirListingStatus()) {
            case OK -> "Directory contains no data files:<br><br>" + dir;
            case NOT_SPECIFIED -> "Please select data directory!";
            case NOT_EXISTING -> "Directory does not exist:<br><br>" + dir;
            case NOT_DIRECTORY -> "Not a directory:<br><br>" + dir;
            case ERROR -> "Error reading directory:<br><br>" + dir;
            case CANCELLED -> "Directory listing was cancelled:<br><br>" + dir;
         };
      }

      private void updateTableSelection(String firstBaseName, String lastBaseName) {
         int firstIndex = dataFileTableModel.fileRowIndexToTableRowIndex(dataFileTableModel.getFileRowIndex(DataType.RAW, firstBaseName));
         int lastIndex = dataFileTableModel.fileRowIndexToTableRowIndex(dataFileTableModel.getFileRowIndex(DataType.RAW, lastBaseName));

         updatingTableSelection = true;
         if (firstIndex != -1 && lastIndex != -1) {
            if (dataFileTable.getSelectionModel().getMinSelectionIndex() != firstIndex
                  || dataFileTable.getSelectionModel().getMaxSelectionIndex() != lastIndex) {
               dataFileTable.getSelectionModel().setSelectionInterval(firstIndex, lastIndex);
            }
         } else {
            dataFileTable.getSelectionModel().clearSelection();
         }
         updatingTableSelection = false;
      }
   }
}
