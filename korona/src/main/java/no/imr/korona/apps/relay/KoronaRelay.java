package no.imr.korona.apps.relay;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.computation.io.WriterModule;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.ConfigFileSettingsUtils;
import no.imr.korona.config.KoronaSettingsUtils;
import no.imr.korona.config.gui.ContextVisibility;
import no.imr.korona.data.datamanager.labelling.DataFileLabel;
import no.imr.korona.data.datamanager.labelling.DataFileLabelUtils;
import no.imr.korona.data.datamanager.labelling.DataFileLabelling;
import no.imr.korona.data.formats.ek60.EK60DataFormatPlugin;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.util.ForwardingPingReader;
import no.imr.korona.resources.KoronaResource;
import no.imr.korona.util.CfsManager;
import no.imr.korona.util.KoronaPreferences;
import no.imr.korona.viewer.KoronaHelpSystem;
import no.imr.korona.viewer.KoronaPlayboxDialog;
import no.imr.tools.UnionList;
import no.imr.tools.Utils;
import no.imr.tools.adm.AdmService;
import no.imr.tools.adm.ApplicationInfo;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.CoalescingExecutor;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.help.HelpSystem;
import no.imr.tools.io.DirectoryListing;
import no.imr.tools.io.FileUtils;
import no.imr.tools.io.LastModifiedAndSize;
import no.imr.tools.io.LockedFile;
import no.imr.tools.listening.ListenableProperty;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.Listeners;
import no.imr.tools.logging.Log;
import no.imr.tools.logging.LoggingManager;
import no.imr.tools.misc.test.TestUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.FileListTransferHandler;
import no.imr.tools.swing.GeometryListener;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MenuAdapter;
import no.imr.tools.swing.MenuItems;
import no.imr.tools.swing.MenuUtils;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.SuffixFileFilter;
import no.imr.tools.swing.SwingDelayer;
import no.imr.tools.swing.WhenShowingTimer;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.table.TableUtils;
import no.imr.tools.time.RealtimeSyncer;
import no.imr.tools.time.Stopwatch;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Program for processing all raw files in a directory.
 * <p>
 * A file's modification time and size changes are used to decide when it is safe
 * to start processing a file.
 * <p>
 * NB: Source and destination directories should be different.
 */
public final class KoronaRelay {
   public static final ApplicationInfo APPLICATION_INFO = new ApplicationInfo(
         "LSSS", "Large Scale Survey System", "KoronaRelay", Korona.VERSION,
         KoronaResource.KORONA_32, KoronaResource.KORONA_64,
         "korona", "lsss");
   public static final LoggingManager LOGGING_MANAGER = new LoggingManager(APPLICATION_INFO);

   private static boolean startedAsMainApplication;

   public static final String ARG_COMMENT = "comment";
   public static final String ARG_SOURCE = "-source";
   public static final String ARG_DESTINATION = "-destination";
   public static final String ARG_CONFIG_FILE_SETTINGS = "-cfs";
   public static final String ARG_FIRST_SELECTED_FILE = "firstFile";
   public static final String ARG_LAST_SELECTED_FILE = "lastFile";
   public static final String ARG_DATA_FILE_LABELLING_FILE = "dataFileLabellingFile";

   public static final String STATUS_FILENAME = "status.xml";
   public static final String STATUS_LOCK_FILENAME = STATUS_FILENAME + FileUtils.LOCK_FILE_SUFFIX;
   public static final String NEW_SUFFIX = ".new";
   public static final String TMP_SUFFIX = ".tmp";
   public static final String KORONA_SUFFIX = "-korona";
   public static final String COPIED_CONFIG_FILES_DIR_NAME = "copiedConfigFiles";

   private static final String PREFERENCE_REALTIME_FACTOR = "realtimeFactor";

   private static final int UPDATE_INTERVAL_MILLIS = 5 * 1000;
   private static final int CHANGE_THRESHOLD_MILLIS = 300 * 1000;    //RK: 20 -> 300
   private static final int LAST_FILE_THRESHOLD_HOURS = 48;

   private int realtimeFactor = getPreferences().getInt(PREFERENCE_REALTIME_FACTOR, 5);

   private AsyncHandle processingAsyncHandle = new AsyncHandle();

   private final CoalescingExecutor rowInfoExecutor = new CoalescingExecutor(Exec.CACHED_THREAD_POOL);
   private AsyncHandle rowInfoAsyncHandle = new AsyncHandle();

   private final ListenableProperty<Boolean> processing = new ListenableProperty<>(false);
   private final ListenableProperty<Integer> remainingFiles = new ListenableProperty<>(0);
   private final ListenableProperty<Float> remainingWork = new ListenableProperty<>(0f);

   private final Korona korona = new Korona();
   private final HelpSystem koronaHelpSystem = KoronaHelpSystem.createHelpSystem(korona);

   private @Nullable Predicate<SegmentHandle> initialSelection;
   private @Nullable Path dataFileLabellingFile;
   private @Nullable DataFileLabelling dataFileLabelling;

   private class KoronaRelayFileParameter extends FileParameter {
      private KoronaRelayFileParameter(Name name, Mode mode) {
         super(name, getPreferenceFile(name.persistentName()), mode);

         subscribe(__ -> {
            Path file = getFile();
            if (file != null) {
               getPreferences().put(getName().persistentName(), file.toString());
            } else {
               getPreferences().remove(getName().persistentName());
            }
            Log.global.fine(getName().persistentName() + " = " + file);
            updateButtonsEnabledState();
         });
      }

      @Override
      public boolean isEnabled() {
         return processingAsyncHandle.isFinished();
      }

      @Override
      public void customizeFileChooser(JFileChooser fileChooser) {
         fileChooser.setDialogTitle("Select " + getName().displayName());
         ConfigFileSettingsUtils.installTooltip(fileChooser);
      }
   }

   private final StringParameter comment = new StringParameter(new Name("Comment"));

   private final FileParameter configFileSettings = new KoronaRelayFileParameter(
         new Name("ConfigFileSettings", "Config file settings"),
         FileParameter.Mode.FILE) {
      @Override
      public List<FileFilter> getFileFilters() {
         return List.of(new SuffixFileFilter(ConfigFileSettings.FILE_TYPE));
      }

      @Override
      public Copier getCopier() {
         return new DefaultCopier(this);
      }

      @Override
      public Editor getEditor() {
         return cfsManager.createCfsEditor();
      }
   };

   private final FileParameter sourceDirectory = new KoronaRelayFileParameter(
         new Name("SourceDirectory", "Source directory"),
         FileParameter.Mode.DIRECTORY);

   private final FileParameter destinationDirectory = new KoronaRelayFileParameter(
         new Name("DestinationDirectory", "Destination directory"),
         FileParameter.Mode.DIRECTORY);

   private final JFrame frame = new JFrame("KORONA relay " + Korona.VERSION);
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final JMenuItem editSettingsItem = MiscIcons.SETTINGS.on(new JMenuItem());
   private final JMenuItem exitItem = MiscIcons.POWER.on(new JMenuItem("Exit"));

   private static final Dimension BUTTON_DIMENSION = new Dimension(24, 24);
   private static final Dimension TABLE_SIZE = new Dimension(700, 200);

   private final JButton playButton = new JButton();
   private final JCheckBox fullSpeedCheckBox = new JCheckBox("Full speed", true);
   private final JCheckBox notLastFileCheckBox = new JCheckBox("Not last file if recently modified", true);
   private final JButton refreshButton = MiscIcons.REFRESH.on(new JButton());
   private final JButton koronaPlayboxButton = new JButton("KORONA playbox");

   private final StatusFileError statusFileError = new StatusFileError(this::reCreateStatusXml);

   private final RawFileTableModel rawFileTableModel = new RawFileTableModel();
   private final JTable rawFileTable = new JTable(rawFileTableModel);

   private final CfsManager cfsManager = new CfsManager(korona, configFileSettings, null, ContextVisibility.SHOW);

   private DirectoryListing destinationDirectoryListing = DirectoryListing.of();

   private final KoronaRelaySettings settings = new KoronaRelaySettings();

   private final ParameterEditor parameterEditor = new ParameterEditor(List.of(
         comment,
         configFileSettings,
         sourceDirectory,
         destinationDirectory
   ));

   public KoronaRelay(Map<String, String> env) {
      cfsManager.setDataFileLabellingSupplier(() -> dataFileLabelling);

      parameterEditor.getGUIConfig().setHorizontalFill(true);

      setParameter(env, ARG_COMMENT, comment);
      comment.setEnabled(false);
      comment.setProperty(BaseParameter.KEY_LEFT_ALIGNED, true);
      if (comment.getValue().isBlank()) {
         comment.setVisible(false);
      }

      parseEnv(env);
      init();

      // Add directory listeners after initialization.
      Listener directoryListener = () -> {
         updateTable();
         checkDirectories();
      };
      directoryListener.addTo(
            sourceDirectory,
            destinationDirectory
      );
   }

   private static Preferences getPreferences() {
      return KoronaPreferences.node("relay");
   }

   private void parseEnv(Map<String, String> env) {
      setParameter(env, ARG_SOURCE, sourceDirectory);
      setParameter(env, ARG_DESTINATION, destinationDirectory);
      setParameter(env, ARG_CONFIG_FILE_SETTINGS, configFileSettings);

      String first = env.get(ARG_FIRST_SELECTED_FILE);
      String last = env.get(ARG_LAST_SELECTED_FILE);
      if (first != null && last != null) {
         Range<String> range = new DefaultRange<>(first, last);
         initialSelection = segmentHandle -> {
            return range.containsIncludingEnd(segmentHandle.getBaseName());
         };
      }

      String labellingFile = env.get(ARG_DATA_FILE_LABELLING_FILE);
      if (labellingFile != null) {
         dataFileLabellingFile = Path.of(labellingFile);
         dataFileLabelling = new DataFileLabelling(dataFileLabellingFile);
         if (initialSelection == null) {
            try {
               String title = cfsManager.loadConfigFileSettings().getDataFileLabelTitle();
               DataFileLabel label = dataFileLabelling.getLabelByTitle(title);
               if (label != null) {
                  initialSelection = segmentHandle -> {
                     return dataFileLabelling.getLabels(segmentHandle).contains(label);
                  };
               }
            } catch (IOException e) {
               // Ignore.
            }
         }
      }
   }

   private static void setParameter(Map<String, String> env, String key, ValueParameter<?> parameter) {
      String value = env.get(key);
      if (value != null) {
         parameter.setStringValue(value);
      }
   }

   private void init() {
      frame.setIconImage(KoronaResource.KORONA_32);
      frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      frame.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            shutDown();
         }
      });
      frame.setJMenuBar(createMenuBar());
      frame.getContentPane().add(createMainPanel());
      GeometryListener.startPreferenceSyncing(frame, new Dimension(800, 600), null, getPreferences(), "windowGeometry");
      frame.setVisible(true);

      SwingUtilities.invokeLater(playButton::requestFocusInWindow);
      SwingUtilities.invokeLater(frame::toFront); // Needed when starting KoronaRelay from LSSS.

      resetTable();
      updateButtonsEnabledState();
      WhenShowingTimer.start(frame, UPDATE_INTERVAL_MILLIS, this::updateTable);

      checkDirectories();
   }

   private void checkDirectories() {
      Path source = sourceDirectory.getFile();
      Path destination = destinationDirectory.getFile();
      if (source != null && source.equals(destination)) {
         JOptionPane.showMessageDialog(frame, """
                     NB: Source and destination directories are equal!

                     Processed files will processed again repeatedly...""",
               "Warning", JOptionPane.WARNING_MESSAGE);
      }
   }

   private static @Nullable Path getPreferenceFile(String preferenceKey) {
      String path = getPreferences().get(preferenceKey, null);
      return path != null ? Path.of(path) : null;
   }

   private JPanel createMainPanel() {
      FileListTransferHandler transferHandler = new FileListTransferHandler(files -> {
         if (files.isEmpty()) {
            return;
         }
         Path file = files.getFirst();
         if (Files.isRegularFile(file)) {
            file = file.getParent();
         }
         sourceDirectory.setFile(file);
      });

      rawFileTable.setTransferHandler(transferHandler);
      rawFileTable.setPreferredScrollableViewportSize(TABLE_SIZE);
      rawFileTable.getTableHeader().setReorderingAllowed(false);

      TableColumn skipColumn = rawFileTable.getColumnModel().getColumn(RawFileTableModel.SKIP_COLUMN);
      skipColumn.setMinWidth(30);
      skipColumn.setMaxWidth(30);

      TableColumn nameColumn = rawFileTable.getColumnModel().getColumn(RawFileTableModel.NAME_COLUMN);
      DefaultTableCellRenderer nameCellRenderer = new NameCellRenderer();
      nameColumn.setCellRenderer(nameCellRenderer);
      nameColumn.setPreferredWidth(200);

      TableColumn sizeColumn = rawFileTable.getColumnModel().getColumn(RawFileTableModel.SIZE_COLUMN);
      sizeColumn.setCellRenderer(TableUtils.defaultTableCellRenderer(DefaultTableCellRenderer.RIGHT));
      sizeColumn.setMinWidth(100);
      sizeColumn.setMaxWidth(100);

      TableColumn lastModifiedColumn = rawFileTable.getColumnModel().getColumn(RawFileTableModel.LAST_MODIFIED_COLUMN);
      lastModifiedColumn.setCellRenderer(TableUtils.defaultTableCellRenderer(DefaultTableCellRenderer.CENTER));
      lastModifiedColumn.setMinWidth(160);
      lastModifiedColumn.setMaxWidth(160);

      TableColumn statusColumn = rawFileTable.getColumnModel().getColumn(RawFileTableModel.STATUS_COLUMN);
      DefaultTableCellRenderer statusCellRenderer = new StatusCellRenderer();
      statusCellRenderer.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
      statusColumn.setCellRenderer(statusCellRenderer);
      statusColumn.setResizable(false);
      statusColumn.setMinWidth(120);
      statusColumn.setMaxWidth(120);

      JPanel rawTablePanel = new JPanel(new BorderLayout());
      rawTablePanel.add(rawFileTable.getTableHeader(), BorderLayout.NORTH);
      JScrollPane rawFileTableScrollPane = new JScrollPane(rawFileTable);
      rawTablePanel.add(rawFileTableScrollPane);
      rawTablePanel.setTransferHandler(transferHandler);
      PopupMenuMouseListener popupMenuMouseListener = new PopupMenuMouseListener(__ -> {
         JPopupMenu menu = new JPopupMenu();

         if (dataFileLabelling != null) {
            menu.add(labelMenu(dataFileLabelling));
            menu.addSeparator();
         }

         JMenuItem selectAll = menu.add("Select all");
         selectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK));
         selectAll.addActionListener(e -> rawFileTable.selectAll());

         JMenuItem scrollToSelectionItem = menu.add("Scroll to selected files");
         scrollToSelectionItem.setEnabled(!rawFileTable.getSelectionModel().isSelectionEmpty());
         scrollToSelectionItem.addActionListener(e -> TableUtils.scrollToSelectedRows(rawFileTable));

         menu.addSeparator();

         addSkipItems(menu);

         menu.addSeparator();

         JMenuItem startItem = MiscIcons.PLAY.on(menu.add("Start processing selected files"));
         startItem.addActionListener(e -> {
            int rowCount = rawFileTable.getRowCount();
            for (int i = 0; i < rowCount; i++) {
               rawFileTableModel.rows.get(i).setSkip(!rawFileTable.isRowSelected(i));
            }
            start();
         });

         return menu;
      });
      rawFileTable.addMouseListener(popupMenuMouseListener);
      rawFileTableScrollPane.addMouseListener(popupMenuMouseListener);

      JComponent editorComponent = parameterEditor.getEditorComponent();
      editorComponent.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

      JPanel topPanel = new JPanel(new BorderLayout());
      topPanel.add(editorComponent);
      topPanel.add(statusFileError.getComponent(), BorderLayout.SOUTH);

      mainPanel.add(topPanel, BorderLayout.NORTH);
      mainPanel.add(rawTablePanel);
      mainPanel.add(KoronaRelayStatusBar.create(destinationDirectory, remainingFiles, remainingWork, processing).getComponent(), BorderLayout.SOUTH);
      return mainPanel;
   }

   private JMenuBar createMenuBar() {
      MiscIcons.PLAY.on(playButton);
      playButton.setPreferredSize(BUTTON_DIMENSION);
      playButton.setToolTipText("Start processing");
      playButton.addActionListener(e -> {
         if (processing.getValue()) {
            stop();
         } else {
            start();
         }
      });

      refreshButton.setPreferredSize(BUTTON_DIMENSION);
      refreshButton.setToolTipText("Refresh file list");
      refreshButton.addActionListener(e -> {
         refreshButton.setEnabled(false);
         if (dataFileLabellingFile != null) {
            dataFileLabelling = new DataFileLabelling(dataFileLabellingFile);
         }
         updateTable();
      });

      notLastFileCheckBox.setToolTipText("<html>"
            + "Don't process the last file if modified in the last " + LAST_FILE_THRESHOLD_HOURS + " hours."
            + "<p>Useful for avoiding processing files still being written to.");

      koronaPlayboxButton.setToolTipText("Starts KORONA playbox for the selected file");
      koronaPlayboxButton.addActionListener(e -> {
         int[] selectedRows = rawFileTable.getSelectedRows();
         int i = selectedRows.length == 0 ? 0 : selectedRows[0];
         RawFileTableModel.Row row = rawFileTableModel.rows.get(i);
         new KoronaPlayboxDialog(frame, korona, configFileSettings.getFile(), row.segmentHandle);
      });
      rawFileTableModel.addTableModelListener(e -> koronaPlayboxButton.setEnabled(rawFileTableModel.getRowCount() > 0));

      SpinnerNumberModel realtimeSpinnerModel = new SpinnerNumberModel(realtimeFactor, 1, Integer.MAX_VALUE, 1);
      JSpinner realtimeSpinner = new JSpinner(realtimeSpinnerModel);
      realtimeSpinner.setToolTipText("Processing speed relative to realtime");
      realtimeSpinner.setPreferredSize(new Dimension(50, 24));
      realtimeSpinner.addChangeListener(e -> {
         realtimeFactor = realtimeSpinnerModel.getNumber().intValue();
         getPreferences().putInt(PREFERENCE_REALTIME_FACTOR, realtimeFactor);
      });
      realtimeSpinner.setEnabled(!fullSpeedCheckBox.isSelected());

      fullSpeedCheckBox.setToolTipText("Do processing as fast as possible");
      fullSpeedCheckBox.addActionListener(e -> realtimeSpinner.setEnabled(!fullSpeedCheckBox.isSelected()));

      JMenu fileMenu = new JMenu("File");
      fileMenu.setMnemonic(KeyEvent.VK_F);

      addSkipItems(fileMenu.getPopupMenu());

      fileMenu.addSeparator();

      JMenuItem resetRemoteProcessingItem = fileMenu.add("Reset remote processing status");
      resetRemoteProcessingItem.addActionListener(e -> resetRemoteProcessing());

      JMenuItem reCreateStatusXmlItem = fileMenu.add("Re-create status.xml");
      reCreateStatusXmlItem.addActionListener(e -> reCreateStatusXml());

      fileMenu.addSeparator();

      fileMenu.add(editSettingsItem);
      editSettingsItem.setMnemonic(KeyEvent.VK_E);
      editSettingsItem.addActionListener(e -> {
         settings.showEditor(frame, processingAsyncHandle.isFinished());
      });

      fileMenu.addSeparator();

      fileMenu.add(exitItem);
      exitItem.setMnemonic(KeyEvent.VK_X);
      exitItem.addActionListener(e -> shutDown());

      fileMenu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuSelected(MenuEvent e) {
            boolean remoteProcessing = rawFileTableModel.rows.stream()
                  .anyMatch(row -> row.status == Status.RemoteProcessing);
            resetRemoteProcessingItem.setVisible(remoteProcessing);

            reCreateStatusXmlItem.setEnabled(destinationDirectory.getFile() != null);
         }
      });

      JMenu helpMenu = new JMenu("Help");
      helpMenu.setMnemonic(KeyEvent.VK_H);
      helpMenu.add(koronaHelpSystem.createHelpMenuItem());
      helpMenu.add(MenuItems.logFile(LOGGING_MANAGER));
      AdmService.INSTANCE.addUpdateLicenseMenuItem(helpMenu);
      helpMenu.add(MenuItems.about(APPLICATION_INFO));

      JPanel buttonPanel = new JPanel();
      buttonPanel.add(refreshButton);
      buttonPanel.add(Box.createHorizontalStrut(10));
      buttonPanel.add(playButton);
      buttonPanel.add(realtimeSpinner);
      buttonPanel.add(Box.createHorizontalStrut(5));
      buttonPanel.add(fullSpeedCheckBox);
      buttonPanel.add(Box.createHorizontalStrut(5));
      buttonPanel.add(notLastFileCheckBox);
      buttonPanel.add(Box.createHorizontalStrut(50));
      buttonPanel.add(koronaPlayboxButton);

      JMenuBar menuBar = new JMenuBar();
      menuBar.add(fileMenu);
      menuBar.add(helpMenu);
      if (Utils.useTestFeatures()) {
         menuBar.add(TestUtils.createDebugMenu(LOGGING_MANAGER));
      }
      menuBar.add(buttonPanel);
      return menuBar;
   }

   private JMenu labelMenu(DataFileLabelling dataFileLabelling) {
      JMenu menu = MiscIcons.LABEL.on(new JMenu("Data file labels"));
      menu.setMnemonic(KeyEvent.VK_L);
      menu.setDisplayedMnemonicIndex(10);

      Collection<DataFileLabel> allLabels = Utils.sorted(dataFileLabelling.getAllLabels());

      JMenu selectMenu = MenuUtils.addMenu(menu, "Select", KeyEvent.VK_S);
      JMenu setToBeProcessedMenu = MenuUtils.addMenu(menu, "Set to be processed", KeyEvent.VK_P);

      for (DataFileLabel label : allLabels) {
         selectMenu.add(DataFileLabelUtils.menuItem(label, e -> {
            List<RawFileTableModel.Row> rows = rawFileTableModel.rows;
            TableUtils.setTableSelection(rawFileTable, i -> {
               return dataFileLabelling.getLabels(rows.get(i).segmentHandle).contains(label);
            });
         }));
      }
      for (DataFileLabel label : allLabels) {
         setToBeProcessedMenu.add(DataFileLabelUtils.menuItem(label, e -> {
            List<RawFileTableModel.Row> rows = rawFileTableModel.rows;
            for (RawFileTableModel.Row row : rows) {
               boolean hasLabel = dataFileLabelling.getLabels(row.segmentHandle).contains(label);
               row.setSkip(!hasLabel);
            }
         }));
      }

      return menu;
   }

   private void addSkipItems(JPopupMenu menu) {
      JMenuItem invertSkipItem = menu.add("Invert skip in selection");
      invertSkipItem.setMnemonic(KeyEvent.VK_I);
      invertSkipItem.addActionListener(e -> {
         for (int i : getSelectedRows()) {
            RawFileTableModel.Row row = rawFileTableModel.rows.get(i);
            row.setSkip(!row.skip);
         }
      });

      JMenuItem skipAllItem = menu.add("Skip all in selection");
      skipAllItem.setMnemonic(KeyEvent.VK_A);
      skipAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
      skipAllItem.addActionListener(e -> {
         for (int i : getSelectedRows()) {
            RawFileTableModel.Row row = rawFileTableModel.rows.get(i);
            row.setSkip(true);
         }
      });

      JMenuItem skipNoneItem = menu.add("Skip none in selection");
      skipNoneItem.setMnemonic(KeyEvent.VK_N);
      skipNoneItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0));
      skipNoneItem.addActionListener(e -> {
         for (int i : getSelectedRows()) {
            RawFileTableModel.Row row = rawFileTableModel.rows.get(i);
            row.setSkip(false);
         }
      });
   }

   private void reCreateStatusXml() {
      if (KoronaRelayUtils.showReCreateStatusXmlDialog(frame, getStatusFile())) {
         statusFileError.setError(null);
      }
   }

   private int[] getSelectedRows() {
      int[] selectedRows = rawFileTable.getSelectedRows();
      if (selectedRows.length == 0) {
         JOptionPane.showMessageDialog(frame, "No rows selected in file table!");
      }
      return selectedRows;
   }

   private void start() {
      if (!processingAsyncHandle.isFinished()) {
         return;
      }
      Path destDir = destinationDirectory.getFile();
      if (destDir == null) {
         stop();
         return;
      }
      try {
         FileUtils.createDirectories(destDir);
      } catch (IOException e) {
         GuiUtils.showErrorDialog(frame, "Error creating directory " + destDir, e);
      }

      processingAsyncHandle = new AsyncHandle();
      Exec.CACHED_THREAD_POOL.execute(() -> {
         Path copyDir = destDir.resolve(COPIED_CONFIG_FILES_DIR_NAME);
         try {
            ConfigFileSettingsUtils.copy(cfsManager.loadConfigFileSettings(), copyDir, processingAsyncHandle);
            Log.global.info("Copied config files to " + copyDir);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error copying config files to " + copyDir, e);
         }
      });
      ExecutorService processingExecutor = settings.lowPriority.getBooleanValue()
            ? Exec.LOW_PRIORITY_CACHED_THREAD_POOL
            : Exec.CACHED_THREAD_POOL;
      for (int i = 0; i < settings.getProcessCount(); i++) {
         processingExecutor.execute(processingAsyncHandle.createManagedRunnable(new Processor()));
      }
      processing.setValue(true);

      playButton.setToolTipText("Stop processing");
      MiscIcons.STOP.on(playButton);
      updateButtonsEnabledState();
   }

   private void stop() {
      processingAsyncHandle.cancel();
      new WorkerDialog(frame, "Stopping processing...")
            .startWithoutCancel(processingAsyncHandle::waitUntilFinished);
      processing.setValue(false);

      playButton.setToolTipText("Start processing");
      MiscIcons.PLAY.on(playButton);
      updateButtonsEnabledState();
      updateTable();
   }

   private void updateButtonsEnabledState() {
      parameterEditor.getParameterEditorData().update();

      editSettingsItem.setText(processingAsyncHandle.isFinished() ? "Edit settings..." : "View settings...");
      playButton.setEnabled(sourceDirectory.getFile() != null && destinationDirectory.getFile() != null);
      refreshButton.setEnabled(sourceDirectory.getFile() != null);
   }

   private void resetTable() {
      rowInfoAsyncHandle.cancel();
      rowInfoAsyncHandle = new AsyncHandle();
      rawFileTableModel.clear();

      AtomicReference<Map<SegmentHandle, RowInfo>> rowInfos = new AtomicReference<>();
      AtomicReference<@Nullable KoronaRelayStatus> koronaRelayStatus = new AtomicReference<>();
      WorkerDialog.Result result = new WorkerDialog(frame, "Updating Searching for files...")
            .start(asyncHandle -> {
               destinationDirectoryListing = listDirectory(destinationDirectory.getFile(), asyncHandle);
               rowInfos.set(createRowInfos(korona, sourceDirectory.getFile(), asyncHandle));
               koronaRelayStatus.set(lockAndGetKoronaRelayStatus());
            });
      if (result.success()) {
         rawFileTableModel.update(rowInfos.get(), koronaRelayStatus.get());
         doInitialSelection();
      }
   }

   private void doInitialSelection() {
      if (initialSelection == null) {
         return;
      }
      rawFileTable.getSelectionModel().setValueIsAdjusting(true);
      List<RawFileTableModel.Row> rows = rawFileTableModel.rows;
      for (int i = 0; i < rows.size(); i++) {
         RawFileTableModel.Row row = rows.get(i);
         boolean select = initialSelection.test(row.segmentHandle);
         row.setSkip(!select);
         if (select) {
            rawFileTable.getSelectionModel().addSelectionInterval(i, i);
         }
      }
      rawFileTable.getSelectionModel().setValueIsAdjusting(false);
      initialSelection = null;
      SwingUtilities.invokeLater(() -> TableUtils.scrollToSelectedRows(rawFileTable));
   }

   private void updateTable() {
      rowInfoExecutor.execute(() -> {
         destinationDirectoryListing = listDirectory(destinationDirectory.getFile(), rowInfoAsyncHandle);
         Map<SegmentHandle, RowInfo> rowInfos = createRowInfos(korona, sourceDirectory.getFile(), rowInfoAsyncHandle);
         KoronaRelayStatus koronaRelayStatus = lockAndGetKoronaRelayStatus();
         SwingUtilities.invokeLater(() -> {
            rawFileTableModel.update(rowInfos, koronaRelayStatus);
            refreshButton.setEnabled(sourceDirectory.getFile() != null);
         });
      });
   }

   private void resetRemoteProcessing() {
      int answer = JOptionPane.showConfirmDialog(frame, """
                  Reset remote processing status?
                  Please make sure that no other instances of KoronaRelay are running.
                  """,
            "Reset remote processing status", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
      if (answer != JOptionPane.YES_OPTION) {
         return;
      }

      Log.global.info("Reset remote processing status file " + destinationDirectory.getFile());

      try (LockedFile lockedFile = LockedFile.getUnlocked(getLockFile())) {
         lockedFile.lock();

         KoronaRelayStatus koronaRelayStatus;
         try {
            koronaRelayStatus = getKoronaRelayStatus();
         } catch (IOException e) {
            SwingUtilities.invokeLater(() -> KoronaRelayUtils.showStatusFileErrorDialog(frame, e, getStatusFile()));
            return;
         }
         for (RawFileTableModel.Row row : rawFileTableModel.rows) {
            if (row.status == Status.RemoteProcessing) {
               koronaRelayStatus.getFilesBeingProcessed().remove(row.getProcessedNewFile());
            }
         }
         rawFileTableModel.updateRemoteStatus(koronaRelayStatus);
         koronaRelayStatus.save();
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error updating status file", e);
      }
   }

   private Path getLockFile() {
      return destinationDirectory.getFile().resolve(STATUS_LOCK_FILENAME);
   }

   private Path getStatusFile() {
      return destinationDirectory.getFile().resolve(STATUS_FILENAME);
   }

   private KoronaRelayStatus getKoronaRelayStatus() throws IOException {
      try {
         KoronaRelayStatus koronaRelayStatus = new KoronaRelayStatus(getStatusFile());
         statusFileError.setError(null);
         return koronaRelayStatus;
      } catch (IOException e) {
         statusFileError.setError(e);
         throw e;
      }
   }

   private @Nullable KoronaRelayStatus lockAndGetKoronaRelayStatus() {
      if (!destinationDirectory.exists()) {
         return null;
      }

      try (LockedFile lockedFile = LockedFile.getUnlocked(getLockFile())) {
         lockedFile.lock();
         try {
            return getKoronaRelayStatus();
         } catch (IOException e) {
            // Error shown by statusFileError.
            return null;
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error accessing status file lock", e);
         return null;
      }
   }

   private static DirectoryListing listDirectory(@Nullable Path directory, AsyncHandle asyncHandle) {
      if (directory == null) {
         return DirectoryListing.of();
      }
      try {
         return DirectoryListing.of(directory, asyncHandle);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error listing files in " + directory, e);
         return DirectoryListing.of();
      }
   }

   private record RowInfo(SegmentHandle segmentHandle, long lastModified, long size) {
   }

   private static Map<SegmentHandle, RowInfo> createRowInfos(Korona korona, @Nullable Path directory, AsyncHandle asyncHandle) {
      DirectoryListing sourceDirectoryListing = listDirectory(directory, asyncHandle);
      List<SegmentHandle> segmentHandles = korona.getDataFormatManager().createSegmentHandles(sourceDirectoryListing.map().keySet(), asyncHandle);
      Map<SegmentHandle, RowInfo> infos = HashMap.newHashMap(segmentHandles.size());
      for (SegmentHandle segmentHandle : segmentHandles) {
         if (asyncHandle.isCancelled()) {
            return Map.of();
         }
         LastModifiedAndSize lastModifiedAndSize = segmentHandle.getLastModifiedAndSize(sourceDirectoryListing.map(), asyncHandle);
         infos.put(segmentHandle, new RowInfo(segmentHandle, lastModifiedAndSize.lastModified(), lastModifiedAndSize.size()));
      }
      return infos;
   }

   private enum ProcessingResult {
      Success, Cancelled, Failure
   }

   /**
    * For doing processing. As long as {@link KoronaRelay#processingAsyncHandle} is not cancelled, is keeps checking
    * {@link KoronaRelay#sourceDirectory} for files to process.
    */
   private final class Processor implements Runnable {
      private Processor() {
      }

      @Override
      public void run() {
         while (!processingAsyncHandle.isCancelled()) {
            RawFileTableModel.Row row = null;
            try {
               row = getNextRowForProcessing();
               tick(row);
            } catch (Throwable e) {
               Log.global.log(Level.WARNING, "KoronaRelay error" + (row != null ? ": " + row.segmentHandle.getDisplayName() : ""), e);
               processingAsyncHandle.sleep(1000); // Wait a little and keep going.
            }
         }
      }

      private void tick(RawFileTableModel.@Nullable Row row) throws IOException {
         if (row == null) {
            processingAsyncHandle.sleep(1000);
         } else {
            ProcessingResult processingResult = ProcessingResult.Failure;
            try {
               processingResult = process(row);
            } finally {
               doneProcessing(row, processingResult);

               if (processingResult != ProcessingResult.Success) {
                  row.deleteTmpAndNew();
               }
            }
         }
      }

      private RawFileTableModel.@Nullable Row getNextRowForProcessing() throws IOException {
         try (LockedFile lockedFile = LockedFile.getUnlocked(getLockFile())) {
            lockedFile.lock();
            if (processingAsyncHandle.isCancelled()) {
               return null;
            }
            KoronaRelayStatus koronaRelayStatus;
            try {
               koronaRelayStatus = getKoronaRelayStatus();
            } catch (IOException e) {
               // Error shown by statusFileError.
               return null;
            }
            RawFileTableModel.Row row = rawFileTableModel.getNextRowForProcessing(koronaRelayStatus);
            if (row != null) {
               koronaRelayStatus.getFilesBeingProcessed().add(row.getProcessedNewFile());
               row.getProcessedNewFiles().forEach(koronaRelayStatus.getFilesReadyForCopy()::remove);
               koronaRelayStatus.save();
            }
            return row;
         }
      }

      private void doneProcessing(RawFileTableModel.Row row, ProcessingResult processingResult) throws IOException {
         try (LockedFile lockedFile = LockedFile.getUnlocked(getLockFile())) {
            lockedFile.lock();

            KoronaRelayStatus koronaRelayStatus;
            try {
               koronaRelayStatus = getKoronaRelayStatus();
            } catch (IOException e) {
               // Error shown by statusFileError.
               synchronized (rawFileTableModel) {
                  row.doneProcessing(processingResult);
               }
               return;
            }

            synchronized (rawFileTableModel) {
               koronaRelayStatus.getFilesBeingProcessed().remove(row.getProcessedNewFile());

               if (processingResult == ProcessingResult.Success) {
                  koronaRelayStatus.getFilesReadyForCopy().addAll(row.getProcessedNewFiles());
               }

               row.doneProcessing(processingResult);
            }

            koronaRelayStatus.save();
         }
      }

      private ProcessingResult process(RawFileTableModel.Row row) throws IOException {
         String segmentName = row.segmentHandle.getDisplayName();
         Log.global.info("Starting to process " + segmentName);
         Stopwatch stopwatch = Stopwatch.createStarted();
         SingleRowProcessor singleRowProcessor = new SingleRowProcessor(row);
         singleRowProcessor.process();
         Log.global.info("Finished processing " + segmentName + " in " + stopwatch.seconds() + " seconds");
         return singleRowProcessor.cancelled() ? ProcessingResult.Cancelled : ProcessingResult.Success;
      }
   }

   /**
    * Processes a single file.
    */
   private final class SingleRowProcessor {
      private final RawFileTableModel.Row row;
      private long ntDateIn;
      private long ntDateOut;

      private SingleRowProcessor(RawFileTableModel.Row row) {
         this.row = row;
      }

      private void process() throws IOException {
         Path destinationRawFile = row.getProcessedFile();
         FileUtils.createDirectories(destinationRawFile.getParent());

         try (PingReader pingReader = row.segmentHandle.createPingReader()) {

            ModuleContainer moduleContainer = cfsManager.loadModuleContainer();

            long t0 = pingReader.getPingConfiguration().getRawFileConfiguration().getNTDate();
            ntDateIn = t0;
            ntDateOut = t0;

            WriterModule writerModule = moduleContainer.addModule(new WriterModule());
            writerModule.setExtraSuffix(TMP_SUFFIX);
            writerModule.fileName.setValue(destinationRawFile.getFileName().toString());
            writerModule.directory.setFile(destinationRawFile.getParent());

            Listener updateListener = Listeners.coalescingInExecutor(Exec.CACHED_THREAD_POOL, () -> updateProgress(pingReader));

            PingReader observingPingReader = new ForwardingPingReader(pingReader) {
               @Override
               public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
                  Ping ping = super.nextPing(asyncHandle);
                  if (ping != null) {
                     ntDateIn = ping.getNTDate();
                  }
                  updateListener.listen();
                  return ping;
               }
            };

            try (ModuleContainerComputation computation = moduleContainer.createComputation(observingPingReader, processingAsyncHandle)) {
               RealtimeSyncer realtimeSyncer = new RealtimeSyncer();
               while (!cancelled()) {
                  Ping ping = computation.nextPing();
                  if (ping == null) {
                     break;
                  }
                  ntDateOut = ping.getNTDate();
                  updateListener.listen();
                  realtimeSyncer.sync(ping.getTimeInMillis(), realtimeFactor, fullSpeedCheckBox.isSelected());
               }
            }
         }

         if (cancelled()) {
            row.deleteTmpAndNew();
         } else {
            row.moveTmpToNew();
         }
      }

      private boolean cancelled() {
         return processingAsyncHandle.isCancelled() || row.skip;
      }

      private void updateProgress(PingReader pingReader) {
         long t0 = pingReader.getPingConfiguration().getRawFileConfiguration().getNTDate();
         float readFraction = pingReader.getReadFraction();
         float processedFraction = ntDateIn == t0 ? 0 : readFraction * (ntDateOut - t0) / (ntDateIn - t0);
         row.setProgress((readFraction + processedFraction) / 2);
      }
   }

   private enum Status {
      New(false),
      Pending(true),
      Reprocess(true),
      Processing(false),
      RemoteProcessing(false),
      Error(false),
      Done(false);

      private final boolean readyForProcessing;

      Status(boolean readyForProcessing) {
         this.readyForProcessing = readyForProcessing;
      }

      private boolean isReadyForProcessing() {
         return readyForProcessing;
      }
   }

   /**
    * Information about the raw files in {@link KoronaRelay#sourceDirectory}.
    */
   private final class RawFileTableModel extends AbstractTableModel {
      private final class Row {
         private final SegmentHandle segmentHandle;
         private long lastModified;
         private long size;
         private long lastChangeTime;

         private long processedLastModified;

         private Status status = Status.New;
         private boolean skip;
         private float progress;

         private Row(RowInfo rowInfo) {
            segmentHandle = rowInfo.segmentHandle;
            lastModified = rowInfo.lastModified;
            size = rowInfo.size;
            lastChangeTime = lastModified;

            processedLastModified = getUpdatedProcessedLastModified();

            skip = anyDestinationFileExist();
         }

         private boolean anyDestinationFileExist() {
            return processedLastModified > 0;
         }

         private long getUpdatedProcessedLastModified() {
            if (destinationDirectory.getFile() == null) {
               return 0;
            }
            return Math.max(
                  destinationDirectoryListing.lastModifiedOr0(getProcessedFile()),
                  destinationDirectoryListing.lastModifiedOr0(getProcessedNewFile()));
         }

         private void update(RowInfo rowInfo) {
            if (lastModified != rowInfo.lastModified || size != rowInfo.size) {
               size = rowInfo.size;
               lastModified = rowInfo.lastModified;
               lastChangeTime = System.currentTimeMillis();
               fireTableRowUpdated();
            }
            updateStatus();
         }

         private void updateRemoteStatus(KoronaRelayStatus koronaRelayStatus) {
            if (koronaRelayStatus.getFilesBeingProcessed().contains(getProcessedNewFile())) {
               if (status != Status.Processing) {
                  setStatus(Status.RemoteProcessing);
               }
            } else {
               if (status == Status.RemoteProcessing) {
                  setStatus(Status.New);
                  updateStatus();
               }
            }
         }

         private void updateStatus() {
            if (status == Status.RemoteProcessing) {
               return;
            }
            if (status == Status.Processing) {
               return;
            }
            if (status == Status.Error && skip) {
               return;
            }

            long updatedProcessedLastModified = getUpdatedProcessedLastModified();
            if (processedLastModified != updatedProcessedLastModified) {
               processedLastModified = updatedProcessedLastModified;
               if (anyDestinationFileExist()) {
                  setSkip(true);
               }
            }

            Status newStatus;
            if (lastChangeTime + CHANGE_THRESHOLD_MILLIS > System.currentTimeMillis()) {
               newStatus = Status.New;
            } else if (anyDestinationFileExist()) {
               newStatus = skip ? Status.Done : Status.Reprocess;
            } else {
               newStatus = Status.Pending;
            }

            setStatus(newStatus);
         }

         private void doneProcessing(ProcessingResult processingResult) {
            assert status == Status.Processing : status;

            processedLastModified = getUpdatedProcessedLastModified();

            switch (processingResult) {
               case Success -> {
                  status = Status.Done;
                  skip = true;
               }
               case Cancelled -> {
                  status = anyDestinationFileExist() ? Status.Reprocess : Status.Pending;
               }
               case Failure -> {
                  status = Status.Error;
                  skip = true;
               }
            }

            fireTableRowUpdated();
         }

         private Path getProcessedFileBaseName() {
            return destinationDirectory.getFile().resolve(segmentHandle.getBaseName() + KORONA_SUFFIX);
         }

         private Path getProcessedFile() {
            return FileUtils.addSuffix(getProcessedFileBaseName(), EK60DataFormatPlugin.RAW_SUFFIX);
         }

         private Path getProcessedNewFile() {
            return FileUtils.addSuffix(getProcessedFileBaseName(), EK60DataFormatPlugin.RAW_SUFFIX + NEW_SUFFIX);
         }

         private List<Path> getProcessedNewFiles() {
            return getProcessedFiles(NEW_SUFFIX);
         }

         private List<Path> getProcessedFiles(String extraSuffix) {
            Path raw = FileUtils.addSuffix(getProcessedFileBaseName(), EK60DataFormatPlugin.RAW_SUFFIX + extraSuffix);
            Path idx = FileUtils.addSuffix(getProcessedFileBaseName(), EK60DataFormatPlugin.IDX_SUFFIX + extraSuffix);
            Path bot = FileUtils.addSuffix(getProcessedFileBaseName(), EK60DataFormatPlugin.BOT_SUFFIX + extraSuffix);
            return List.of(raw, idx, bot);
         }

         private void moveTmpToNew() throws IOException {
            List<Path> tmpFiles = getProcessedFiles(TMP_SUFFIX);
            List<Path> newFiles = getProcessedFiles(NEW_SUFFIX);
            for (int i = 0; i < tmpFiles.size(); i++) {
               FileUtils.move(tmpFiles.get(i), newFiles.get(i));
            }
         }

         private void deleteTmpAndNew() throws IOException {
            for (Path file : new UnionList<>(getProcessedFiles(TMP_SUFFIX), getProcessedFiles(NEW_SUFFIX))) {
               Files.deleteIfExists(file);
            }
         }

         private void setProgress(float progress) {
            this.progress = progress;
            fireTableRowUpdated();
         }

         private void setStatus(Status status) {
            if (this.status != status) {
               this.status = status;
               fireTableRowUpdated();
            }
         }

         private void setSkip(boolean skip) {
            if (this.skip != skip) {
               this.skip = skip;
               updateStatus();
               fireTableRowUpdated();
            }
         }

         private void fireTableRowUpdated() {
            updateRemainingFileCount();
            SwingUtilities.invokeLater(() -> {
               int i = rows.indexOf(this);
               fireTableRowsUpdated(i, i);
            });
         }

         private String getStatusString() {
            return switch (status) {
               case Processing -> {
                  String percent = progress == 1 ? "100" : progressFormat.format(progress * 100);
                  yield status + " " + percent + " %";
               }
               default -> status.toString();
            };
         }

         private float getRemainingProgress() {
            if (skip) {
               return 0;
            }
            if (status.isReadyForProcessing()) {
               return 1;
            }
            if (status == Status.Processing) {
               return 1 - progress;
            }
            return 0;
         }
      }

      private final DateTimeFormatter dateFormat = Utils.createLocalDateTimeFormatter("yyyy.MM.dd HH:mm:ss");
      private final DecimalFormat progressFormat = Utils.createDecimalFormat("##0.0");
      private final DecimalFormat sizeFormat = new DecimalFormat("#,##0");

      private final List<Row> rows = new ArrayList<>();
      private final NavigableMap<SegmentHandle, Row> rowMap = new TreeMap<>();
      private final String[] columnNames = {"Skip", "File", "Size", "Last modified", "Status"};
      private final Class<?>[] columnClasses = {Boolean.class, String.class, String.class, String.class, Row.class};

      private static final int SKIP_COLUMN = 0;
      private static final int NAME_COLUMN = 1;
      private static final int SIZE_COLUMN = 2;
      private static final int LAST_MODIFIED_COLUMN = 3;
      private static final int STATUS_COLUMN = 4;

      private RawFileTableModel() {
         progressFormat.setRoundingMode(RoundingMode.FLOOR);
         DecimalFormatSymbols decimalFormatSymbols = Utils.createDecimalFormatSymbols();
         decimalFormatSymbols.setGroupingSeparator(' ');
         sizeFormat.setDecimalFormatSymbols(decimalFormatSymbols);
      }

      private void clear() {
         rows.clear();
         rowMap.clear();
         fireTableDataChanged();
      }

      private synchronized void update(Map<SegmentHandle, RowInfo> rowInfos, @Nullable KoronaRelayStatus koronaRelayStatus) {
         boolean changed = rowMap.keySet().retainAll(rowInfos.keySet());

         for (Map.Entry<SegmentHandle, RowInfo> entry : rowInfos.entrySet()) {
            SegmentHandle segmentHandle = entry.getKey();
            RowInfo rowInfo = entry.getValue();
            Row row = rowMap.get(segmentHandle);
            if (row == null) {
               row = new Row(rowInfo);
               rowMap.put(segmentHandle, row);
               changed = true;
            }
            row.update(rowInfo);
         }

         if (changed) {
            Set<SegmentHandle> selectedSegmentHandles = Arrays.stream(rawFileTable.getSelectedRows())
                  .mapToObj(i -> rows.get(i).segmentHandle)
                  .collect(Collectors.toSet());
            rows.clear();
            rows.addAll(rowMap.values());
            fireTableDataChanged();
            updateRemainingFileCount();
            rawFileTable.revalidate();
            rawFileTable.repaint();
            TableUtils.setTableSelection(rawFileTable, i -> {
               return selectedSegmentHandles.contains(rows.get(i).segmentHandle);
            });
         }

         if (koronaRelayStatus != null) {
            updateRemoteStatus(koronaRelayStatus);
         }
      }

      private synchronized void updateRemoteStatus(KoronaRelayStatus koronaRelayStatus) {
         for (Row row : rows) {
            row.updateRemoteStatus(koronaRelayStatus);
         }
      }

      private synchronized @Nullable Row getNextRowForProcessing(KoronaRelayStatus koronaRelayStatus) {
         updateRemoteStatus(koronaRelayStatus);

         int endIndex = rows.size();
         if (notLastFileCheckBox.isSelected() && endIndex > 0) {
            Instant lastFileThreshold = Instant.now().minus(LAST_FILE_THRESHOLD_HOURS, ChronoUnit.HOURS);
            Instant lastFileModified = Instant.ofEpochMilli(rows.getLast().lastModified);
            boolean recentlyModified = lastFileModified.isAfter(lastFileThreshold);
            if (recentlyModified) {
               endIndex--;
            }
         }
         for (int i = 0; i < endIndex; i++) {
            Row row = rows.get(i);
            if (!row.skip && row.status.isReadyForProcessing()) {
               row.setStatus(Status.Processing);
               row.setProgress(0);
               return row;
            }
         }
         return null;
      }

      private void updateRemainingFileCount() {
         SwingDelayer.invokeLater(remainingWork, () -> {
            int file = 0;
            float work = 0;
            for (Row row : rowMap.values()) {
               float remainingProgress = row.getRemainingProgress();
               if (remainingProgress > 0) {
                  file++;
                  work += remainingProgress * row.size;
               }
            }
            remainingFiles.setValue(file);
            remainingWork.setValue(work);
         });
      }

      @Override
      public int getColumnCount() {
         return columnNames.length;
      }

      @Override
      public int getRowCount() {
         return rows.size();
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
         Row row = rows.get(rowIndex);
         return switch (columnIndex) {
            case SKIP_COLUMN -> row.skip;
            case NAME_COLUMN -> row;
            case SIZE_COLUMN -> sizeFormat.format(row.size);
            case LAST_MODIFIED_COLUMN -> dateFormat.format(Instant.ofEpochMilli(row.lastModified));
            case STATUS_COLUMN -> row;
            default -> throw new IllegalArgumentException(Integer.toString(columnIndex));
         };
      }

      @Override
      public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
         assert columnIndex == SKIP_COLUMN : columnIndex;
         rows.get(rowIndex).setSkip((Boolean) aValue);
      }

      @Override
      public String getColumnName(int column) {
         return columnNames[column];
      }

      @Override
      public Class<?> getColumnClass(int columnIndex) {
         return columnClasses[columnIndex];
      }

      @Override
      public boolean isCellEditable(int rowIndex, int columnIndex) {
         return columnIndex == 0;
      }
   }

   private final class NameCellRenderer extends DefaultTableCellRenderer {
      private NameCellRenderer() {
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         RawFileTableModel.Row tableRow = (RawFileTableModel.Row) value;
         Collection<DataFileLabel> labels = dataFileLabelling != null
               ? Utils.sorted(dataFileLabelling.getLabels(tableRow.segmentHandle))
               : List.of();
         String text = DataFileLabelUtils.addLabelsText(tableRow.segmentHandle.getDisplayName(), labels);
         return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
      }
   }

   private static final class StatusCellRenderer extends DefaultTableCellRenderer {
      private RawFileTableModel.@Nullable Row tableRow;

      private StatusCellRenderer() {
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         tableRow = (RawFileTableModel.Row) value;
         switch (tableRow.status) {
            case New, Pending, Reprocess -> {
               // No fill => Can render cell as selected.
            }
            default -> {
               // Filled => Do not also render as selected.
               isSelected = false;
            }
         }
         return super.getTableCellRendererComponent(table, tableRow.getStatusString(), isSelected, hasFocus, row, column);
      }

      @Override
      protected void paintComponent(Graphics g) {
         if (tableRow != null) {
            switch (tableRow.status) {
               case New, Pending, Reprocess -> {
                  // No fill.
               }
               case Processing -> {
                  fill(g, ColorUtils.PALEGREEN, tableRow.progress);
               }
               case Done -> {
                  fill(g, ColorUtils.PALEGREEN, 1);
               }
               case RemoteProcessing -> {
                  fill(g, ColorUtils.YELLOW, 1);
               }
               case Error -> {
                  fill(g, ColorUtils.TOMATO, 1);
               }
            }
         }
         super.paintComponent(g);
      }

      private void fill(Graphics g, Color color, float fraction) {
         g.setColor(color);
         g.fillRect(0, 0, (int) (fraction * getWidth()), getHeight());
      }
   }

   public void shutDown() {
      stop();
      rowInfoAsyncHandle.cancel();
      koronaHelpSystem.close();
      frame.dispose();
      if (startedAsMainApplication) {
         LOGGING_MANAGER.shutDown();
      }
   }

   public static KoronaRelay start(Map<String, String> env) {
      KoronaRelay koronaRelay = new KoronaRelay(env);
      KoronaSettingsUtils.showMainConfigDirDialogIfNecessary(koronaRelay.korona, koronaRelay.frame, Path.of(""));
      return koronaRelay;
   }

   public static void main(String[] args) {
      long t0 = System.currentTimeMillis();

      startedAsMainApplication = true;

      Utils.init(args, KoronaResource.KORONA_64);
      if (Utils.isTestRun()) {
         KoronaRelaySmoke.main(args);
         return;
      }

      LOGGING_MANAGER.startLogging();

      SwingUtilities.invokeLater(() -> {
         KoronaRelay koronaRelay = start(System.getenv());
         long t1 = System.currentTimeMillis();
         Log.global.info("Startup time: " + (t1 - t0) / 1000f + " sec");

         if (Arrays.asList(args).contains("--start-processing")) {
            if (koronaRelay.playButton.isEnabled()) {
               koronaRelay.playButton.doClick();
            }
         }
      });
   }
}
