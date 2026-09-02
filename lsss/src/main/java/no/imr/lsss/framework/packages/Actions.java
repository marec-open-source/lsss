package no.imr.lsss.framework.packages;

import com.google.common.util.concurrent.Runnables;
import no.imr.korona.computation.feature.CategoryVisualizer;
import no.imr.korona.computation.feature.EchogramWindow;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.Dep0Datagram;
import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.region.CurveBoundary;
import no.imr.korona.region.ExclusionDetector;
import no.imr.korona.region.Layer;
import no.imr.korona.region.LayerBoundary;
import no.imr.korona.region.LayerManager;
import no.imr.korona.region.Region;
import no.imr.korona.region.VerticalBoundary;
import no.imr.korona.util.KoronaUtils;
import no.imr.korona.viewer.coloring.CategoryColorConverter;
import no.imr.korona.viewer.coloring.ColorConverter;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.korona.viewer.variables.categorization.CategoryVariable;
import no.imr.korona.viewer.variables.plankton.PlanktonVariable;
import no.imr.korona.viewer.variables.raw.RawVariableFactory;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.LsssRegionConfiguration;
import no.imr.lsss.framework.config.survey.misc.ices.IcesConf;
import no.imr.lsss.modules.interpretation.InterpretationModule;
import no.imr.lsss.resources.LsssIcons;
import no.imr.lsss.util.LsssUtils;
import no.imr.lsss.util.SchoolVisualizerDialog;
import no.imr.lsss.viewer.ActionsSearchDialog;
import no.imr.lsss.viewer.Shortcuts;
import no.imr.tools.NoCanDoException;
import no.imr.tools.listening.Listener;
import no.imr.tools.misc.ToFloatFunction;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ConfigurableGUI;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditorData;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.range.RangeSet;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.SimpleInputDialog;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.icons.MiscIcons;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.prefs.Preferences;

public final class Actions {
   private static final String PREFERENCE_UPPER_BOUNDARY_RANGE = "upperBoundaryRange";

   private LSSS lsss;

   public final LsssAction newSurvey = new TaskLsssAction("newSurvey", "New survey...",
         _ -> GuiUtils.invokeNowOrWait(() -> {
            if (lsss.getSurveyManager().isUnmodifiedOrUserApproved()) {
               lsss.getSurveyManager().createNew();
            }
         }))
         .setToolTipText("Create configuration file and choose work file directory for a new survey")
         .setIcon(MiscIcons.ADD);

   public final LsssAction openSurvey = new TaskLsssAction("openSurvey", "Open survey...",
         _ -> GuiUtils.invokeNowOrWait(() -> {
            if (lsss.getSurveyManager().isUnmodifiedOrUserApproved()) {
               lsss.getSurveyManager().open();
            }
         }))
         .setToolTipText("Open existing survey configuration file")
         .setIcon(MiscIcons.OPEN);

   public final LsssAction editSurvey = new TaskLsssAction("editSurvey", "Edit survey...",
         _ -> GuiUtils.invokeNowOrWait(lsss.getConfigurationManager()::showDialog))
         .setToolTipText("Edit survey configuration")
         .setIcon(MiscIcons.SETTINGS);

   public final LsssAction saveSurvey = new TaskLsssAction("saveSurvey", "Save survey",
         _ -> GuiUtils.invokeNowOrWait(lsss.getSurveyManager()::save))
         .setToolTipText("<html>Save survey configuration file and interpretation work files<br>NB: Database is not stored")
         .setIcon(MiscIcons.SAVE);

   public final LsssAction closeSurvey = new TaskLsssAction("closeSurvey", "Close survey",
         _ -> GuiUtils.invokeNowOrWait(() -> {
            if (lsss.getSurveyManager().isUnmodifiedOrUserApproved()) {
               lsss.getSurveyManager().closeByUser();
            }
         }))
         .setToolTipText("Close survey configuration file and interpretation work files");

   public final LsssAction help = new TaskLsssAction("help", "Help",
         _ -> lsss.getHelpSystem().getMainHelpSet().getTopHelpID().show())
         .setIcon(MiscIcons.HELP);

   public final LsssAction actionsDialog = new TaskLsssAction("actionsDialog", "Actions dialog",
         _ -> SwingUtilities.invokeLater(() -> new ActionsSearchDialog(lsss)));

   public final BooleanLsssAction showToolbar = new BooleanLsssAction("showToolbar", "Show toolbar", true);
   public final BooleanLsssAction showStatusBar = new BooleanLsssAction("showStatusBar", "Show status bar", true);
   public final BooleanLsssAction showTooltip = new BooleanLsssAction("showTooltip", "Show tooltip", false);

   public final BooleanLsssAction showStoredMasking = new BooleanLsssAction("showStoredMasking", "Show stored masking", true);
   public final BooleanLsssAction showCategorization = new BooleanLsssAction("showCategorization", "Show categorization", false);
   public final BooleanLsssAction showPlankton = new BooleanLsssAction("showPlankton", "Show plankton", false);
   public final BooleanLsssAction showConditionalMasking = new BooleanLsssAction("showConditionalMasking", "Show conditional masking", true);
   public final BooleanLsssAction showOnlyEchogram = new BooleanLsssAction("showOnlyEchogram", "Show only echogram", false);

   private ColorConverter lastNonCategorizationColorConverter;
   private ColorConverter lastCategorizationColorConverter;
   private ColorConverter lastPlanktonColorConverter;
   private boolean colorConverterIsChanging;

   public final LsssAction previousSegment = new TaskLsssAction("previousSegment", "Step backward to previous segment",
         _ -> lsss.getInterpretationSettings().gotoPreviousPingRange())
         .setIcon(MiscIcons.STEP_BACK);

   public final LsssAction nextSegment = new TaskLsssAction("nextSegment", "Step forward to next segment",
         _ -> lsss.getInterpretationSettings().gotoNextPingRange())
         .setIcon(MiscIcons.STEP_FORWARD);

   public final LsssAction preferredSegmentSize = new TaskLsssAction("preferredSegmentSize", "Adjust segment to preferred size",
         this::doPreferredSegmentSize);

   public final LsssAction selectNextRegion = new TaskLsssAction("selectNextRegion", "Select next visible region",
         a -> {
            if ((a.modifiers() & ActionEvent.SHIFT_MASK) == 0) {
               lsss.getRegionManager().setNextRegionSelected();
            } else {
               lsss.getRegionManager().setPreviousRegionSelected();
            }
         })
         .setIcon(MiscIcons.NAVIGATE_NEXT);

   public final LsssAction selectVisibleRegions = new TaskLsssAction("selectVisibleRegions", "Select all visible regions",
         _ -> lsss.getRegionManager().replaceSelectedRegions(lsss.getRegionManager().getVisibleRegions()));

   public final LsssAction selectVisibleRegionsNot100PercentAssignedOnCurrentFrequency = new TaskLsssAction("selectVisibleRegionsNot100PercentAssignedOnCurrentFrequency",
         "Select visible regions with total assignment ≠ 100% on current frequency",
         _ -> selectVisibleRegionsNot100PercentAssigned(Set.of(lsss.getInterpretationSettings().getChannel())));

   public final LsssAction selectVisibleRegionsNot100PercentAssignedOnStorableFrequencies = new TaskLsssAction("selectVisibleRegionsNot100PercentAssignedOnStorableFrequencies",
         "Select visible regions with total assignment ≠ 100% on storable frequencies",
         _ -> selectVisibleRegionsNot100PercentAssigned(lsss.getModuleManager().getModule(InterpretationModule.class).getChannelsToStore()));

   private boolean resetInterpretationUsingCoordinatedBottom = true;
   public final LsssAction resetInterpretation = new TaskLsssAction("resetInterpretation", "Reset interpretation...",
         _ -> resetInterpretation())
         .setIcon(MiscIcons.UNDO);

   public final LsssAction excludeLowSpeed = new TaskLsssAction("excludeLowSpeed", "Exclude pings with low speed...",
         _ -> excludeLowSpeed())
         .setIcon(LsssIcons.EXCLUDE);

   public final LsssAction mergeLayersWithSameInterpretation = new TaskLsssAction("mergeLayersWithSameInterpretation", "Merge layers with same interpretation",
         _ -> mergeLayersWithSameInterpretation())
         .setToolTipText("Merge visible neighbouring layers with same interpretation and same set of labels");

   public final LsssAction setUpperBoundaryFromRange = new TaskLsssAction("setUpperBoundaryFromRange", "Set upper boundary from range",
         _ -> setUpperBoundaryFromRange());

   public final LsssAction setUpperBoundaryFromThreshold = new TaskLsssAction("setUpperBoundaryFromThreshold", "Set upper boundary from threshold",
         _ -> setUpperBoundaryFromThreshold());

   public final LsssAction setLowerBoundaryFromThreshold = new TaskLsssAction("setLowerBoundaryFromThreshold", "Set lower boundary from threshold",
         _ -> setLowerBoundaryFromThreshold());

   public final LsssAction setLowerBoundaryFromCoordinatedBottom = new TaskLsssAction(
         "setLowerBoundaryFromCoordinatedBottom",
         "Set lower boundary from coordinated bottom",
         _ -> setLowerBoundaryFromCoordinatedBottom());

   public final LsssAction setLowerBoundaryFromCoordinatedBottomInPreprocessedData = new TaskLsssAction(
         "setLowerBoundaryFromCoordinatedBottomInPreprocessedData",
         "Set lower boundary from coordinated bottom in preprocessed data",
         _ -> setLowerBoundaryFromCoordinatedBottomInPreprocessedData());

   public final LsssAction setLowerBoundaryFromCurrentFrequencyBottom = new TaskLsssAction("setLowerBoundaryFromCurrentFrequencyBottom",
         "Set lower boundary from bottom on current frequency",
         _ -> setLowerBoundaryFromCurrentFrequencyBottom());

   public final LsssAction deleteBottomDataCurrentFrequency = new TaskLsssAction("deleteBottomDataCurrentFrequency",
         "Delete data above lower integration line and below bottom on current frequency",
         _ -> deleteBottomDataCurrentFrequency())
         .setIcon(MiscIcons.ERASE);

   public final LsssAction deleteAssignmentsOnOtherFrequencies = new TaskLsssAction("deleteAssignmentsOnOtherFrequencies", "Delete assignments on other frequencies",
         _ -> {
            if (lsss.getInterpretationSettings().isInteractiveMode()) {
               String message = "Delete assignments of acoustic categories for selected regions on all frequencies other than " + KoronaUtils.hzToKHz(lsss.getInterpretationSettings().getFrequency()) + " kHz?";
               int answer = JOptionPane.showConfirmDialog(lsss.getFrame(), message, "Delete interpretation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
               if (answer != JOptionPane.OK_OPTION) {
                  return;
               }
            }
            int channelToKeep = lsss.getInterpretationSettings().getChannel();
            int maxChannel = lsss.getInterpretationSettings().getDataFileSet().getTransducerCount();
            for (Region region : lsss.getRegionManager().getSelectedRegions()) {
               if (region.isReadOnly()) {
                  continue;
               }
               for (int channel = 1; channel <= maxChannel; channel++) {
                  if (channel != channelToKeep) {
                     region.getInterpretation().resetInterpretation(channel);
                  }
               }
            }
            lsss.getRegionManager().getInterpretationChangeManager().notifyListeners(this);
         });

   public final LsssAction deleteAssignmentsOnAllFrequencies = new TaskLsssAction("deleteAssignmentsOnAllFrequencies", "Delete assignments on all frequencies",
         _ -> {
            if (lsss.getInterpretationSettings().isInteractiveMode()) {
               String message = "Delete assignments of acoustic categories for selected regions on all frequencies?";
               int answer = JOptionPane.showConfirmDialog(lsss.getFrame(), message, "Delete interpretation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
               if (answer != JOptionPane.OK_OPTION) {
                  return;
               }
            }
            for (Region region : lsss.getRegionManager().getSelectedRegions()) {
               if (region.isReadOnly()) {
                  continue;
               }
               region.getInterpretation().reset();
            }
            lsss.getRegionManager().getInterpretationChangeManager().notifyListeners(this);
         });

   public final LsssAction showSchoolVisualizerDialog = new TaskLsssAction("showSchoolVisualizerDialog", "School visualizer dialog...",
         _ -> SwingUtilities.invokeLater(() -> new SchoolVisualizerDialog(lsss, lsss.getFrame())))
         .setIcon(MiscIcons.SCATTER_PLOT);

   public final LsssAction showCategoryEditor = new TaskLsssAction("showCategoryEditor", "Category editor...",
         _ -> SwingUtilities.invokeLater(() -> {
            try {
               EchogramWindow echogramWindow = LsssUtils.getEchogramWindow(lsss);
               if (echogramWindow != null) {
                  ConfigFileSettings configFileSettings = lsss.getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getMainSetup().createConfigFileSettings();
                  new CategoryVisualizer(echogramWindow, configFileSettings, lsss.getFrame(), Dialog.ModalityType.MODELESS);
               }
            } catch (IOException e) {
               lsss.showError("Error creating echogram window", e);
            }
         }));

   public final LsssAction reloadData = new TaskLsssAction("reloadData", "Reload data",
         _ -> lsss.getInterpretationSettings().reloadAllData())
         .setIcon(MiscIcons.REFRESH);

   public final LsssAction undo = new TaskLsssAction("undo", "Undo last school edit",
         _ -> lsss.getRegionManager().undo())
         .setIcon(MiscIcons.UNDO);

   public final LsssAction redo = new TaskLsssAction("redo", "Redo last school edit",
         _ -> lsss.getRegionManager().redo())
         .setIcon(MiscIcons.REDO);

   public final LsssAction icesResetMetadata = new TaskLsssAction("icesResetMetadata", "Resets ICES metadata from data files",
         _ -> {
            IcesConf icesConf = lsss.getConfigurationManager().getSurveyMiscConf().getIcesConf();
            icesConf.resetValuesFromData();
            icesConf.saveToDatabase();
         })
         .setIcon(MiscIcons.UNDO);

   public final LsssAction startPreprocessing = new TaskLsssAction("startPreprocessing", "Start KORONA preprocessing",
         _ -> lsss.getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getMainSetup().start(Optional.empty()))
         .setIcon(LsssIcons.KORONA);

   public Actions(LSSS lsss) {
      this.lsss = lsss;

      ColorConverterContainer colorConverterContainer = lsss.getInterpretationSettings().getColorConverterContainer();
      lastNonCategorizationColorConverter = colorConverterContainer.getColorConverter();
      lastCategorizationColorConverter = new CategoryColorConverter(colorConverterContainer.getDiscreteVariable(CategoryVariable.class));
      lastPlanktonColorConverter = new CategoryColorConverter(colorConverterContainer.getDiscreteVariable(PlanktonVariable.class));
   }

   public void setup() {
      LsssPackage lsssPackage = lsss.getPackageManager().lsssPackage;
      lsssPackage.addAction(newSurvey);
      lsssPackage.addAction(openSurvey);
      lsssPackage.addAction(editSurvey);
      lsssPackage.addAction(saveSurvey);
      lsssPackage.addAction(closeSurvey);
      lsssPackage.addAction(help);
      lsssPackage.addAction(actionsDialog);
      lsssPackage.addAction(showToolbar);
      lsssPackage.addAction(showStatusBar);
      lsssPackage.addAction(showTooltip);
      lsssPackage.addAction(showStoredMasking);
      lsssPackage.addAction(showCategorization);
      lsssPackage.addAction(showPlankton);
      lsssPackage.addAction(showConditionalMasking);
      lsssPackage.addAction(showOnlyEchogram);
      lsssPackage.addAction(previousSegment);
      lsssPackage.addAction(nextSegment);
      lsssPackage.addAction(preferredSegmentSize);
      lsssPackage.addAction(selectNextRegion);
      lsssPackage.addAction(selectVisibleRegions);
      lsssPackage.addAction(selectVisibleRegionsNot100PercentAssignedOnCurrentFrequency);
      lsssPackage.addAction(selectVisibleRegionsNot100PercentAssignedOnStorableFrequencies);
      lsssPackage.addAction(resetInterpretation);
      lsssPackage.addAction(excludeLowSpeed);
      lsssPackage.addAction(mergeLayersWithSameInterpretation);
      lsssPackage.addAction(setUpperBoundaryFromRange);
      lsssPackage.addAction(setUpperBoundaryFromThreshold);
      lsssPackage.addAction(setLowerBoundaryFromThreshold);
      lsssPackage.addAction(setLowerBoundaryFromCoordinatedBottom);
      lsssPackage.addAction(setLowerBoundaryFromCoordinatedBottomInPreprocessedData);
      lsssPackage.addAction(setLowerBoundaryFromCurrentFrequencyBottom);
      lsssPackage.addAction(deleteBottomDataCurrentFrequency);
      lsssPackage.addAction(deleteAssignmentsOnOtherFrequencies);
      lsssPackage.addAction(deleteAssignmentsOnAllFrequencies);
      lsssPackage.addAction(showSchoolVisualizerDialog);
      lsssPackage.addAction(showCategoryEditor);
      lsssPackage.addAction(reloadData);
      lsssPackage.addAction(undo);
      lsssPackage.addAction(redo);
      lsssPackage.addAction(icesResetMetadata);
      lsssPackage.addAction(startPreprocessing);

      lsssPackage.setKeyStrokeMap(new ConcurrentHashMap<>());

      Map<KeyStroke, ActionExecutor> anywhereKeyStrokeMap = lsssPackage.getKeyStrokeMap().computeIfAbsent(LsssPackage.KEY_STROKE_CONTEXT_ANYWHERE, _ -> new ConcurrentHashMap<>());
      anywhereKeyStrokeMap.put(Shortcuts.TOOLTIP, showTooltip);

      Map<KeyStroke, ActionExecutor> mainWindowKeyStrokeMap = lsssPackage.getKeyStrokeMap().computeIfAbsent(LsssPackage.KEY_STROKE_CONTEXT_MAIN_WINDOW, _ -> new ConcurrentHashMap<>());
      mainWindowKeyStrokeMap.put(Shortcuts.NEW, newSurvey);
      mainWindowKeyStrokeMap.put(Shortcuts.OPEN, openSurvey);
      mainWindowKeyStrokeMap.put(Shortcuts.EDIT, editSurvey);
      mainWindowKeyStrokeMap.put(Shortcuts.SAVE, saveSurvey);
      mainWindowKeyStrokeMap.put(Shortcuts.HELP, help);
      mainWindowKeyStrokeMap.put(Shortcuts.ACTION_DIALOG, actionsDialog);
      mainWindowKeyStrokeMap.put(Shortcuts.STORED_MASKING, showStoredMasking);
      mainWindowKeyStrokeMap.put(Shortcuts.CATEGORIZATION, showCategorization);
      mainWindowKeyStrokeMap.put(Shortcuts.PLANKTON, showPlankton);
      mainWindowKeyStrokeMap.put(Shortcuts.CONDITIONAL_MASKING, showConditionalMasking);
      mainWindowKeyStrokeMap.put(Shortcuts.ONLY_ECHOGRAM, showOnlyEchogram);

      Listener surveyListener = () -> {
         boolean open = lsss.getSurveyManager().isOpen();
         saveSurvey.setEnabled(lsss.getLsssConfig().isPrimaryLSSS && open);
         closeSurvey.setEnabled(lsss.getLsssConfig().isPrimaryLSSS && open);
      };
      lsss.getSurveyManager().getChangeManager().addListener(surveyListener);
      surveyListener.listen();

      newSurvey.setEnabled(lsss.getLsssConfig().isPrimaryLSSS);
      openSurvey.setEnabled(lsss.getLsssConfig().isPrimaryLSSS);

      Listener undoRedoUpdateListener = () -> {
         undo.setEnabled(lsss.getRegionManager().canUndo());
         redo.setEnabled(lsss.getRegionManager().canRedo());
      };
      undoRedoUpdateListener.addToAndNotify(
            lsss.getRegionManager().getUndoChangeManager(),
            lsss.getInterpretationSettings().getPingRangeChangeManager()
      );

      lsss.getInterpretationSettings().getPingRangeChangeManager().addListener(this::update);
      update();

      lsss.getInterpretationSettings().getPingRangeChangeManager().addListener(() -> showStoredMasking.set(true));

      lsss.getInterpretationSettings().getDataFileChangeManager().addListener(this::updateFromDataFileSet);
      updateFromDataFileSet();

      ColorConverterContainer colorConverterContainer = lsss.getInterpretationSettings().getColorConverterContainer();
      colorConverterContainer.getChangeManager().addListener(() -> {
         colorConverterIsChanging = true;
         try {
            ColorConverter converter = colorConverterContainer.getColorConverter();
            DiscreteVariable discreteVariable = converter.getDiscreteVariable();
            ContinuousVariable continuousVariable = converter.getContinuousVariable();
            if (discreteVariable == null && continuousVariable != null && continuousVariable.getVariableGroup() == RawVariableFactory.RAW_VARIABLE_GROUP) {
               lastNonCategorizationColorConverter = converter;
            }
            showCategorization.set(discreteVariable instanceof CategoryVariable);
            if (discreteVariable instanceof CategoryVariable) {
               lastCategorizationColorConverter = converter;
            }
            showPlankton.set(discreteVariable instanceof PlanktonVariable);
            if (discreteVariable instanceof PlanktonVariable) {
               lastPlanktonColorConverter = converter;
            }
         } finally {
            colorConverterIsChanging = false;
         }
      });

      showCategorization.getChangeManager().addListener(() -> {
         boolean show = showCategorization.get();
         if (show && !showCategorization.isEnabled()) {
            showCategorization.set(false);
            return;
         }
         if (show) {
            showPlankton.set(false);
         }
         if (colorConverterIsChanging) {
            return;
         }
         colorConverterContainer.setColorConverter(show ? lastCategorizationColorConverter : lastNonCategorizationColorConverter);
      });
      showPlankton.getChangeManager().addListener(() -> {
         boolean show = showPlankton.get();
         if (show && !showPlankton.isEnabled()) {
            showPlankton.set(false);
            return;
         }
         if (show) {
            showCategorization.set(false);
         }
         if (colorConverterIsChanging) {
            return;
         }
         colorConverterContainer.setColorConverter(show ? lastPlanktonColorConverter : lastNonCategorizationColorConverter);
      });
   }

   private void updateFromDataFileSet() {
      DataFileSet dataFileSet = lsss.getInterpretationSettings().getDataFileSet();

      boolean hasCategorization = dataFileSet.getConfigurationItem(Cac0Datagram.class) != null;
      showCategorization.setEnabled(hasCategorization);
      if (!hasCategorization) {
         showCategorization.set(false);
      }

      boolean hasPlankton = dataFileSet.getConfigurationItem(Pic0Datagram.class) != null;
      showPlankton.setEnabled(hasPlankton);
      if (!hasPlankton) {
         showPlankton.set(false);
      }
   }

   private void update() {
      PingRange pingRange = lsss.getInterpretationSettings().getPingRange();
      boolean hasData = !pingRange.isEmpty();
      if (hasData) {
         previousSegment.setEnabled(pingRange.begin().getPingNumber()
               > lsss.getInterpretationSettings().getDataFileSet().getTotalRange().begin().getPingNumber());
         nextSegment.setEnabled(pingRange.end().getPingNumber()
               < lsss.getInterpretationSettings().getDataFileSet().getTotalRange().end().getPingNumber());
      } else {
         previousSegment.setEnabled(false);
         nextSegment.setEnabled(false);
      }

      selectNextRegion.setEnabled(hasData);
      selectVisibleRegions.setEnabled(hasData);
      selectVisibleRegionsNot100PercentAssignedOnCurrentFrequency.setEnabled(hasData);
      selectVisibleRegionsNot100PercentAssignedOnStorableFrequencies.setEnabled(hasData);
      resetInterpretation.setEnabled(hasData);
      mergeLayersWithSameInterpretation.setEnabled(hasData);
      setUpperBoundaryFromRange.setEnabled(hasData);
      setUpperBoundaryFromThreshold.setEnabled(hasData);
      setLowerBoundaryFromThreshold.setEnabled(hasData);
      setLowerBoundaryFromCoordinatedBottom.setEnabled(hasData);
      deleteBottomDataCurrentFrequency.setEnabled(hasData);
      deleteAssignmentsOnOtherFrequencies.setEnabled(hasData);
      deleteAssignmentsOnAllFrequencies.setEnabled(hasData);
      reloadData.setEnabled(hasData);
      showSchoolVisualizerDialog.setEnabled(hasData);
      showCategoryEditor.setEnabled(hasData);
   }

   private void doPreferredSegmentSize(ActionArgument argument) {
      List<Double> sizes = lsss.getConfigurationManager().getGridConf().preferredHorizontalSizes.getValue();
      if (sizes.isEmpty()) {
         throw new NoCanDoException("No preferred sizes configured");
      }
      lsss.getInterpretationSettings().gotoPreferredSize(sizes.getFirst());
   }

   private void selectVisibleRegionsNot100PercentAssigned(Set<Integer> channels) {
      List<Region> regions = lsss.getRegionManager().visibleRegions()
            .filter(region -> !region.getInterpretation().isCompletelyAssigned(channels))
            .toList();
      lsss.getRegionManager().replaceSelectedRegions(regions);
   }

   public boolean resetInterpretation() {
      float initialPelagicDepth = Math.round(LsssRegionConfiguration.PELAGIC_DEPTH_FACTOR * lsss.getInterpretationSettings().getPelagicZSettings().getZoomedZRange().max());
      FloatParameter pelagicDepth = new FloatParameter(new Name("PelagicDepth"),
            initialPelagicDepth, Unit.METER);
      if (lsss.getInterpretationSettings().isInteractiveMode()) {
         GridBag gridBag = new GridBag()
               .configureVerticalBox();
         gridBag.add(new JLabel("<html><span style='color: red'>Warning: Delete existing interpretation in visual horizontal range?</span>"));
         gridBag.add(Box.createVerticalStrut(5));
         Runnable onOk;
         if (lsss.getDataManager().getDataConfiguration().isSeabedMounted()) {
            onOk = Runnables.doNothing();
         } else if (lsss.getConfigurationManager().getSurveyMiscConf().pelagicMode.getBooleanValue()) {
            ParameterEditorData parameterEditorData = new ParameterEditorData(gridBag.getPanel(), new GUIConfig(), List.of(pelagicDepth));
            Box box = Box.createHorizontalBox();
            gridBag.add(box);
            box.add(new JLabel("Pelagic mode: Set lower boundary to "));
            JComponent inputComponent = parameterEditorData.getInputComponent(pelagicDepth);
            box.add(inputComponent);
            box.add(new JLabel(" m"));
            onOk = Runnables.doNothing();
         } else {
            JRadioButton useCoordinatedBottom = new JRadioButton("Use coordinated bottom", resetInterpretationUsingCoordinatedBottom);
            JRadioButton useBottomOnCurrentChannel = new JRadioButton("Use bottom on current channel", !resetInterpretationUsingCoordinatedBottom);
            GuiUtils.createButtonGroup(useCoordinatedBottom, useBottomOnCurrentChannel);
            gridBag.add(useCoordinatedBottom);
            gridBag.add(useBottomOnCurrentChannel);
            onOk = () -> resetInterpretationUsingCoordinatedBottom = useCoordinatedBottom.isSelected();
         }
         int answer = JOptionPane.showConfirmDialog(lsss.getFrame(), gridBag.getPanel(),
               "Reset interpretation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
         if (answer != JOptionPane.OK_OPTION) {
            return false;
         }
         onOk.run();
      }
      LsssRegionConfiguration regionConfiguration = new LsssRegionConfiguration(lsss);
      ToFloatFunction<PingIndex> upperDepth = regionConfiguration.initialUpperDepth();
      ToFloatFunction<PingIndex> lowerDepth = regionConfiguration.initialLowerDepth(pelagicDepth.getFloatValue(), resetInterpretationUsingCoordinatedBottom);
      lsss.getRegionManager().writeablePingRanges(lsss.getInterpretationSettings().getPingRange()).forEach(range -> {
         lsss.getRegionManager().reset(PingRange.of(range), upperDepth, lowerDepth);
      });
      lsss.getInterpretationSettings().getBottomZSettings().resetBottomBoundaryDepthTransform();
      return true;
   }

   private void excludeLowSpeed() {
      new SimpleInputDialog<>("Exclude pings with low speed", "Minimum speed", "3", Float::parseFloat)
            .setUnit(Unit.KNOTS)
            .setBelowText("The excluded distances are expanded until the speed is stable.")
            .show(lsss.getFrame())
            .ifPresent(minKnots -> {
               RangeSet<PingIndex> exclusion = ExclusionDetector.fromLowSpeed(
                     lsss.getInterpretationSettings().getDataFileSet(),
                     lsss.getInterpretationSettings().getPingRange(),
                     minKnots
               );
               exclusion.forEach(lsss.getRegionManager().getExclusionManager()::excludeRange);
            });
   }

   private void mergeLayersWithSameInterpretation() {
      new WorkerDialog(lsss.getFrame(), "Merging layers with same interpretation")
            .start(_ -> {
               PingRange pingRange = lsss.getInterpretationSettings().getPingRange();
               LayerManager layerManager = lsss.getRegionManager().getLayerManager();
               while (true) {
                  boolean didMerge = false;
                  Set<LayerBoundary> done = new HashSet<>();
                  layerLoop:
                  for (Layer layer : layerManager.getLayers()) {
                     if (!layerManager.getLayers().contains(layer)) {
                        continue;
                     }
                     for (VerticalBoundary boundary : layer.getVerticalBoundaries()) {
                        if (done.add(boundary)) {
                           if (pingRange.containsExcludingBegin(boundary.getPingIndex()) && canMerge(boundary.getLayers())) {
                              if (layerManager.mergeLayers(boundary.getLayers()).isEmpty()) {
                                 didMerge = true;
                                 continue layerLoop;
                              }
                           }
                        }
                     }
                     for (CurveBoundary boundary : layer.getUpperCurveBoundaries()) {
                        if (done.add(boundary)) {
                           if (pingRange.intersects(boundary.getPingRange()) && canMerge(boundary.getLayers())) {
                              if (layerManager.mergeLayers(boundary.getLayers()).isEmpty()) {
                                 didMerge = true;
                                 continue layerLoop;
                              }
                           }
                        }
                     }
                  }
                  if (!didMerge) {
                     break;
                  }
               }
            });
   }

   private static boolean canMerge(List<Layer> layers) {
      if (layers.size() != 2) {
         return false;
      }
      Layer a = layers.get(0);
      Layer b = layers.get(1);
      return a.hasEqualInterpretationTo(b) && a.getLabels().equals(b.getLabels());
   }

   private void setUpperBoundaryFromRange() {
      Preferences preferences = lsss.getPreferences("actions");
      FloatParameter rangeParameter = new FloatParameter(new Name("Range"),
            preferences.getFloat(PREFERENCE_UPPER_BOUNDARY_RANGE, 10), Unit.METER, ValueConstraints.gte(0f));

      boolean ok = new ConfigurableGUIDialog(lsss.getFrame(), null, rangeParameter)
            .setGUI(new ConfigurableGUI().createComponent(rangeParameter))
            .show();
      if (!ok) {
         return;
      }

      float range = rangeParameter.getFloatValue();
      preferences.putFloat(PREFERENCE_UPPER_BOUNDARY_RANGE, range);

      forEachPing("Setting upper boundary from range", (pingIndex, powerData) -> {
         CurveBoundary upperBoundary = lsss.getRegionManager().getLayerManager().getUpperBoundary(pingIndex);
         float depth = powerData.getDepthRange().clamp(powerData.rangeToDepth(range));

         EchogramPoint p = new EchogramPoint(pingIndex, depth);
         lsss.getRegionManager().getLayerManager().editBoundary(p, p, IdentityDepthTransform.INSTANCE, upperBoundary);
         lsss.getRegionManager().getLayerManager().verifyAndAdjustConnectors(upperBoundary.getLayers());
      });
   }

   private void setUpperBoundaryFromThreshold() {
      forEachPing("Setting upper boundary from threshold", (pingIndex, powerData) -> {
         CurveBoundary upperBoundary = lsss.getRegionManager().getLayerManager().getUpperBoundary(pingIndex);
         float depth = upperBoundary.getCurve().getDepth(pingIndex);
         int sampleIndex = powerData.depthToClampedSampleIndex(depth);

         float[] logSv = powerData.getLogSv();
         float threshold = lsss.getRegionManager().getThresholdManager().getLogSvRange(pingIndex).min();
         while (sampleIndex < logSv.length && logSv[sampleIndex] > threshold) {
            sampleIndex++;
         }
         while (sampleIndex > 0 && logSv[sampleIndex - 1] < threshold) {
            sampleIndex--;
         }

         EchogramPoint p = new EchogramPoint(pingIndex, powerData.getSampleDepth(sampleIndex));
         lsss.getRegionManager().getLayerManager().editBoundary(p, p, IdentityDepthTransform.INSTANCE, upperBoundary);
         lsss.getRegionManager().getLayerManager().verifyAndAdjustConnectors(upperBoundary.getLayers());
      });
   }

   private void setLowerBoundaryFromThreshold() {
      forEachPing("Setting lower boundary from threshold", (pingIndex, powerData) -> {
         CurveBoundary bottomBoundary = lsss.getRegionManager().getLayerManager().getBottomBoundary(pingIndex);
         float depth = bottomBoundary.getCurve().getDepth(pingIndex);
         int sampleIndex = powerData.depthToClampedSampleIndex(depth);

         float[] logSv = powerData.getLogSv();
         float threshold = lsss.getRegionManager().getThresholdManager().getLogSvRange(pingIndex).min();
         while (sampleIndex < logSv.length && logSv[sampleIndex] < threshold) {
            sampleIndex++;
         }
         while (sampleIndex > 0 && logSv[sampleIndex - 1] > threshold) {
            sampleIndex--;
         }

         float bottomBoundaryOffset = lsss.getConfigurationManager().getSurveyMiscConf().bottomBoundaryOffset.getFloatValue();
         EchogramPoint p = new EchogramPoint(pingIndex, powerData.getSampleDepth(sampleIndex) - bottomBoundaryOffset);
         lsss.getRegionManager().getLayerManager().editBoundary(p, p, IdentityDepthTransform.INSTANCE, bottomBoundary);
         lsss.getRegionManager().getLayerManager().verifyAndAdjustConnectors(bottomBoundary.getLayers());
      });
   }

   private void forEachPing(String text, BiConsumer<PingIndex, PowerData> pingConsumer) {
      List<PingIndex> pingIndexes = lsss.getRegionManager().writeablePingRanges(lsss.getInterpretationSettings().getPingRange()).stream()
            .flatMap(lsss.getInterpretationSettings().getDataFileSet()::getPingIndexStream)
            .toList();
      ProgressView progressView = new ProgressView(text, pingIndexes.size())
            .mainProgressAsPercentage();
      new WorkerDialog(lsss.getFrame(), progressView.getComponent())
            .start(asyncHandle -> {
               for (PingIndex pingIndex : pingIndexes) {
                  if (asyncHandle.isCancelled()) {
                     return;
                  }
                  progressView.incrementMainProgress("");

                  Ping ping = lsss.getInterpretationSettings().getDataFileSet().getPing(pingIndex);
                  PowerData powerData = ping.getPowerData(lsss.getInterpretationSettings().getChannel());
                  if (powerData == null) {
                     continue;
                  }
                  pingConsumer.accept(pingIndex, powerData);
               }
            });
   }

   private void setLowerBoundaryFromCoordinatedBottom() {
      float bottomBoundaryOffset = lsss.getConfigurationManager().getSurveyMiscConf().bottomBoundaryOffset.getFloatValue();
      DataFileSet dataFileSet = lsss.getDataManager().getDataFileSet();
      setLowerBoundary(setLowerBoundaryFromCoordinatedBottom.getLabel(), pingIndex -> {
         return dataFileSet.getCoordinatedDepth(pingIndex) - bottomBoundaryOffset;
      });
   }

   private void setLowerBoundaryFromCoordinatedBottomInPreprocessedData() {
      float bottomBoundaryOffset = lsss.getConfigurationManager().getSurveyMiscConf().bottomBoundaryOffset.getFloatValue();
      DataFileSet dataFileSet = lsss.getDataManager().getDataFileSet();
      setLowerBoundary(setLowerBoundaryFromCoordinatedBottomInPreprocessedData.getLabel(), pingIndex -> {
         Ping ping = dataFileSet.getPing(pingIndex);
         Dep0Datagram dep0Datagram = ping.getPingItem(Dep0Datagram.class);
         return dep0Datagram != null
               ? dep0Datagram.getMinimumDepth()
               : dataFileSet.getCoordinatedDepth(pingIndex) - bottomBoundaryOffset;
      });
   }

   private void setLowerBoundaryFromCurrentFrequencyBottom() {
      float bottomBoundaryOffset = lsss.getConfigurationManager().getSurveyMiscConf().bottomBoundaryOffset.getFloatValue();
      DataFileSet dataFileSet = lsss.getDataManager().getDataFileSet();
      int channelIndex = lsss.getInterpretationSettings().getChannel() - 1;
      setLowerBoundary(setLowerBoundaryFromCurrentFrequencyBottom.getLabel(), pingIndex -> {
         return (float) dataFileSet.getBot0Datagram(pingIndex).getChannelDepths()[channelIndex] - bottomBoundaryOffset;
      });
   }

   private void setLowerBoundary(String label, ToFloatFunction<PingIndex> depthFunction) {
      int pingCount = lsss.getInterpretationSettings().getPingRange().getPingCount();
      ProgressView progressView = new ProgressView(label, pingCount)
            .mainProgressAsPercentage();
      Listener progressListener = progressView.getMainProgressHandler().asCountingListener(pingCount);
      new WorkerDialog(lsss.getFrame(), progressView.getComponent())
            .start(asyncHandle -> {
               DataFileSet dataFileSet = lsss.getDataManager().getDataFileSet();
               LayerManager layerManager = lsss.getRegionManager().getLayerManager();
               PingRange pingRange = lsss.getInterpretationSettings().getPingRange();
               for (CurveBoundary boundary : getLowerBoundaries(pingRange)) {
                  PingRange intersectionPingRange = boundary.getPingRange().intersection(pingRange);
                  Map<PingIndex, Float> depths = HashMap.newHashMap(intersectionPingRange.getPingCount());
                  for (PingIndex pingIndex : dataFileSet.getPingIndices(intersectionPingRange)) {
                     if (lsss.getRegionManager().isReadOnly(pingIndex)) {
                        continue;
                     }
                     if (asyncHandle.isCancelled()) {
                        return;
                     }
                     float depth = depthFunction.applyAsFloat(pingIndex);
                     depths.put(pingIndex, depth);

                     progressListener.listen();
                  }
                  if (!depths.isEmpty()) {
                     layerManager.editBoundary(intersectionPingRange, depths::get, boundary);
                     layerManager.verifyAndAdjustConnectors(boundary.getLayers());
                  }
               }
            });
   }

   private void deleteBottomDataCurrentFrequency() {
      int pingCount = lsss.getInterpretationSettings().getPingRange().getPingCount();
      ProgressView progressView = new ProgressView(deleteBottomDataCurrentFrequency.getLabel(), pingCount)
            .mainProgressAsPercentage();
      Listener progressListener = progressView.getMainProgressHandler().asCountingListener(pingCount);
      new WorkerDialog(lsss.getFrame(), progressView.getComponent())
            .start(asyncHandle -> {
               DataFileSet dataFileSet = lsss.getDataManager().getDataFileSet();
               int channelIndex = lsss.getInterpretationSettings().getChannel() - 1;
               Map<PingIndex, FloatRangeSet> mask = new HashMap<>();
               PingRange pingRange = lsss.getInterpretationSettings().getPingRange();
               for (CurveBoundary boundary : getLowerBoundaries(pingRange)) {
                  for (PingIndex pingIndex : dataFileSet.getPingIndices(boundary.getPingRange().intersection(pingRange))) {
                     if (asyncHandle.isCancelled()) {
                        return;
                     }
                     if (lsss.getRegionManager().isReadOnly(pingIndex)) {
                        continue;
                     }
                     float lowerBoundaryDepth = boundary.getCurve().getDepth(pingIndex);
                     double channelDepth = dataFileSet.getBot0Datagram(pingIndex).getChannelDepths()[channelIndex];
                     if (lowerBoundaryDepth > channelDepth) {
                        mask.put(pingIndex, FloatRangeSet.of(FloatRange.of(channelDepth, lowerBoundaryDepth)));
                     }

                     progressListener.listen();
                  }
               }
               lsss.getRegionManager().getMaskingManager().mask(mask, lsss.getInterpretationSettings().getChannel());
            });
   }

   private List<CurveBoundary> getLowerBoundaries(PingRange pingRange) {
      return lsss.getRegionManager().getLayerManager().getLayers().stream()
            .flatMap(layer -> layer.getLowerCurveBoundaries().stream())
            .filter(boundary -> boundary.getLayerBelow() == null && boundary.getPingRange().intersects(pingRange))
            .toList();
   }
}
