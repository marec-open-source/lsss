package no.imr.lsss.database.reports;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseReportManager;
import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.PlatformName;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.tables.hibernate.SurveyPK;
import no.imr.lsss.database.types.DatabasePlugin;
import no.imr.lsss.database.util.SurveySelectionDialog;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.resources.LsssHelp;
import no.imr.lsss.viewer.Shortcuts;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.queries.QueryBuilder;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.Listener;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.OptionalIntParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueConverters;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.gui.ParameterEditorData;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.RadioButtonTabbedPane;
import no.imr.tools.swing.WorkerDialog;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.jspecify.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public final class ReportGenerator {
   private static final List<Integer> REPORTS_STANDARD = List.of(/* Multi species: */ 0, 2, 3, 16, /* Single species: */ 4, 5, 7, 8, 9, 11);
   public static final List<Integer> REPORTS_TIME = List.of(20, 21, 22, 23, 24, 25, 26); // Also update commands.html and DatabaseResource.
   private static final List<Integer> REPORTS_OBJECT = List.of(30, 31, 32);
   private static final List<Integer> REPORTS_TOTAL = List.of(40);
   private static final List<List<Integer>> REPORT_GROUPS = List.of(REPORTS_STANDARD, REPORTS_TIME, REPORTS_OBJECT, REPORTS_TOTAL);
   private static final List<Integer> REPORTS_ALL = REPORT_GROUPS.stream()
         .flatMap(List::stream)
         .sorted()
         .toList();

   private static final String REPORTS_SUB_DIR_PREFIX = "Reports_";
   public static final String SCRATCH_FILE_PREFIX = "scratch_";
   private static final String REPORT_SETTINGS_FILE_NAME = SCRATCH_FILE_PREFIX + "ReportSettings";

   private final LSSS mLSSS;
   private final JDialog dialog;
   private static final int MIN_START_DATE = 11000101;
   private static final int MAX_STOP_DATE = 33001231;
   private static final float DEFAULT_STOP_DISTANCE = 987654;
   private static final int DEFAULT_NO_FREQUENCIES = 6;
   private int mNoFrequencies = DEFAULT_NO_FREQUENCIES; //Default
   private final JCheckBox[] wReport = new JCheckBox[REPORTS_ALL.getLast() + 1];

   private final JButton generateReportsButton = new JButton("Generate");
   private final JButton deleteReportsButton = new JButton("Remove old reports");

   private final OptionalIntParameter mStartDate = new OptionalIntParameter(
         new Name("startDate", "Start date"),
         Optional.empty(), Unit.NONE, ValueConstraints.gte(0));

   private final OptionalIntParameter mStopDate = new OptionalIntParameter(
         new Name("stop", "Stop date"),
         Optional.empty(), Unit.NONE, ValueConstraints.gte(0));

   private final OptionalIntParameter mStartTime = new OptionalIntParameter(
         new Name("startTime", "Start time"),
         Optional.empty(), Unit.NONE, ValueConstraints.gte(0));

   private final OptionalIntParameter mStopTime = new OptionalIntParameter(
         new Name("stopTime", "Stop time"),
         Optional.empty(), Unit.NONE, ValueConstraints.gte(0));

   private final OptionalFloatParameter mStartDistance = new OptionalFloatParameter(
         new Name("startDistance", "Start distance"),
         Optional.empty(), Unit.NAUTICAL_MILES, ValueConstraints.gte(0f));

   private final OptionalFloatParameter mStopDistance = new OptionalFloatParameter(
         new Name("stopDistance", "Stop distance"),
         Optional.empty(), Unit.NAUTICAL_MILES, ValueConstraints.gte(0f));

   private final JRadioButton timeCheck = new JRadioButton("Time [hhmmss]", true);
   private final JRadioButton distanceCheck = new JRadioButton("Distance [nmi]");
   private final JCheckBox printScrutinizedSpCheck
         = new JCheckBox("Print all scrutinized species. (Otherwise, use selection in \"Acoustic categories\" window.)", true);
   private final JCheckBox schoolReportCheck = new JCheckBox("Use schools only (i.e. scatter data scrutinized as school objects - closed curve with 'x' inside)", false);
   private final JCheckBox distanceFileExtensionCheck = new JCheckBox("Use distance file extension in report file-names", false);
   private final JCheckBox extinctionCheck = new JCheckBox("Correct for extinction (for species with specified sigma_e)", false);
   private final JCheckBox wAccumulateCheck = new JCheckBox("Print at reduced resolution (not 20-40).", false);
   private final JLabel mAccumulateDistanceLabel = new JLabel("Accumulate distance [nmi]:");

   private final FloatParameter mAccumulateDistance = new FloatParameter(
         new Name("accumulateDistance", "Accumulate distance"),
         ReportEngine.DEFAULT_ACCUMULATE_DISTANCE, Unit.NAUTICAL_MILES, ValueConstraints.gt(0f));

   private final OptionalIntParameter mMaxSpecies = new OptionalIntParameter(
         new Name("maxSpecies", "Max species"),
         Optional.empty(), Unit.COUNT, ValueConstraints.gte(1));

   private final OptionalIntParameter mMaxPrintPelagicCh = new OptionalIntParameter(
         new Name("maxPrintPelagicCh", "Max print pelagic channels"),
         Optional.empty(), Unit.COUNT, ValueConstraints.gte(1));

   private final OptionalIntParameter mMaxPrintBottomCh = new OptionalIntParameter(
         new Name("maxPrintBottomCh", "Max print bottom channels"),
         Optional.empty(), Unit.COUNT, ValueConstraints.gte(0));
   private final @Nullable Path mCurrentSurveyReportsDirectory;
   private final ReportEngine mReportEngine;

   public ReportGenerator(LSSS aLSSS) {
      mLSSS = aLSSS;
      mCurrentSurveyReportsDirectory = aLSSS.getConfigurationManager().getDataConf().getDir(DataConfLSSS.REPORTS_DIR).getFile();
      mReportEngine = new ReportEngine(aLSSS);

      for (int i = 0; i < wReport.length; i++) {
         String name = (i == 0) ? "Compact" : Integer.toString(i);
         wReport[i] = new JCheckBox(name);
      }

      loadSettings();

      dialog = new JDialog(mLSSS.getFrame(), "Report generator", Dialog.ModalityType.DOCUMENT_MODAL);
      int mVerticalSpacing = 10;
      JPanel mainPanel = new JPanel();
      mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

      ParameterEditorData parameterEditorData = new ParameterEditorData(mainPanel, new GUIConfig(),
            List.of(
                  mStartDate, mStopDate,
                  mStartTime, mStopTime,
                  mStartDistance, mStopDistance,
                  mMaxSpecies,
                  mMaxPrintPelagicCh,
                  mMaxPrintBottomCh,
                  mAccumulateDistance));

      // Name of database and report directory
      GridBag infoGridBag = new GridBag();
      //infoGridBag.getConstraints().gridwidth = 2;
      infoGridBag.getConstraints().anchor = GridBagConstraints.WEST;
      infoGridBag.getConstraints().insets = new Insets(0, 5, 2, 0);
      infoGridBag.add(new JLabel("Database connection: "));
      JLabel connectionLabel;
      DatabasePlugin databasePlugin = mLSSS.getDatabaseManager().getConnectionManager().getDatabasePlugin();
      if (databasePlugin != null && mLSSS.getDatabaseManager().getDatabaseConnection().isConnected()) {
         Configuration configuration = databasePlugin.getConfiguration(ConnectionType.CONNECT);
         connectionLabel = new JLabel(databasePlugin.getName().displayName() + ": " + configuration.getProperty(Environment.JAKARTA_JDBC_URL));
      } else {
         connectionLabel = new JLabel("*** Database not connected ***");
         connectionLabel.setForeground(Color.RED);
      }
      infoGridBag.addWithLineBreak(connectionLabel);

      JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      infoPanel.add(infoGridBag.getPanel());
      mainPanel.add(infoPanel);

      List<Survey> surveys = fetchSurveysFromDatabase();
      boolean scatterExistInDB = scatterExist();

      if (surveys.isEmpty()) {
         JLabel noSurveyMessage = new JLabel("*** No surveys in database ***", JLabel.LEFT);
         noSurveyMessage.setForeground(Color.RED);
         infoGridBag.addWithLineBreak(noSurveyMessage);
      } else if (!scatterExistInDB) { // Surveys exist, but nothing scrutinized
         JLabel noDataMessage = new JLabel("*** No scatter data in database ***", JLabel.LEFT);
         noDataMessage.setForeground(Color.RED);
         infoGridBag.addWithLineBreak(noDataMessage);
      }

      Survey currentSurvey = mLSSS.getConfigurationManager().getSurveyConf().getSurvey();

      FileParameter currentSurveyReportsDir = new FileParameter(new Name("ReportsDirectory", "Reports directory"),
            mLSSS.getConfigurationManager().getDataConf().getDir(DataConfLSSS.REPORTS_DIR).getFile(),
            FileParameter.Mode.DIRECTORY);
      JPanel currentSurveyPanel = new JPanel(new BorderLayout());
      currentSurveyPanel.setBorder(BorderFactory.createEmptyBorder(mVerticalSpacing, 5, 0, 5));
      currentSurveyPanel.add(new ParameterEditor(List.of(currentSurveyReportsDir)).getEditorComponent());
      JLabel currentSurveyLabel = new JLabel("Current survey: " + (currentSurvey == null ? "Not selected" :
            currentSurvey.getSurveyTitle() + " (" + currentSurvey.getCompId().getSurvey() + ")"
                  + " / " + currentSurvey.getPlatform().findPlatformName(currentSurvey) + " (" + currentSurvey.getPlatform().getCompId().getPlatform() + ")"
                  + " / " + currentSurvey.getPlatform().getNation().getNationName()));
      currentSurveyLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
      currentSurveyPanel.add(currentSurveyLabel, BorderLayout.SOUTH);

      Set<Survey> selectedSurveys = new HashSet<>();

      FileParameter selectSurveysReportsDir = new FileParameter(new Name("ReportsDirectory", "Reports directory"),
            mLSSS.getConfigurationManager().getApplicationConfiguration().getDirectoryConf().getMainDir().resolve("DatabaseReports"),
            FileParameter.Mode.DIRECTORY);
      JPanel selectSurveysTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
      JLabel surveysSelectedLabel = new JLabel("0 surveys selected");
      selectSurveysTopPanel.add(surveysSelectedLabel);
      selectSurveysTopPanel.add(Box.createHorizontalStrut(5));
      JButton selectSurveysButton = new JButton("Select surveys...");
      selectSurveysTopPanel.add(selectSurveysButton);
      selectSurveysTopPanel.add(Box.createHorizontalStrut(20));
      selectSurveysTopPanel.add(new JLabel("Reports for each survey in separate subdirectory"));
      JPanel selectSurveysPanel = new JPanel(new BorderLayout());
      selectSurveysPanel.setBorder(BorderFactory.createEmptyBorder(mVerticalSpacing, 5, 0, 5));
      selectSurveysPanel.add(new ParameterEditor(List.of(selectSurveysReportsDir)).getEditorComponent());
      selectSurveysPanel.add(selectSurveysTopPanel, BorderLayout.SOUTH);

      JTabbedPane tabbedPane = new RadioButtonTabbedPane()
            .add("Current survey", currentSurveyPanel)
            .add("Select surveys", selectSurveysPanel)
            .getTabbedPane();
      tabbedPane.setBorder(BorderFactory.createEmptyBorder(mVerticalSpacing, 5, 0, 5));
      mainPanel.add(tabbedPane);

      AtomicBoolean currentSurveySelected = new AtomicBoolean(true);

      Listener buttonUpdateListener = () -> {
         Path dir;
         Set<Survey> reportSurveys;
         if (currentSurveySelected.get()) {
            dir = currentSurveyReportsDir.getFile();
            reportSurveys = currentSurvey != null ? Set.of(currentSurvey) : Set.of();
         } else {
            dir = selectSurveysReportsDir.getFile();
            reportSurveys = selectedSurveys;
         }
         generateReportsButton.setEnabled(!reportSurveys.isEmpty() && scatterExistInDB && dir != null);
         deleteReportsButton.setEnabled(dir != null);
      };
      buttonUpdateListener.addToAndNotify(
            currentSurveyReportsDir,
            selectSurveysReportsDir
      );
      selectSurveysButton.addActionListener(_ -> {
         new SurveySelectionDialog(surveys, selectedSurveys, dialog).show();
         surveysSelectedLabel.setText(selectedSurveys.size() + " surveys selected");
         buttonUpdateListener.listen();
      });

      tabbedPane.addChangeListener(_ -> {
         boolean current = tabbedPane.getSelectedIndex() == 0;
         currentSurveySelected.set(current);
         distanceCheck.setEnabled(current);
         if (current) {
            generateReportsButton.setEnabled(currentSurvey != null);
         } else {
            generateReportsButton.setEnabled(!selectedSurveys.isEmpty());
            timeCheck.setSelected(true);
         }
         buttonUpdateListener.listen();
      });

      if (currentSurvey == null) {
         tabbedPane.setSelectedIndex(1);
      }

      GuiUtils.createButtonGroup(timeCheck, distanceCheck);

      timeCheck.setHorizontalAlignment(SwingConstants.CENTER);
      distanceCheck.setHorizontalAlignment(SwingConstants.CENTER);

      ActionListener timeDistanceListener = _ -> {
         boolean timeSelected = timeCheck.isSelected();
         mStartTime.setEnabled(timeSelected);
         mStopTime.setEnabled(timeSelected);
         mStartDistance.setEnabled(!timeSelected);
         mStopDistance.setEnabled(!timeSelected);
         parameterEditorData.update();
      };
      timeDistanceListener.actionPerformed(null);
      timeCheck.addActionListener(timeDistanceListener);
      distanceCheck.addActionListener(timeDistanceListener);

      wAccumulateCheck.addActionListener(_ -> {
         boolean accumulate = wAccumulateCheck.isSelected();
         mAccumulateDistanceLabel.setEnabled(accumulate);
         mAccumulateDistance.setEnabled(accumulate);
         parameterEditorData.update();
      });

      GridBag startStopGridBag = new GridBag();
      startStopGridBag.getPanel().setBorder(BorderFactory.createEmptyBorder(mVerticalSpacing, 5, mVerticalSpacing, 5));
      startStopGridBag.getConstraints().gridwidth = 4;

      startStopGridBag.add(new JLabel(""));
      startStopGridBag.activateHorizontalFill();
      startStopGridBag.add(new JLabel("Date [YYYYMMDD]", SwingConstants.CENTER));
      startStopGridBag.add(timeCheck);
      startStopGridBag.addWithLineBreak(distanceCheck);
      startStopGridBag.deactivateFill();

      JLabel startLabel = new JLabel("Start:");
      startLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
      startStopGridBag.add(startLabel);
      startStopGridBag.activateHorizontalFill();
      startStopGridBag.add(parameterEditorData.getInputComponent(mStartDate));
      startStopGridBag.add(parameterEditorData.getInputComponent(mStartTime));
      startStopGridBag.addWithLineBreak(parameterEditorData.getInputComponent(mStartDistance));
      startStopGridBag.deactivateFill();

      startStopGridBag.activateHorizontalFill();
      startStopGridBag.getConstraints().weightx = 0;
      startStopGridBag.add(new JLabel("Stop:"));
      startStopGridBag.getConstraints().weightx = 1;
      startStopGridBag.add(parameterEditorData.getInputComponent(mStopDate));
      startStopGridBag.add(parameterEditorData.getInputComponent(mStopTime));
      startStopGridBag.addWithLineBreak(parameterEditorData.getInputComponent(mStopDistance));
      startStopGridBag.deactivateFill();

      mainPanel.add(startStopGridBag.getPanel());

      // Panel for general print options
      JPanel generalPanel = new JPanel();
      BoxLayout generalPanelLayout = new BoxLayout(generalPanel, BoxLayout.Y_AXIS);
      generalPanel.setLayout(generalPanelLayout);
      mainPanel.add(generalPanel);

      // Select number of species for special reports
      JPanel noSpeciesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      JLabel noSpeciesLabel = new JLabel("Max number of species in \"single species\" reports:  ");
      noSpeciesPanel.add(noSpeciesLabel);
      JTextField maxSpeciesComponent = (JTextField) parameterEditorData.getInputComponent(mMaxSpecies);
      maxSpeciesComponent.setColumns(5);
      noSpeciesPanel.add(maxSpeciesComponent);
      generalPanel.add(noSpeciesPanel);
      mainPanel.add(generalPanel);

      // Checkbox for selecting all species scrutinized (alternative: use those selected in "Acoustic categories" window)
      JPanel scrutinizeAllPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      scrutinizeAllPanel.add(printScrutinizedSpCheck);
      generalPanel.add(scrutinizeAllPanel);

      // Select accumulate checkbox for reduced resolution in reports
      JPanel accumulatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      accumulatePanel.add(wAccumulateCheck);
      accumulatePanel.add(mAccumulateDistanceLabel);
      mAccumulateDistanceLabel.setEnabled(false);
      mAccumulateDistance.setEnabled(false);
      JTextField accumulateDistanceComponent = (JTextField) parameterEditorData.getInputComponent(mAccumulateDistance);
      accumulateDistanceComponent.setColumns(5);
      accumulatePanel.add(accumulateDistanceComponent);
      mainPanel.add(accumulatePanel);

      // Select max number of pelagic and bottom channels: currently used by reports 4, 9 and 11.
      JPanel noMaxPelBotPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      JLabel noMaxPelagicLabel = new JLabel("Max number of channels in reports 4, 9, 11.   Pelagic:  ");
      noMaxPelBotPanel.add(noMaxPelagicLabel);
      JTextField maxPelagicComponent = (JTextField) parameterEditorData.getInputComponent(mMaxPrintPelagicCh);
      maxPelagicComponent.setColumns(5);
      noMaxPelBotPanel.add(maxPelagicComponent);
      JLabel noMaxBottomLabel = new JLabel("    Bottom:  ");
      noMaxPelBotPanel.add(noMaxBottomLabel);
      JTextField maxBottomComponent = (JTextField) parameterEditorData.getInputComponent(mMaxPrintBottomCh);
      maxBottomComponent.setColumns(5);
      noMaxPelBotPanel.add(maxBottomComponent);
      generalPanel.add(noMaxPelBotPanel);
      mainPanel.add(generalPanel);

      //Echosounder report settings
      JPanel echosounderPanel = new JPanel();
      BoxLayout echosounderLayout = new BoxLayout(echosounderPanel, BoxLayout.Y_AXIS);
      echosounderPanel.setLayout(echosounderLayout);
      mainPanel.add(headerPanel(new JLabel("Echosounder reports")));
      mainPanel.add(echosounderPanel);

      // Mark correct for extinction (shadow effect)
      JPanel extinctionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      extinctionPanel.add(extinctionCheck);
      //echosounderPanel.add(extinctionPanel);

      // Format of printed reports
      JPanel reportFormatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      reportFormatPanel.add(schoolReportCheck);
      echosounderPanel.add(reportFormatPanel);

      // Distance extension in (most of) the filenames
      JPanel distancePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      distancePanel.add(distanceFileExtensionCheck);
      echosounderPanel.add(distancePanel);

      // Which reports to be generated
      JPanel reportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

      JCheckBox selectAllCheckBox = new JCheckBox("Select all reports", false);
      selectAllCheckBox.addActionListener(_ -> {
         for (JCheckBox reportCheckBox : wReport) {
            reportCheckBox.setSelected(selectAllCheckBox.isSelected());
         }
      });
      ActionListener updateSelectAllCheckBox = _ -> {
         selectAllCheckBox.setSelected(Arrays.stream(wReport).allMatch(AbstractButton::isSelected));
      };
      updateSelectAllCheckBox.actionPerformed(null);

      reportPanel.add(selectAllCheckBox);

      reportPanel.add(Box.createHorizontalStrut(10));

      for (JCheckBox checkBox : wReport) {
         checkBox.addActionListener(updateSelectAllCheckBox);
      }
      REPORTS_ALL.forEach(i -> reportPanel.add(wReport[i]));

      echosounderPanel.add(reportPanel);

      for (DatabaseReportManager reportManager : mReportEngine.getPluginReportManagers()) {
         mainPanel.add(headerPanel(reportManager.getIcon().on(new JLabel(reportManager.getTitle()))));
         mainPanel.add(reportManager.getViewHolder().getComponent());
      }

      JButton helpButton = new JButton("Help");
      LsssHelp.REPORT_GENERATOR.enableHelpKeyOnButton(helpButton);

      generateReportsButton.addActionListener(_ -> {
         if (!parameterEditorData.commitEdits()) {
            return;
         }
         if (currentSurvey == null) {
            return;
         }

         Path directory = currentSurveySelected.get() ? currentSurveyReportsDir.getFile() : selectSurveysReportsDir.getFile();
         if (directory == null) {
            mLSSS.showError(dialog, "No output directory configured");
            return;
         }

         if (distanceCheck.isSelected()) {
            assert currentSurveySelected.get();
            int startDate = mStartDate.getValue().orElse(MIN_START_DATE);
            int stopDate = mStopDate.getValue().orElse(MAX_STOP_DATE);
            setStartFromDistance(currentSurvey, startDate, stopDate, mStartDistance.getValue().orElse(0f));
            setStopFromDistance(currentSurvey, startDate, stopDate, mStopDistance.getValue().orElse(DEFAULT_STOP_DISTANCE));
         }

         int activeReportGroups = 0;
         for (List<Integer> reportGroup : REPORT_GROUPS) {
            if (anyReportsEnabled(reportGroup)) {
               activeReportGroups++;
            }
         }

         for (DatabaseReportManager reportManager : mReportEngine.getPluginReportManagers()) {
            activeReportGroups += reportManager.getActiveReportGroupCount();
         }

         mReportEngine.setStartDate(mStartDate.getValue().orElse(MIN_START_DATE));
         mReportEngine.setStopDate(mStopDate.getValue().orElse(MAX_STOP_DATE));
         mReportEngine.setStartTime(guiTimeToDatabaseTime(mStartTime.getValue().orElse(0)));
         mReportEngine.setStopTime(guiTimeToDatabaseTime(mStopTime.getValue().orElse(24_00_00)));
         mReportEngine.setMaxSpecialReportSpecies(mMaxSpecies.getValue().orElse(Integer.MAX_VALUE));
         mReportEngine.setPrintScrutinizedSpCheck(printScrutinizedSpCheck.isSelected());
         mReportEngine.setAccumulateDistance(mAccumulateDistance.getFloatValue());
         mReportEngine.setMaxPrintPelagicCh(mMaxPrintPelagicCh.getValue().orElse(ReportEngine.DEFAULT_MAX_PRINT_PELAGIC));
         mReportEngine.setMaxPrintBottomCh(mMaxPrintBottomCh.getValue().orElse(ReportEngine.DEFAULT_MAX_PRINT_BOTTOM));
         mReportEngine.setReports(type -> wReport[type].isSelected());
         mReportEngine.setExtinctionCheck(extinctionCheck.isSelected());
         mReportEngine.setSchoolReport(schoolReportCheck.isSelected());
         mReportEngine.setDistanceFileExtension(distanceFileExtensionCheck.isSelected());
         mReportEngine.setMode(wAccumulateCheck.isSelected() ? ReportMode.ACCUMULATE : ReportMode.NATIVE);
         mReportEngine.setExpectedFrequencyCount(mNoFrequencies);

         if (mReportEngine.getStartDate() > mReportEngine.getStopDate() ||
               mReportEngine.getStartDate() == mReportEngine.getStopDate() && mReportEngine.getStartTime() >= mReportEngine.getStopTime()) {
            JOptionPane.showMessageDialog(dialog, "No data for selected interval");
            return;
         }

         ProgressView progressView = new ProgressView("Retrieving DB-data and generating reports....................", activeReportGroups)
               .useSecondaryProgress();
         JLabel reportSurveyLabel;
         JComponent workerComponent;
         Set<Survey> reportSurveys = currentSurveySelected.get() ? Set.of(currentSurvey) : selectedSurveys;
         if (reportSurveys.size() == 1) {
            reportSurveyLabel = null;
            workerComponent = progressView.getComponent();
         } else {
            JPanel panel = new JPanel(new BorderLayout());
            reportSurveyLabel = new JLabel("qwe");
            reportSurveyLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            panel.add(reportSurveyLabel, BorderLayout.NORTH);
            panel.add(progressView.getComponent());
            workerComponent = panel;
         }
         AtomicInteger feedbackMaxChannel = new AtomicInteger();
         Map<Integer, AcousticCategory> missingIcesCategories = new HashMap<>();
         Map<SurveyPK, Survey> missingIcesValues = new HashMap<>();
         new WorkerDialog(dialog, workerComponent)
               .start(aAsyncHandle -> {
                  AtomicInteger surveyCounter = new AtomicInteger();
                  for (Survey survey : reportSurveys) {
                     surveyCounter.incrementAndGet();
                     if (reportSurveyLabel != null) {
                        SwingUtilities.invokeLater(() -> {
                           reportSurveyLabel.setText("Survey " + surveyCounter.get() + "/" + reportSurveys.size()
                                 + ": " + survey.getSurveyTitle() + " (" + survey.getCompId().getSurvey() + ")");
                        });
                     }
                     progressView.setMainProgress(0, "");
                     Path dir = currentSurveySelected.get()
                           ? directory
                           : directory.resolve(REPORTS_SUB_DIR_PREFIX + mLSSS.getConfigurationManager().getApplicationConfiguration().getDirectoryConf().getSelectedSurveyDirStructure().getSurveyDirName(survey));
                     FileUtils.createDirectories(dir);
                     ReportEngine.Feedback feedback = mReportEngine.printReports(survey, dir, progressView, aAsyncHandle);
                     if (aAsyncHandle.isCancelled()) {
                        return;
                     }
                     feedbackMaxChannel.accumulateAndGet(feedback.getMaxChannel(), Math::max);
                     mNoFrequencies = feedback.getActualNoFrequencies(); //For progress bar only
                     missingIcesCategories.putAll(feedback.getMissingIcesCategories());
                     missingIcesValues.putAll(feedback.getMissingIcesValues());
                  }
                  saveSettings();
               });
         GridBag doneGridBag = new GridBag()
               .configureVerticalBox();
         boolean warning = false;
         doneGridBag.add(new JLabel("Report generation finished."));

         if (feedbackMaxChannel.get() > PrintData.MAX_DEPTH_CHANNEL) {
            warning = true;
            doneGridBag.add(Box.createVerticalStrut(10));
            JLabel label = new JLabel("<html>Not enough depth channels to retrieve all data.  Found "
                  + feedbackMaxChannel.get() + ", but max " + PrintData.MAX_DEPTH_CHANNEL + " allowed."
                  + "<br>Change depth channel thickness and restore data to database to overcome problem.");
            label.setForeground(Color.RED);
            doneGridBag.add(label);
         }

         if (!missingIcesCategories.isEmpty()) {
            warning = true;
            doneGridBag.add(Box.createVerticalStrut(10));
            HtmlStringBuilder builder = new HtmlStringBuilder().text("Acoustic categories mapped to ICES category \"" + GetIces.UNKNOWN_ACOUSTIC_CATEGORY + "\":");
            missingIcesCategories.values().stream()
                  .sorted(mReportEngine.getLanguageUtils().acousticCategoryComparator())
                  .forEach(a -> builder.html("<br>").text(a.getCompId().getAcousticCategory() + ": " + mReportEngine.getLanguageUtils().getAcCatName(a)));
            JLabel label = new JLabel(builder.build());
            label.setForeground(Color.RED);
            doneGridBag.add(label);
         }

         AtomicReference<JDialog> doneDialogRef = new AtomicReference<>();

         if (!missingIcesValues.isEmpty()) {
            warning = true;
            doneGridBag.add(Box.createVerticalStrut(10));
            HtmlStringBuilder builder = new HtmlStringBuilder().text("Surveys with missing ICES acoustic metadata:");
            missingIcesValues.values().stream()
                  .map(survey -> survey.getSurveyTitle() + " (" + survey.getCompId().getSurvey() + ")")
                  .sorted()
                  .forEach(text -> builder.html("<br>").text(text));
            JLabel label = new JLabel(builder.build());
            label.setForeground(Color.RED);
            doneGridBag.add(label);
            doneGridBag.add(Box.createVerticalStrut(10));
            JTextPane component = GuiUtils.labelLikeHtmlTextPane("Go to configuration of <a href='ices'>ICES acoustic metadata</a>.", _ -> {
               doneDialogRef.get().dispose();
               dialog.dispose();
               mLSSS.getConfigurationManager().getSurveyMiscConf().getIcesConf().showInConfigurationDialog();
            });
            doneGridBag.add(component);
         }

         JOptionPane doneOptionPane = new JOptionPane(doneGridBag.getPanel(), warning ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
         JDialog doneDialog = doneOptionPane.createDialog(dialog, "Message");
         doneDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
         doneDialogRef.set(doneDialog);
         doneDialog.setVisible(true);
      });

      deleteReportsButton.addActionListener(_ -> {
         if (!parameterEditorData.commitEdits()) {
            return;
         }
         Path directory = currentSurveySelected.get() ? currentSurveyReportsDir.getFile() : selectSurveysReportsDir.getFile();
         if (directory == null) {
            return;
         }
         new WorkerDialog(dialog, "Deleting reports")
               .start(asyncHandle -> {
                  if (currentSurveySelected.get()) {
                     deleteReportFiles(directory);
                  } else {
                     for (Path subDir : FileUtils.listFiles(directory, asyncHandle, file -> file.getFileName().toString().startsWith(REPORTS_SUB_DIR_PREFIX))) {
                        deleteReportFiles(subDir);
                        FileUtils.deleteDirectoryIfEmpty(subDir);
                     }
                  }
               });
      });

      JButton cancelButton = new JButton("Exit");
      GuiUtils.setAccelerator(cancelButton, Shortcuts.ESCAPE);
      cancelButton.addActionListener(_ -> {
         if (!parameterEditorData.commitEdits()) {
            return;
         }
         dialog.dispose();
      });

      JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonsPanel.add(generateReportsButton);
      buttonsPanel.add(deleteReportsButton);
      buttonsPanel.add(cancelButton);
      buttonsPanel.add(helpButton);

      JPanel dialogPanel = new JPanel(new BorderLayout());
      JScrollPane dialogScrollPane = new JScrollPane(mainPanel);
      dialogPanel.add(dialogScrollPane);
      dialogPanel.add(buttonsPanel, BorderLayout.SOUTH);

      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.getRootPane().setDefaultButton(generateReportsButton);
      dialog.getContentPane().add(dialogPanel);
      dialog.pack();
      GuiUtils.expandSizeWith(dialog, dialogScrollPane.getVerticalScrollBar().getPreferredSize().width, 0);
      dialog.setLocationRelativeTo(mLSSS.getFrame());
      GuiUtils.clampToScreen(dialog);
      dialog.setVisible(true);
   }

   private static JPanel headerPanel(JLabel label) {
      JPanel panel = new JPanel(new BorderLayout());
      panel.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 5));
      panel.add(new JSeparator(), BorderLayout.NORTH);
      label.setFont(label.getFont().deriveFont(Font.BOLD));
      label.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
      panel.add(label);
      return panel;
   }

   //For PROMUS and other reports
   public JDialog getDialog() {
      return dialog;
   }

   private boolean anyReportsEnabled(List<Integer> reports) {
      return reports.stream().anyMatch(i -> wReport[i].isSelected());
   }

   private List<Survey> fetchSurveysFromDatabase() {
      return mLSSS.getDatabaseManager().getDatabaseConnection().executeValuedQuery(session -> {
         List<Survey> result = LsssQuery.fetch(Survey.class).executeAndGetValue(session);
         for (Survey survey : result) {
            // Get associated objects while session is open.

            //noinspection ResultOfMethodCallIgnored
            survey.getPlatform().getNation().getNationName();

            for (PlatformName platformName : survey.getPlatform().getPlatformNames()) {
               //noinspection ResultOfMethodCallIgnored
               platformName.getPlatformName();
            }
         }
         return result;
      });
   }

   private void setStartFromDistance(Survey aSurvey, int aStartDate, int aStopDate, float aStartDistance) {
      mLSSS.getDatabaseManager().getDatabaseConnection().executeStatelessQuery(session -> {
         String aQuery = " from Observation a " +
               " where a.compId.nation   = " + aSurvey.getCompId().getNation() +
               " and   a.compId.platform = " + aSurvey.getCompId().getPlatform() +
               " and   a.compId.survey   = " + aSurvey.getCompId().getSurvey() +
               " and   a.compId.observationDate >= " + aStartDate +
               " and   a.compId.observationDate <= " + aStopDate +
               " and   a.distance >= " + aStartDistance +
               " order by a.compId.observationDate, a.compId.observationTime";

         try (ScrollableResults<Observation> observationResults = session.createSelectionQuery(aQuery, Observation.class)
               .setReadOnly(true)
               .scroll(ScrollMode.FORWARD_ONLY)) {
            if (observationResults.next()) {
               Observation obs = observationResults.get();
               mStartDate.setIntValue(obs.getCompId().getObservationDate());
               mStartTime.setIntValue(databaseTimeToGuiTime(obs.getCompId().getObservationTime()));
               mStartDistance.setFloatValue(obs.getDistance());
            }
         }
      });
   }

   private void setStopFromDistance(Survey aSurvey, int aStartDate, int aStopDate, float aStopDistance) {
      mLSSS.getDatabaseManager().getDatabaseConnection().executeStatelessQuery(session -> {
         String aQuery = " from Observation a " +
               " where a.compId.nation   = " + aSurvey.getCompId().getNation() +
               " and   a.compId.platform = " + aSurvey.getCompId().getPlatform() +
               " and   a.compId.survey   = " + aSurvey.getCompId().getSurvey() +
               " and   a.compId.observationDate >= " + aStartDate +
               " and   a.compId.observationDate <= " + aStopDate +
               " and   a.distance <= " + aStopDistance +
               " order by a.compId.observationDate desc, a.compId.observationTime desc";

         try (ScrollableResults<Observation> observationResults = session.createSelectionQuery(aQuery, Observation.class)
               .setReadOnly(true)
               .scroll(ScrollMode.FORWARD_ONLY)) {
            if (observationResults.next()) {
               Observation obs = observationResults.get();
               mStopDate.setIntValue(obs.getCompId().getObservationDate());
               mStopTime.setIntValue(databaseTimeToGuiTime(obs.getCompId().getObservationTime()));
               mStopDistance.setFloatValue(obs.getDistance());
            }
         }
      });
   }

   private boolean scatterExist() {
      return mLSSS.getDatabaseManager().getDatabaseConnection().executeStatelessValuedQuery(
            QueryBuilder.count(Scatter.class).build()) > 0;
   }

   private void deleteReportFiles(Path aDirectory) throws IOException {
      for (Path file : FileUtils.listFiles(aDirectory)) {
         String fileName = file.getFileName().toString();
         if (fileName.startsWith("List") || fileName.startsWith(SCRATCH_FILE_PREFIX)) {
            try {
               Files.deleteIfExists(file);
            } catch (IOException e) {
               Log.global.log(Level.WARNING, "Error deleting " + file, e);
            }
         }
      }
      for (DatabaseReportManager reportManager : mReportEngine.getPluginReportManagers()) {
         reportManager.deleteReports(aDirectory);
      }
   }

   private void saveSettings() {
      if (mCurrentSurveyReportsDirectory == null) {
         return;
      }
      try (PrintWriter out = FileUtils.newPrintWriter(mCurrentSurveyReportsDirectory.resolve(REPORT_SETTINGS_FILE_NAME), mReportEngine.getCharset())) {
         out.println("StartDate: " + mStartDate.getStringValue());
         out.println("StartTime: " + ValueConverters.OPTIONAL_INTEGER.stringify(mStartTime.getValue().map(ReportGenerator::guiTimeToDatabaseTime)));
         out.println("StartDistance: " + mStartDistance.getStringValue());
         out.println("StopDate: " + mStopDate.getStringValue());
         out.println("StopTime: " + ValueConverters.OPTIONAL_INTEGER.stringify(mStopTime.getValue().map(ReportGenerator::guiTimeToDatabaseTime)));
         out.println("StopDistance: " + mStopDistance.getStringValue());
         out.println("SpeciesInSingleSpeciesReports: " + mMaxSpecies.getStringValue());
         out.println("NoFrequencies: " + mNoFrequencies);
         out.println("AccumulateDistance: " + mAccumulateDistance.getStringValue());
         for (int i = 0; i < wReport.length; i++) {
            if (wReport[i].isSelected()) {
               out.println("Report: " + i);
            }
         }
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error", e);
      }
   }

   private void loadSettings() {
      if (mCurrentSurveyReportsDirectory == null) {
         return;
      }
      Path reportSettingsFile = mCurrentSurveyReportsDirectory.resolve(REPORT_SETTINGS_FILE_NAME);
      if (!Files.exists(reportSettingsFile)) {
         return;
      }
      try (BufferedReader in = Files.newBufferedReader(reportSettingsFile, mReportEngine.getCharset())) {
         while (true) {
            String line = in.readLine();
            if (line == null) {
               break;
            }
            int startIndex = line.indexOf(':') + 2;
            String stringValue = line.substring(startIndex);
            if (line.startsWith("StartDate: ")) mStartDate.setStringValue(stringValue);
            if (line.startsWith("StartTime: ")) mStartTime.setValue(ValueConverters.OPTIONAL_INTEGER.parse(stringValue).map(ReportGenerator::databaseTimeToGuiTime));
            if (line.startsWith("StartDistance: ")) mStartDistance.setStringValue(stringValue);
            if (line.startsWith("StopDate: ")) mStopDate.setStringValue(stringValue);
            if (line.startsWith("StopTime: ")) mStopTime.setValue(ValueConverters.OPTIONAL_INTEGER.parse(stringValue).map(ReportGenerator::databaseTimeToGuiTime));
            if (line.startsWith("StopDistance: ")) mStopDistance.setStringValue(stringValue);
            if (line.startsWith("SpeciesInSingleSpeciesReports: ")) mMaxSpecies.setStringValue(stringValue);
            if (line.startsWith("MaxPrintPelagicCh: ")) mMaxPrintPelagicCh.setStringValue(stringValue);
            if (line.startsWith("MaxPrintBottomCh: ")) mMaxPrintBottomCh.setStringValue(stringValue);
            if (line.startsWith("NoFrequencies: ")) mNoFrequencies = Integer.parseInt(stringValue);
            if (line.startsWith("AccumulateDistance: ")) mAccumulateDistance.setStringValue(stringValue);
            if (line.startsWith("Report: ")) {
               int i = Integer.parseInt(stringValue);
               if (i >= 0 && i < wReport.length) {
                  wReport[i].setSelected(true);
               }
            }
         }
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error", e);
      }
   }

   private static int databaseTimeToGuiTime(int aDatabaseTime) {
      return aDatabaseTime / 100; // [hhmmss] in gui, [hhmmssxx] in database
   }

   private static int guiTimeToDatabaseTime(int aGuiTime) {
      return aGuiTime * 100; // [hhmmss] in gui, [hhmmssxx] in database
   }
}
