package no.imr.korona.viewer;

import no.imr.korona.Korona;
import no.imr.korona.computation.BaseModule;
import no.imr.korona.computation.BaseModuleComputation;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.computation.ModuleEditor;
import no.imr.korona.computation.ModuleException;
import no.imr.korona.computation.display.DisplayModule;
import no.imr.korona.computation.display.PlayboxModule;
import no.imr.korona.computation.display.VisualizerModule;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.util.PingReaderFactory;
import no.imr.korona.plugins.ModulePlugin;
import no.imr.korona.util.KoronaPreferences;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.tools.Utils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.swing.FileListTransferHandler;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MultiSplitPane;
import no.imr.tools.swing.Toast;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerListModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.prefs.Preferences;

/**
 * Main class for display.
 */
public final class KoronaPlaybox {
   private static final String PREFERENCE_LAST_RAW_FILE = "lastRawFile";
   private static final String PREFERENCE_LAST_CDS_FILE = "lastCdsFile";
   private static final String PREFERENCE_LAST_CFS_FILE = "lastCfsFile";

   private static final Dimension BUTTON_SIZE = new Dimension(24, 24);

   private final Korona korona;
   private final ConfigFileSettings configFileSettings;
   private final ModuleContainer moduleContainer;
   private final ColorConverterContainer converterContainer = new ColorConverterContainer();

   private @Nullable Path rawFile;

   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final JButton playButton = new JButton();
   private final JCheckBox fullSpeedCheckBox = new JCheckBox("Full speed");
   private final JButton rewindButton = MiscIcons.STEP_BACK.on(new JButton());
   private final JCheckBox autoRewindCheckBox = new JCheckBox("Auto rewind");
   private final JTextField surveyText = new JTextField();
   private final JTextField logTime = new JTextField(13);
   private final DateTimeFormatter logTimeFormat = Utils.createUTCDateTimeFormatter("yyyy.MM.dd HH:mm:ss");

   private final JSpinner realtimeFactorSpinner = new JSpinner(new SpinnerListModel(
         List.of(1, 2, 5, 10, 20, 50, 100, 200, 500, 1000)));

   private boolean fullSpeed = false;
   private int realtimeFactor = 20;
   private @Nullable KoronaPlayboxProcessing processing;

   private List<VisualizerModule> visualizerModules = List.of();
   private List<PlayboxModule> playboxModules = List.of();
   private final JPanel displayPanel = new JPanel(new BorderLayout());

   private DefaultVisualizerModule defaultVisualizerModule = new DefaultVisualizerModule(null, null);

   private final ChangeManager updateChangeManager = new ChangeManager();

   public KoronaPlaybox(Korona korona) {
      this.korona = korona;
      moduleContainer = new ModuleContainer(korona);
      configFileSettings = moduleContainer.getConfigFileSettings();

      SvColorPanel svColorPanel = new SvColorPanel(converterContainer);
      svColorPanel.setUseAdvancedDialog(true);

      getCdsFileParameter().subscribe(__ -> {
         Path cdsFile = getCdsFile();
         if (cdsFile != null) {
            setLastCdsFile(cdsFile);
         }
         update();
      });

      autoRewindCheckBox.setToolTipText("Auto rewind");
      GuiUtils.setAccelerator(autoRewindCheckBox, KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK));

      realtimeFactorSpinner.setToolTipText("Play speed");
      realtimeFactorSpinner.setPreferredSize(new Dimension(50, 24));
      realtimeFactorSpinner.setValue(realtimeFactor);
      realtimeFactorSpinner.addChangeListener(e -> realtimeFactor = (Integer) realtimeFactorSpinner.getValue());

      rewindButton.addActionListener(e -> rewind());
      rewindButton.setPreferredSize(BUTTON_SIZE);
      rewindButton.setToolTipText("Rewind");
      GuiUtils.setAccelerator(rewindButton, KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));

      MiscIcons.PLAY.on(playButton);
      playButton.setPreferredSize(BUTTON_SIZE);
      playButton.setToolTipText("Play");
      GuiUtils.setAccelerator(playButton, KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK));
      playButton.addActionListener(e -> {
         if (processing != null && processing.displayRunner().isRunning()) {
            stop();
         } else {
            start();
         }
      });

      fullSpeedCheckBox.setToolTipText("Do processing as fast as possible");
      GuiUtils.setAccelerator(fullSpeedCheckBox, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
      fullSpeedCheckBox.addActionListener(e -> {
         fullSpeed = fullSpeedCheckBox.isSelected();
         realtimeFactorSpinner.setEnabled(!fullSpeed);
      });

      surveyText.setToolTipText("Survey name");
      surveyText.setEditable(false);

      logTime.setToolTipText("Time");
      logTime.setEditable(false);

      mainPanel.add(displayPanel);
      mainPanel.add(svColorPanel.getComponent(), BorderLayout.WEST);
      mainPanel.setTransferHandler(new FileListTransferHandler(this::importFiles));

      update();
   }

   public Korona getKorona() {
      return korona;
   }

   public ConfigFileSettings getConfigFileSettings() {
      return configFileSettings;
   }

   private FileParameter getCdsFileParameter() {
      return configFileSettings.getModuleConfigurationFileParameter();
   }

   private @Nullable Path getCdsFile() {
      return getCdsFileParameter().getFile();
   }

   private void setCdsFile(Path cdsFile) {
      getCdsFileParameter().setFile(cdsFile);
   }

   public @Nullable Path getRawFile() {
      return rawFile;
   }

   public ColorConverterContainer getColorConverterContainer() {
      return converterContainer;
   }

   public ModuleContainer getModuleContainer() {
      return moduleContainer;
   }

   @Nullable KoronaPlayboxProcessing getProcessing() {
      return processing;
   }

   public @Nullable ModuleContainerComputation getModuleContainerComputation() {
      return processing != null ? processing.computation() : null;
   }

   public JComponent getComponent() {
      return mainPanel;
   }

   public GridBag createButtonsGridBag() {
      GridBag gridBag = new GridBag();
      gridBag.getConstraints().insets = new Insets(3, 0, 3, 6);
      gridBag.add(Box.createHorizontalStrut(0));
      gridBag.activateHorizontalFill();
      gridBag.add(surveyText);
      gridBag.deactivateFill();
      gridBag.add(playButton);
      gridBag.add(realtimeFactorSpinner);
      gridBag.add(fullSpeedCheckBox);
      gridBag.add(rewindButton);
      gridBag.add(autoRewindCheckBox);
      gridBag.add(logTime);
      return gridBag;
   }

   private void closeComputation() {
      try {
         if (processing != null) {
            processing.close();
            processing = null;
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error closing computation", e);
      }
   }

   public void reset() {
      closeComputation();
      surveyText.setText("");
      logTime.setText("");

      ModuleContainerComputation computation = createComputation();

      List<PlayboxModule> newPlayboxModules = new ArrayList<>();
      List<VisualizerModule> newVisualizerModules = new ArrayList<>();
      for (BaseModule module : moduleContainer.getModules()) {
         if (module.active.getBooleanValue()) {
            if (module instanceof PlayboxModule playboxModule) {
               newPlayboxModules.add(playboxModule);
               if (playboxModule instanceof VisualizerModule visualizerModule) {
                  newVisualizerModules.add(visualizerModule);
               }
            }
         }
      }

      if (computation != null) {
         try {
            PingSource pingSource;
            if (newVisualizerModules.isEmpty()) {
               VisualizerModule visualizerModule = getDefaultVisualizerModule(computation.getPingConfiguration());
               newPlayboxModules.add(visualizerModule);
               newVisualizerModules.add(visualizerModule);
               visualizerModule.setModuleContainer(moduleContainer);
               BaseModuleComputation visualizerComputation = visualizerModule.doConfigure(computation.getComputationContext(), computation);
               pingSource = visualizerComputation != null ? visualizerComputation : computation;
            } else {
               pingSource = computation;
            }
            TmpFileWriter tmpFileWriter = new TmpFileWriter(pingSource);
            DisplayRunner displayRunner = new DisplayRunner(this, tmpFileWriter);
            processing = new KoronaPlayboxProcessing(computation, tmpFileWriter, displayRunner);
            surveyText.setText(pingSource.getPingConfiguration().getRawFileConfiguration().getSurveyName());
         } catch (IOException e) {
            try {
               computation.close();
            } catch (IOException suppressed) {
               e.addSuppressed(suppressed);
            }
            GuiUtils.showErrorDialog(displayPanel, "Error setting up computation", e);
         }
      }

      for (PlayboxModule oldPlayboxModule : playboxModules) {
         if (!newPlayboxModules.contains(oldPlayboxModule)) {
            oldPlayboxModule.setKoronaPlaybox(null);
         }
      }
      for (PlayboxModule playboxModule : newPlayboxModules) {
         playboxModule.setKoronaPlaybox(this);
      }
      playboxModules = newPlayboxModules;

      if (!visualizerModules.equals(newVisualizerModules)) {
         visualizerModules = newVisualizerModules;

         MultiSplitPane multiSplitPane = new MultiSplitPane(JSplitPane.HORIZONTAL_SPLIT);
         for (VisualizerModule visualizerModule : visualizerModules) {
            multiSplitPane.add(visualizerModule.getComponent());
         }

         GuiUtils.replaceContent(displayPanel, multiSplitPane.getPanel());

         multiSplitPane.distributeEvenly();
      }

      update();
   }

   private @Nullable ModuleContainerComputation createComputation() {
      if (rawFile == null) {
         return null;
      }
      while (true) {
         PingReader pingReader;
         try {
            pingReader = PingReaderFactory.create(korona.getDataFormatManager(), rawFile);
         } catch (IOException e) {
            GuiUtils.showErrorDialog(displayPanel, "Error opening file " + rawFile, e);
            return null;
         }
         AtomicReference<@Nullable Throwable> errorRef = new AtomicReference<>();
         ModuleContainerComputation computation = new WorkerDialog(displayPanel, "Preparing computation setup...")
               .setOnError(errorRef::set)
               .startMakeValue(asyncHandle -> {
                  ComputationContext computationContext = new ComputationContext(moduleContainer, pingReader, asyncHandle)
                        .setUsingKoronaPlaybox(true);
                  return new ModuleContainerComputation(computationContext);
               });
         Throwable error = errorRef.get();
         if (error != null) {
            if (error instanceof ModuleException e) {
               GuiUtils.showErrorDialog(displayPanel, "Error setting up computation for module " + e.getModule().getDisplayName(), e);
               boolean ok = new ModuleEditor(moduleContainer, true, mainPanel)
                     .setSelectedModule(e.getModule())
                     .show()
                     .getOK();
               if (ok) {
                  continue;
               }
            } else {
               GuiUtils.showErrorDialog(displayPanel, "Error setting up computation", error);
            }
         }
         if (computation == null) {
            try {
               pingReader.close();
            } catch (IOException e) {
               Log.global.log(Level.WARNING, "Error closing file " + rawFile, e);
            }
         }
         return computation;
      }
   }

   private VisualizerModule getDefaultVisualizerModule(PingConfiguration pingConfiguration) {
      VisualizerModule previousDefaultVisualizerModule = defaultVisualizerModule.visualizerModule;

      for (ModulePlugin modulePlugin : korona.getModuleManager().getModulePlugins()) {
         VisualizerModule previous = defaultVisualizerModule.plugin == modulePlugin ? previousDefaultVisualizerModule : null;
         VisualizerModule newDefaultVisualizerModule = modulePlugin.getDefaultVisualizerModule(pingConfiguration, previous);
         if (newDefaultVisualizerModule != null) {
            defaultVisualizerModule = new DefaultVisualizerModule(modulePlugin, newDefaultVisualizerModule);
            return newDefaultVisualizerModule;
         }
      }

      DisplayModule displayModule;
      if (previousDefaultVisualizerModule instanceof DisplayModule d) {
         displayModule = d;
      } else {
         displayModule = new DisplayModule();
      }
      defaultVisualizerModule = new DefaultVisualizerModule(null, displayModule);
      return displayModule;
   }

   private void update() {
      updateEnabledSettings();
      updateChangeManager.notifyListeners();
   }

   public ChangeManager getUpdateChangeManager() {
      return updateChangeManager;
   }

   /**
    * Updates the enabled settings for user input items.
    * according to the current program state.
    */
   private void updateEnabledSettings() {
      boolean hasData = rawFile != null;
      playButton.setEnabled(hasData);
      realtimeFactorSpinner.setEnabled(hasData);
      rewindButton.setEnabled(hasData);
   }

   private void updatePlayButton() {
      if (processing != null && processing.displayRunner().isRunning()) {
         playButton.setToolTipText("Pause");
         MiscIcons.PAUSE.on(playButton);
      } else {
         playButton.setToolTipText("Play");
         MiscIcons.PLAY.on(playButton);
      }
   }

   boolean isFullSpeed() {
      return fullSpeed;
   }

   int getRealtimeFactor() {
      return realtimeFactor;
   }

   public void loadRawFile(Path file) {
      rawFile = file;
      Log.global.info("Opening: " + file);
      setLastRawFile(file);
      reset();
   }

   void displayRunnerDone() {
      if (processing != null && processing.displayRunner().isEndOfInput()) {
         closeComputation();
         if (autoRewindCheckBox.isSelected()) {
            start();
            return;
         }
      }
      updatePlayButton();
   }

   void displayRunnerTime(Instant time) {
      logTime.setText(logTimeFormat.format(time));
   }

   public void loadCdsFile(Path file) {
      setCdsFile(file);

      Log.global.info("Opening: " + file);

      try {
         moduleContainer.readConfiguration(file);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
      }

      reset();
   }

   public void loadCfsFile(Path file) {
      Log.global.info("Opening: " + file);
      new WorkerDialog(mainPanel, "Loading module configuration...")
            .startWithoutCancel(() -> {
               configFileSettings.load(file);
               Path cdsFile = getCdsFile();
               if (cdsFile != null && Files.exists(cdsFile)) {
                  moduleContainer.readConfiguration(cdsFile);
               }
            });
      setLastCfsFile(file);
      update();
   }

   static Preferences getPreferences() {
      return KoronaPreferences.node("playbox");
   }

   private static @Nullable Path getLastFile(String preferenceName) {
      String path = getPreferences().get(preferenceName, null);
      return path != null ? Path.of(path) : null;
   }

   public static @Nullable Path getLastRawFile() {
      return getLastFile(PREFERENCE_LAST_RAW_FILE);
   }

   private static void setLastRawFile(Path file) {
      getPreferences().put(PREFERENCE_LAST_RAW_FILE, file.toString());
   }

   public static @Nullable Path getLastCdsFile() {
      return getLastFile(PREFERENCE_LAST_CDS_FILE);
   }

   public static @Nullable Path getLastCfsFile() {
      return getLastFile(PREFERENCE_LAST_CFS_FILE);
   }

   private static void setLastCdsFile(Path file) {
      getPreferences().put(PREFERENCE_LAST_CDS_FILE, file.toString());
   }

   public static void setLastCfsFile(Path file) {
      getPreferences().put(PREFERENCE_LAST_CFS_FILE, file.toString());
   }

   private void rewind() {
      boolean running = isRunning();
      stop();

      reset();

      if (running) {
         start();
      }
   }

   public boolean isRunning() {
      return processing != null && processing.displayRunner().isRunning();
   }

   public void start() {
      if (processing == null || processing.displayRunner().isEndOfInput()) {
         reset();
      }
      if (processing != null) {
         processing.displayRunner().start();
      }
      updatePlayButton();
   }

   public void stop() {
      new WorkerDialog(mainPanel, "Stopping KORONA...")
            .startWithoutCancel(() -> {
               if (processing != null) {
                  processing.displayRunner().stop();
               }
            });
      updatePlayButton();
   }

   public void close() {
      stop();
      for (PlayboxModule playboxModule : playboxModules) {
         playboxModule.setKoronaPlaybox(null);
      }
      closeComputation();
   }

   public boolean editCurrentConfiguration() {
      boolean running = isRunning();
      stop();

      boolean ok = new ModuleEditor(moduleContainer, true, mainPanel)
            .show()
            .getOK();

      reset();

      if (running) {
         start();
      }

      return ok;
   }

   private void importFiles(List<Path> files) {
      if (files.isEmpty()) {
         return;
      }
      Path file = files.getFirst(); // loading only one file.
      boolean running = isRunning();
      stop();
      importFile(file);
      if (running) {
         start();
      }
   }

   private void importFile(Path file) {
      if (file.toString().endsWith(ConfigFileSettings.FILE_TYPE.suffix())) {
         loadCfsFile(file);
         return;
      }
      SegmentHandle segmentHandle = korona.getDataFormatManager().createSegmentHandle(file);
      if (segmentHandle != null) {
         loadRawFile(file);
         return;
      }
      Toast.warning(mainPanel, "Cannot import file\n" + file);
   }

   private record DefaultVisualizerModule(
         @Nullable ModulePlugin plugin,
         @Nullable VisualizerModule visualizerModule
   ) {
   }
}
