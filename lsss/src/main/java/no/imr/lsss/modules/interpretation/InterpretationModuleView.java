package no.imr.lsss.modules.interpretation;

import com.google.common.collect.ImmutableSortedSet;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.storing.StoringIntervalConfig;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.QualityEnum;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.lsss.framework.config.survey.GridConf;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.framework.config.survey.misc.SurveyMiscConf;
import no.imr.lsss.framework.packages.ActionUtils;
import no.imr.lsss.incubator.LsssIncubatorFeatureToggles;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.pojo.PojoRange;
import no.imr.lsss.resources.LsssIcons;
import no.imr.lsss.viewer.Shortcuts;
import no.imr.tools.Utils;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.InstantParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.CurrentInputComponent;
import no.imr.tools.swing.DeepInputListener;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MenuItems;
import no.imr.tools.swing.MouseAndKeyAdapter;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.WrappingFlowLayout;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.table.TableCellFloat;
import no.imr.tools.swing.table.TableCellSlider;
import no.imr.tools.swing.table.TableCellString;
import no.imr.tools.swing.table.TableUtils;
import no.imr.tools.time.TimeUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The view for {@link InterpretationModule}.
 */
public final class InterpretationModuleView extends BaseViewModule.BaseView {
   public static final Color BACKGROUND_COLOR = Color.WHITE;
   public static final Color STORE_BUTTON_COLOR = ColorUtils.MEDIUMAQUAMARINE;
   public static final Color DELETE_BUTTON_COLOR = ColorUtils.TOMATO;
   static final Color MULTIPLE_VALUES_COLOR = ColorUtils.TOMATO;
   static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);
   public static final int STORE_PANEL_HGAP = 2;
   public static final int STORE_PANEL_VGAP = 1;

   private final LSSS lsss;
   private final InterpretationModule module;

   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final StatusTableModel statusTableModel;
   private final ParameterTableModel parameterTableModel;
   private final JTable parameterTable;
   private final InterpretationTableModel interpretationTableModel;
   private final JTable interpretationTable;

   private final DefaultTableCellRenderer rightAlignedCellRenderer = TableUtils.defaultTableCellRenderer(DefaultTableCellRenderer.RIGHT);

   private final JLabel qualityLabel = new JLabel("Quality: ");
   private final JPanel qualityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, STORE_PANEL_HGAP, STORE_PANEL_VGAP));
   private final JPanel nbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, STORE_PANEL_HGAP, STORE_PANEL_VGAP));
   private final StoreDeleteButton storeButton = new StoreDeleteButton(null, STORE_BUTTON_COLOR);
   private final StoreDeleteButton deleteButton = new StoreDeleteButton("Delete", DELETE_BUTTON_COLOR);
   private final JPanel segmentButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, STORE_PANEL_HGAP, STORE_PANEL_VGAP));
   private final JPanel frequencyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, STORE_PANEL_HGAP, STORE_PANEL_VGAP));

   private final AcousticCategoryButtons acousticCategoryButtons;

   InterpretationModuleView(InterpretationModule module) {
      super(module);

      lsss = module.getLSSS();
      this.module = module;

      statusTableModel = new StatusTableModel(module);

      parameterTableModel = new ParameterTableModel(this);
      parameterTable = createParameterTable();

      InterpretationManager interpretationManager = new InterpretationModuleInterpretationManager(module);

      acousticCategoryButtons = new AcousticCategoryButtons(lsss, interpretationManager);

      interpretationTableModel = new InterpretationTableModel(lsss, interpretationManager, acousticCategoryButtons, InterpretationTableModel.ValueType.SA);
      interpretationTableModel.addTableModelListener(_ -> updateStatusTable());
      interpretationTable = new InterpretationTable(interpretationTableModel);

      JPanel leftPanel = new JPanel(new BorderLayout());
      leftPanel.add(new JScrollPane(createStatusTable()));
      leftPanel.add(parameterTable, BorderLayout.SOUTH);

      JPanel rightPanel = new JPanel(new BorderLayout());
      rightPanel.add(new JScrollPane(interpretationTable));
      rightPanel.add(acousticCategoryButtons.getComponent(), BorderLayout.SOUTH);
      acousticCategoryButtons.getComponent().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GuiUtils.SEPARATOR_COLOR));

      JSplitPane upperSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
      upperSplitPane.setResizeWeight(0);
      upperSplitPane.setDividerLocation(240);

      JPanel lowerPanel = new JPanel(new BorderLayout());
      lowerPanel.add(qualityPanel, BorderLayout.NORTH);
      lowerPanel.add(createStoreButtonsPanel());

      mainPanel.add(upperSplitPane);
      mainPanel.add(lowerPanel, BorderLayout.SOUTH);
      mainPanel.setMinimumSize(new Dimension(450, 201));
      mainPanel.setMaximumSize(new Dimension(450, Integer.MAX_VALUE));
      new DeepInputListener(mainPanel, new MouseAndKeyAdapter() {
         private boolean inside;

         @Override
         public void mouseEntered(MouseEvent e) {
            inside = true;
         }

         @Override
         public void mouseExited(MouseEvent e) {
            if (inside && !mainPanel.contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), mainPanel))) {
               inside = false;
               TableUtils.stopCellEditing(parameterTable);
               TableUtils.stopCellEditing(interpretationTable);
            }
         }
      });

      updateAll();
   }

   @Override
   public JComponent getComponent() {
      return mainPanel;
   }

   private JTable createStatusTable() {
      JTable statusTable = new JTable(statusTableModel) {
         @Override
         protected JTableHeader createDefaultTableHeader() {
            // Taken from http://java.sun.com/docs/books/tutorial/uiswing/components/table.html#headertooltip

            return new JTableHeader(columnModel) {
               @Override
               public @Nullable String getToolTipText(MouseEvent e) {
                  int column = TableUtils.pointToModelColumn(table, e.getPoint());
                  return column >= 0 ? statusTableModel.getColumnToolTip(column) : null;
               }
            };
         }
      };
      statusTable.setBackground(BACKGROUND_COLOR);
      statusTable.setCellSelectionEnabled(false);
      statusTable.getTableHeader().setReorderingAllowed(false);

      TableColumn nameColumn = statusTable.getColumnModel().getColumn(StatusTableModel.NAME_COLUMN);
      nameColumn.setMinWidth(60);
      nameColumn.setMaxWidth(60);

      TableColumn totalColumn = statusTable.getColumnModel().getColumn(StatusTableModel.TOTAL_VISUAL_COLUMN);
      totalColumn.setCellRenderer(rightAlignedCellRenderer);
      totalColumn.setPreferredWidth(100);

      TableColumn selectedRegionColumn = statusTable.getColumnModel().getColumn(StatusTableModel.SELECTED_REGION_COLUMN);
      selectedRegionColumn.setCellRenderer(rightAlignedCellRenderer);
      selectedRegionColumn.setPreferredWidth(100);

      return statusTable;
   }

   private JTable createParameterTable() {
      JTable parameterTable = new JTable(parameterTableModel);
      parameterTable.setBackground(BACKGROUND_COLOR);
      parameterTable.setCellSelectionEnabled(false);

      TableColumn nameColumn = parameterTable.getColumnModel().getColumn(ParameterTableModel.NAME_COLUMN);
      nameColumn.setCellRenderer(new TableCellString.Renderer(new TableCellString.RenderSettings(null, null, Color.BLACK)));
      nameColumn.setMinWidth(70);
      nameColumn.setMaxWidth(70);

      TableColumn sliderColumn = parameterTable.getColumnModel().getColumn(ParameterTableModel.SLIDER_COLUMN);
      sliderColumn.setCellRenderer(new TableCellSlider.Renderer());
      sliderColumn.setCellEditor(new TableCellSlider.Editor());
      sliderColumn.setMinWidth(50);

      TableColumn valueColumn = parameterTable.getColumnModel().getColumn(ParameterTableModel.VALUE_COLUMN);
      valueColumn.setCellRenderer(rightAlignedCellRenderer);
      valueColumn.setCellEditor(new TableCellFloat.Editor());
      valueColumn.setMinWidth(30);
      valueColumn.setMaxWidth(30);

      return parameterTable;
   }

   private JComponent createStoreButtonsPanel() {
      storeButton.setMargin(EMPTY_INSETS);
      storeButton.addActionListener(_ -> storeButtonPressed());
      addMouseInsideListener(storeButton, module::setMouseOverStoreButton);

      deleteButton.setMargin(EMPTY_INSETS);
      deleteButton.addActionListener(_ -> deleteButtonPressed());
      addMouseInsideListener(deleteButton, module::setMouseOverDeleteButton);

      nbPanel.setBackground(BACKGROUND_COLOR);
      segmentButtonsPanel.setBackground(BACKGROUND_COLOR);
      frequencyPanel.setBackground(BACKGROUND_COLOR);

      JPanel panel = new JPanel(new WrappingFlowLayout(FlowLayout.LEFT, 0, 0));
      panel.setBackground(BACKGROUND_COLOR);
      panel.add(nbPanel);
      panel.add(Box.createHorizontalStrut(STORE_PANEL_HGAP));
      panel.add(storeButton);
      panel.add(Box.createHorizontalStrut(STORE_PANEL_HGAP));
      panel.add(deleteButton);
      panel.add(Box.createHorizontalStrut(STORE_PANEL_HGAP));
      panel.add(createMoreActionsMenu());
      panel.add(Box.createHorizontalStrut(5));
      panel.add(createStepButtonPanel());
      panel.add(segmentButtonsPanel);
      panel.add(Box.createHorizontalStrut(3));
      panel.add(frequencyPanel);
      return panel;
   }

   private JComponent createMoreActionsMenu() {
      JButton button = MiscIcons.MENU.on(new JButton());
      button.setMargin(EMPTY_INSETS);
      button.setBackground(BACKGROUND_COLOR);
      button.setToolTipText("Menu with more actions");
      GuiUtils.addPopupMenuToButton(button, this::addToMoreActionsMenu);
      return button;
   }

   private void addToMoreActionsMenu(JPopupMenu popupMenu) {
      JMenuItem editItem = MiscIcons.EDIT.on(popupMenu.add("Edit storing settings..."));
      editItem.setToolTipText("Edit the storing settings saved in work files");
      editItem.setEnabled(module.canStoreSaved());
      editItem.addActionListener(_ -> editDatabaseStoringSettings());

      JMenuItem deleteByTimeItem = MiscIcons.DELETE.on(popupMenu.add("Delete time range from database..."));
      deleteByTimeItem.setEnabled(!lsss.getInterpretationSummary().getScatterSet().isEmpty());
      deleteByTimeItem.addActionListener(_ -> deleteByTime());

      if (LsssIncubatorFeatureToggles.AUTOMATIC_INTERPRETATION) {
         popupMenu.addSeparator();

         popupMenu.add(MenuItems.label(MiscIcons.EMPTY, "Incubating features:", true));

         JMenuItem automaticRegionDefinitionItem = MiscIcons.UNDO.on(popupMenu.add("Automatic region definition"));
         automaticRegionDefinitionItem.setEnabled(!lsss.getInterpretationSettings().getPingRange().isEmpty());
         automaticRegionDefinitionItem.addActionListener(_ -> new AutomaticRegionDefinition(lsss).run(mainPanel));

         JMenuItem automaticAssignmentsItem = LsssIcons.KORONA.on(popupMenu.add("Automatic assignments of selected regions on selected channels"));
         automaticAssignmentsItem.setToolTipText("Uses KORONA categorization to assign acoustic categories");
         automaticAssignmentsItem.setEnabled(!lsss.getInterpretationSettings().getPingRange().isEmpty());
         automaticAssignmentsItem.addActionListener(_ -> doAutomaticAssignments());
      }
   }

   private void editDatabaseStoringSettings() {
      GridBag gridBag = new GridBag();
      gridBag.activateHorizontalFill();
      gridBag.getConstraints().anchor = GridBagConstraints.WEST;

      JRadioButton frequencyKeep = new JRadioButton("Keep", true);
      JRadioButton frequencyEdit = new JRadioButton();
      List<JCheckBox> frequencyCheckBoxes = new ArrayList<>();
      JPanel frequencyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      List<RawFileTransducer> transducers = lsss.getInterpretationSettings().getDataFileSet().getRawFileConfiguration().getTransducers();
      for (RawFileTransducer transducer : transducers) {
         JCheckBox checkBox = new JCheckBox(String.valueOf(transducer.getKHz()));
         checkBox.addActionListener(_ -> {
            boolean any = frequencyCheckBoxes.stream().anyMatch(AbstractButton::isSelected);
            frequencyKeep.setSelected(!any);
            frequencyEdit.setSelected(any);
         });
         frequencyPanel.add(checkBox);
         frequencyCheckBoxes.add(checkBox);
      }
      frequencyKeep.addActionListener(_ -> {
         frequencyCheckBoxes.forEach(checkBox -> checkBox.setSelected(false));
      });
      GuiUtils.createButtonGroup(frequencyKeep, frequencyEdit);
      gridBag.addWithLineBreak(new JLabel("Frequencies: "), frequencyKeep, frequencyPanel);

      gridBag.addWithLineBreak(new JSeparator());

      JRadioButton dataDirKeep = new JRadioButton("Keep", true);
      JRadioButton dataDirDataDir = new JRadioButton(DataConfLSSS.RAW_SUB_DIR.parameterName().displayName());
      JRadioButton dataDirKoronaDataDir = new JRadioButton(DataConfLSSS.KORONA_SUB_DIR.parameterName().displayName());
      GuiUtils.createButtonGroup(dataDirKeep, dataDirDataDir, dataDirKoronaDataDir);
      gridBag.addWithLineBreak(new JLabel("Data dir: "), dataDirKeep, GuiUtils.createPanel(new FlowLayout(FlowLayout.LEFT), dataDirDataDir, dataDirKoronaDataDir));

      gridBag.addWithLineBreak(new JSeparator());

      JRadioButton pelagicModeKeep = new JRadioButton("Keep", true);
      JRadioButton pelagicModePelagicMode = new JRadioButton("Pelagic mode");
      JRadioButton pelagicModeBottomMode = new JRadioButton("Bottom mode");
      GuiUtils.createButtonGroup(pelagicModeKeep, pelagicModePelagicMode, pelagicModeBottomMode);
      gridBag.addWithLineBreak(new JLabel("Pelagic mode: "), pelagicModeKeep, GuiUtils.createPanel(new FlowLayout(FlowLayout.LEFT), pelagicModePelagicMode, pelagicModeBottomMode));

      gridBag.addWithLineBreak(new JSeparator());

      JRadioButton qualityKeep = new JRadioButton("Keep", true);
      JRadioButton qualityHigh = new JRadioButton("High");
      JRadioButton qualityIntermediate = new JRadioButton("Intermediate");
      JRadioButton qualityLow = new JRadioButton("Low");
      GuiUtils.createButtonGroup(qualityKeep, qualityHigh, qualityIntermediate, qualityLow);
      gridBag.addWithLineBreak(new JLabel("Quality: "), qualityKeep, GuiUtils.createPanel(new FlowLayout(FlowLayout.LEFT), qualityHigh, qualityIntermediate, qualityLow));

      gridBag.addWithLineBreak(Box.createVerticalStrut(10));

      int answer = JOptionPane.showConfirmDialog(mainPanel, gridBag.getPanel(), "Edit storing settings", JOptionPane.OK_CANCEL_OPTION);
      if (answer == JOptionPane.YES_OPTION) {
         ImmutableSortedSet<Integer> newKHz;
         if (frequencyEdit.isSelected()) {
            newKHz = IntStream.range(0, transducers.size())
                  .filter(i -> frequencyCheckBoxes.get(i).isSelected())
                  .mapToObj(i -> transducers.get(i).getKHz())
                  .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
         } else {
            newKHz = null;
         }
         Short newDataDir;
         if (dataDirDataDir.isSelected()) {
            newDataDir = StoringIntervalConfig.DATA_DIR_RAW;
         } else if (dataDirKoronaDataDir.isSelected()) {
            newDataDir = StoringIntervalConfig.DATA_DIR_KORONA;
         } else {
            newDataDir = null;
         }
         Boolean newPelagicMode;
         if (pelagicModePelagicMode.isSelected()) {
            newPelagicMode = true;
         } else if (pelagicModeBottomMode.isSelected()) {
            newPelagicMode = false;
         } else {
            newPelagicMode = null;
         }
         Short newQuality;
         if (qualityHigh.isSelected()) {
            newQuality = QualityEnum.HIGH.value;
         } else if (qualityIntermediate.isSelected()) {
            newQuality = QualityEnum.INTERMEDIATE.value;
         } else if (qualityLow.isSelected()) {
            newQuality = QualityEnum.LOW.value;
         } else {
            newQuality = null;
         }
         module.findSavedStoreInputs().forEach(storeInput -> {
            StoringIntervalConfig a = storeInput.intervalConfig();
            StoringIntervalConfig b = new StoringIntervalConfig(
                  newKHz != null ? newKHz : a.kHz(),
                  newDataDir != null ? newDataDir : a.dataDir(),
                  newPelagicMode != null ? newPelagicMode : a.pelagicMode(),
                  newQuality != null ? newQuality : a.quality());
            lsss.getRegionManager().getStoringConfigManager().put(storeInput.gridColumnInterval().pingRange(), b);
         });
      }
   }

   private void deleteByTime() {
      Range<Instant> storedTimeRange = lsss.getInterpretationSummary().getScatterSet().getTimeRange();
      InstantParameter min = new InstantParameter(new Name("From"), storedTimeRange.begin());
      InstantParameter max = new InstantParameter(new Name("To"), storedTimeRange.end());
      ParameterEditor parameterEditor = new ParameterEditor(List.of(min, max), new GUIConfig()
            .setTextInputColumns(15)
      );

      GridBag gridBag = new GridBag()
            .configureVerticalBox();
      gridBag.add(new JLabel("Delete interpretation from database?"));
      gridBag.add(Box.createVerticalStrut(10));
      gridBag.add(parameterEditor.getEditorComponent());

      int answer = JOptionPane.showConfirmDialog(mainPanel, gridBag.getPanel(), "Delete time range from database", JOptionPane.OK_CANCEL_OPTION);
      if (answer == JOptionPane.OK_OPTION && CurrentInputComponent.commitEdit()) {
         Instant minTime = min.getValue().orElse(Instant.MIN);
         Instant maxTime = max.getValue().orElse(Instant.MAX);
         if (minTime.isBefore(maxTime)) {
            lsss.getInterpretationSummary().delete(new DefaultRange<>(minTime, maxTime));
         }
      }
   }

   private void doAutomaticAssignments() {
      List<Region> regions = module.getSelectedRegions();
      Set<Integer> channels = module.getChannelsToStore();
      new AutomaticAssignment(lsss, regions, channels).run(mainPanel);
   }

   private void deleteButtonPressed() {
      new StoreTasksDialog(module, false, this::updateQualityPanel);
   }

   private JComponent createStepButtonPanel() {
      JButton stepBackwardButton = ActionUtils.newButton(lsss.getActions().previousSegment);
      stepBackwardButton.setMargin(EMPTY_INSETS);
      stepBackwardButton.setBackground(BACKGROUND_COLOR);
      GuiUtils.setAccelerator(stepBackwardButton, Shortcuts.PREVIOUS_SEGMENT);

      JButton stepForwardButton = ActionUtils.newButton(lsss.getActions().nextSegment);
      stepForwardButton.setMargin(EMPTY_INSETS);
      stepForwardButton.setBackground(BACKGROUND_COLOR);
      GuiUtils.setAccelerator(stepForwardButton, Shortcuts.NEXT_SEGMENT);

      Box box = Box.createHorizontalBox();
      box.add(stepBackwardButton);
      box.add(Box.createHorizontalStrut(STORE_PANEL_HGAP));
      box.add(stepForwardButton);
      return box;
   }

   void updateAll() {
      updateQualityPanel();
      updatePreferredHorizontalSizeButtons();
      updateNbPanel();
      updateStoreButtons();
      updateFrequencyPanel();
      updateAcousticCategoryButtons();
      updateStatusTable();
      updateParameterTable();
      interpretationTableModel.reselectRows();
      updateInterpretationTable();
   }

   void updateQualityPanel() {
      qualityPanel.setBackground(BACKGROUND_COLOR);

      qualityPanel.removeAll();
      boolean visible = module.showQualityOptions.getBooleanValue();
      qualityPanel.setVisible(visible);
      if (!visible) {
         return;
      }

      JRadioButton quality1 = createQualityRadioButton("High", "High (" + QualityEnum.HIGH.value + ": Categories separable acoustically)", QualityEnum.HIGH);
      JRadioButton quality2 = createQualityRadioButton("Intermediate", "Intermediate (" + QualityEnum.INTERMEDIATE.value + ")", QualityEnum.INTERMEDIATE);
      JRadioButton quality3 = createQualityRadioButton("Low", "Low (" + QualityEnum.LOW.value + ": Categories completely mixed acoustically)", QualityEnum.LOW);
      GuiUtils.createButtonGroup(quality1, quality2, quality3);
      updateQualityLabel();
      qualityPanel.add(qualityLabel);
      qualityPanel.add(quality1);
      qualityPanel.add(quality2);
      qualityPanel.add(quality3);

      List<StoreTask> storeTasks = module.getStoreTasks();
      if (storeTasks.size() > 1) {
         JSeparator separator = new JSeparator(JSeparator.VERTICAL);
         separator.setPreferredSize(new Dimension(2, 12));
         qualityPanel.add(Box.createHorizontalStrut(2));
         qualityPanel.add(separator);
         qualityPanel.add(Box.createHorizontalStrut(2));
         for (StoreTask storeTask : storeTasks) {
            if (storeTask == module.getInterpretationModuleStoreTask()) {
               continue;
            }
            JCheckBox checkBox = new JCheckBox(storeTask.getShortLabel(), storeTask.isActive());
            checkBox.setToolTipText("Also store/delete " + storeTask.getLongLabel());
            checkBox.setBackground(BACKGROUND_COLOR);
            checkBox.setMargin(EMPTY_INSETS);
            checkBox.addActionListener(_ -> storeTask.setActive(checkBox.isSelected()));
            qualityPanel.add(checkBox);
         }
      }

      qualityPanel.revalidate();
      qualityPanel.repaint();
   }

   private JRadioButton createQualityRadioButton(String text, String toolTipText, QualityEnum qualityEnum) {
      JRadioButton radioButton = new JRadioButton(text, module.getQuality() == qualityEnum);
      radioButton.setToolTipText(toolTipText);
      radioButton.setBackground(BACKGROUND_COLOR);
      radioButton.setMargin(EMPTY_INSETS);
      radioButton.addActionListener(_ -> module.setQuality(qualityEnum));
      return radioButton;
   }

   void updateQualityLabel() {
      NavigableSet<Short> qualities = lsss.getRegionManager().getStoringConfigManager().getConfigMap().stream(lsss.getInterpretationSettings().getPingRange())
            .map(entry -> entry.value().quality())
            .collect(Collectors.toCollection(TreeSet::new));
      String toolTip = "Quality marking (How easy it is to scrutinize)";
      Color foreground = null;
      if (qualities.size() > 1 || qualities.size() == 1 && qualities.first() != module.getQuality().value) {
         String qualitiesText = qualities.stream()
               .map(QualityEnum::valueToText)
               .collect(Collectors.joining(", "));
         toolTip = new HtmlStringBuilder().text(toolTip).html("<br>").text("Saved qualities in visible area: ").text(qualitiesText).build();
         foreground = Color.RED;
      }
      qualityLabel.setToolTipText(toolTip);
      qualityLabel.setForeground(foreground);
   }

   void updatePreferredHorizontalSizeButtons() {
      GridConf gridConf = lsss.getConfigurationManager().getGridConf();
      segmentButtonsPanel.removeAll();
      for (double size : gridConf.preferredHorizontalSizes.getValue()) {
         String sizeString = Utils.toString(size) + " " + gridConf.horizontalGridUnit.getValue().getUnitString();
         JButton button = new JButton(sizeString);
         button.setMargin(EMPTY_INSETS);
         button.setBackground(BACKGROUND_COLOR);
         button.addActionListener(_ -> lsss.getInterpretationSettings().gotoPreferredSize(size));
         button.setToolTipText("Adjust horizontal size to " + sizeString);
         button.setEnabled(!lsss.getInterpretationSettings().getDataFileSet().isEmpty());
         segmentButtonsPanel.add(button);
      }
      mainPanel.validate();
      mainPanel.repaint();
   }

   void updateNbPanel() {
      GridConf gridConf = lsss.getConfigurationManager().getGridConf();
      PingMapping gridPingMapping = gridConf.horizontalGridUnit.getValue();
      SurveyMiscConf surveyMiscConf = lsss.getConfigurationManager().getSurveyMiscConf();
      InterpretationSettings interpretationSettings = lsss.getInterpretationSettings();

      List<String> tooltips = new ArrayList<>();
      List<Consumer<JPopupMenu>> popupMenuItems = new ArrayList<>();

      if (!gridConf.doesSchoolGridEvenlyDivideEchogramGrid()) {
         tooltips.add("Horizontal size of the <b>school grid</b> does not evenly divide the <b>echogram grid</b>"
               + "<br>Parts of schools may not be stored to the database");
         popupMenuItems.add(menu -> {
            JMenuItem item = menu.add("Edit grid sizes...");
            item.addActionListener(_ -> {
               gridConf.showInConfigurationDialog();
            });
         });
      }

      if (gridPingMapping != interpretationSettings.getPingMapping()) {
         tooltips.add("Database storage unit <b>" + gridPingMapping + "</b>"
               + "<br>differs from echogram ping mapping <b>" + interpretationSettings.getPingMapping() + "</b>");
         popupMenuItems.add(menu -> {
            JMenuItem item = menu.add("Set echogram ping mapping to " + gridPingMapping);
            item.addActionListener(_ -> {
               interpretationSettings.setPingMapping(gridPingMapping);
            });
         });
      }

      if (surveyMiscConf.showWarningIfNotUsingPreferredLowerThreshold.getBooleanValue()) {
         PingRange echogramPingRange = interpretationSettings.getPingRange();
         Float singleLowerThreshold = lsss.getRegionManager().getThresholdManager().getLowerThresholdIfSingleValue(echogramPingRange);
         int preferredLowerThreshold = surveyMiscConf.preferredLowerThreshold.getIntValue();
         if (!echogramPingRange.isEmpty() && (singleLowerThreshold == null || singleLowerThreshold != preferredLowerThreshold)) {
            tooltips.add("Lower threshold differs from preferred lower threshold: " + preferredLowerThreshold + " dB");
            popupMenuItems.add(menu -> {
               JMenuItem item = menu.add("Set lower threshold to " + preferredLowerThreshold + " dB");
               item.addActionListener(_ -> {
                  lsss.getRegionManager().getThresholdManager().set(echogramPingRange, null, (float) preferredLowerThreshold, null);
               });
            });
         }
      }

      if (surveyMiscConf.showWarningIfVerticallyZoomed.getBooleanValue() &&
            (interpretationSettings.getPelagicZSettings().isZoomedVertically() ||
                  !surveyMiscConf.pelagicMode.getBooleanValue() && interpretationSettings.getBottomZSettings().isZoomedVertically())) {
         tooltips.add("The echogram is zoomed vertically");
         popupMenuItems.add(menu -> {
            JMenuItem item = menu.add("Zoom echogram out vertically");
            item.addActionListener(_ -> {
               interpretationSettings.getPelagicZSettings().zoomOut();
               if (!surveyMiscConf.pelagicMode.getBooleanValue()) {
                  interpretationSettings.getBottomZSettings().zoomOut();
               }
            });
         });
      }

      nbPanel.removeAll();
      if (!tooltips.isEmpty()) {
         JLabel label = new JLabel("NB!");
         label.setForeground(Color.RED);
         nbPanel.add(label);
         String tooltip = tooltips.stream().collect(Collectors.joining("<br><br>", "<html>", "<br><br>Right-click for options"));
         label.setToolTipText(tooltip);
         label.addMouseListener(new PopupMenuMouseListener(_ -> {
            JPopupMenu menu = new JPopupMenu();
            popupMenuItems.forEach(item -> item.accept(menu));
            return menu;
         }));
      }
      nbPanel.setVisible(!tooltips.isEmpty());
      mainPanel.validate();
   }

   private void storeButtonPressed() {
      new StoreTasksDialog(module, true, this::updateQualityPanel);
   }

   void updateStoreButtons() {
      if (module.isStoring()) {
         return;
      }

      boolean canStoreSaved = module.canStoreSaved();
      boolean canDeleteFromDatabase = module.canDeleteFromDatabase();
      deleteButton.setEnabled(canDeleteFromDatabase || canStoreSaved);
      deleteButton.setStriped(!canDeleteFromDatabase && canStoreSaved);
      deleteButton.setToolTipText(!canDeleteFromDatabase && canStoreSaved
            ? "Delete storing settings from areas not stored to database"
            : "Delete from the database and keep storing settings");

      boolean hasSurvey = lsss.getConfigurationManager().getSurveyConf().getSurvey() != null;
      GridConf gridConf = lsss.getConfigurationManager().getGridConf();
      double horizontalGridSize = gridConf.horizontalGridSize.getDoubleValue();
      PingMapping gridPingMapping = gridConf.horizontalGridUnit.getValue();

      storeButton.setText("Store " + Utils.toString(horizontalGridSize) + " " + gridPingMapping.getUnitString());
      if (hasSurvey) {
         storeButton.setEnabled(canStoreSaved || module.canStoreNew());
         storeButton.setStriped(canStoreSaved);
         storeButton.setToolTipText(canStoreSaved
               ? "Store previously stored areas to database"
               : "Store new areas to database");
      } else {
         storeButton.setEnabled(false);
         storeButton.setStriped(false);
         storeButton.setToolTipText("<html>Cannot store to database<br>No survey selected");
      }
   }

   void updateFrequencyPanel() {
      frequencyPanel.removeAll();

      for (RawFileTransducer transducer : lsss.getInterpretationSettings().getDataFileSet().getRawFileConfiguration().getTransducers()) {
         int kHz = transducer.getKHz();
         JToggleButton toggleButtonKHz = new JToggleButton(String.valueOf(kHz), module.frequencies.getValue().contains(kHz));
         frequencyPanel.add(toggleButtonKHz);
         toggleButtonKHz.setBackground(BACKGROUND_COLOR);
         toggleButtonKHz.setMargin(EMPTY_INSETS);
         toggleButtonKHz.addActionListener(_ -> {
            NavigableSet<Integer> storeKHz = new TreeSet<>(module.frequencies.getValue());
            if (toggleButtonKHz.isSelected()) {
               storeKHz.add(kHz);
            } else {
               storeKHz.remove(kHz);
            }
            module.frequencies.setValue(List.copyOf(storeKHz));
            updateStoreButtons();
         });
      }

      mainPanel.validate();
   }

   void updateAcousticCategoryButtons() {
      acousticCategoryButtons.clearSelection();
      acousticCategoryButtons.setEnabled(module.getSelectedRegions().stream().anyMatch(Region::isWritable));
   }

   void updateStatusTable() {
      TableUtils.updateAllRows(statusTableModel);
   }

   void updateParameterTable() {
      TableUtils.updateAllRows(parameterTableModel);
   }

   void updateInterpretationTable() {
      TableUtils.updateAllRows(interpretationTableModel);
   }

   void interpretationChanged() {
      TableCellEditor cellEditor = interpretationTable.getCellEditor();
      if (cellEditor != null) {
         cellEditor.cancelCellEditing();
      }
      interpretationTableModel.reselectRows();
      updateStatusTable();
   }

   void addPojoData(PojoData.Builder builder) {
      builder
            .with("status", statusTableModel.getPojoData(builder))
            .with("parameters", parameterTableModel.getPojoData(builder))
            .with("interpretation", interpretationTableModel.getPojoData(builder));
   }

   private static void addMouseInsideListener(JComponent component, Consumer<Boolean> insideListener) {
      component.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseEntered(MouseEvent e) {
            insideListener.accept(true);
         }

         @Override
         public void mouseExited(MouseEvent e) {
            insideListener.accept(false);
         }
      });
      component.addHierarchyListener(_ -> {
         if (!component.isShowing()) {
            insideListener.accept(false);
         }
      });
   }

   private static final class StoreDeleteButton extends JButton {
      private final Color color;
      private boolean striped;

      private StoreDeleteButton(@Nullable String text, Color color) {
         super(text);
         this.color = color;
         setContentAreaFilled(false);
      }

      private void setStriped(boolean striped) {
         if (this.striped == striped) {
            return;
         }
         this.striped = striped;
         repaint();
      }

      @Override
      protected void paintComponent(Graphics g) {
         int width = getWidth();
         int height = getHeight();
         if (striped) {
            GuiUtils.renderSlopingLines((Graphics2D) g, width, height, BACKGROUND_COLOR, color);
         } else {
            g.setColor(isEnabled() ? color : BACKGROUND_COLOR);
            g.fillRect(0, 0, width, height);
         }
         super.paintComponent(g);
      }
   }

   /**
    * Status table model.
    */
   private static final class StatusTableModel extends AbstractTableModel {
      private static final int NAME_COLUMN = 0;
      private static final int TOTAL_VISUAL_COLUMN = 1;
      private static final int SELECTED_REGION_COLUMN = 2;

      private final String[] columnNames = {"V. slice", "Distance tot", "Selected region"};
      private final @Nullable String[] columnToolTips = {null, "Total in displayed distance", "Selected regions in displayed distance"};
      private final String[] rowNames = {
            "Date",
            "Time",
            "Duration",
            "Dist[nmi]",
            "#Ping",
            "<html>s<sub>A</sub>",
            "<html>s<sub>A</sub> corr."};

      private final DateTimeFormatter dateFormat = TimeUtils.createUTCDateTimeFormatter("yyyy.MM.dd");
      private final DateTimeFormatter timeFormat = TimeUtils.createUTCDateTimeFormatter("HH:mm:ss");

      private final LSSS lsss;
      private final InterpretationModule interpretationModule;

      private StatusTableModel(InterpretationModule interpretationModule) {
         lsss = interpretationModule.getLSSS();
         this.interpretationModule = interpretationModule;
      }

      @Override
      public int getRowCount() {
         return rowNames.length;
      }

      @Override
      public int getColumnCount() {
         return columnNames.length;
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
         return switch (columnIndex) {
            case NAME_COLUMN -> rowNames[rowIndex];
            case TOTAL_VISUAL_COLUMN -> getTotalValueAt(rowIndex);
            case SELECTED_REGION_COLUMN -> getSelectedRegionValueAt(rowIndex);
            default -> throw new IllegalArgumentException(Integer.toString(columnIndex));
         };
      }

      private Object getTotalValueAt(int rowIndex) {
         PingRange pingRange = getVisiblePingRange();
         if (pingRange.isEmpty()) {
            return "";
         }
         return switch (rowIndex) {
            case 0 -> dateFormat.format(pingRange.begin().getInstant());
            case 1 -> timeFormat.format(pingRange.begin().getInstant());
            case 2 -> pingRange.getDurationString();
            case 3 -> getDistString(pingRange);
            case 4 -> pingRange.getPingCount();
            case 5 -> Utils.numberToString(interpretationModule.getTotalSa(false));
            case 6 -> Utils.numberToString(interpretationModule.getTotalSa(true));
            default -> throw new IllegalArgumentException(Integer.toString(rowIndex));
         };
      }

      private PingRange getVisiblePingRange() {
         return lsss.getInterpretationSettings().getPingRange();
      }

      private Object getSelectedRegionValueAt(int rowIndex) {
         PingRange pingRange = getSelectedRegionsPingRange();
         if (pingRange.isEmpty()) {
            return "";
         }
         return switch (rowIndex) {
            case 0 -> dateFormat.format(pingRange.begin().getInstant());
            case 1 -> timeFormat.format(pingRange.begin().getInstant());
            case 2 -> pingRange.getDurationString();
            case 3 -> getDistString(pingRange);
            case 4 -> pingRange.getPingCount();
            case 5 -> Utils.numberToString(interpretationModule.getRegionSa(false));
            case 6 -> Utils.numberToString(interpretationModule.getRegionSa(true));
            default -> throw new IllegalArgumentException(Integer.toString(rowIndex));
         };
      }

      private PingRange getSelectedRegionsPingRange() {
         return RegionManager.getPingRange(interpretationModule.getSelectedRegions())
               .intersection(getVisiblePingRange());
      }

      private String getDistString(PingRange pingRange) {
         return Utils.format("%.1f(%.3f)", lsss.getInterpretationSettings().getDataFileSet().getVesselDistanceUncorrectedForWrapAround(pingRange.begin()), pingRange.getVesselDistance());
      }

      @Override
      public String getColumnName(int column) {
         return columnNames[column];
      }

      private @Nullable String getColumnToolTip(int column) {
         return columnToolTips[column];
      }

      private PojoData getPojoData(PojoData.Builder context) {
         PojoData.Builder builder = context.newBuilder();
         PingRange visiblePingRange = getVisiblePingRange();
         if (!visiblePingRange.isEmpty()) {
            builder
                  .with("displayedDistance", context.newBuilder()
                        .with("time", new PojoRange<>(
                              visiblePingRange.begin().getInstant().toString(),
                              visiblePingRange.end().getInstant().toString()))
                        .with("vesselDistance", Unit.NAUTICAL_MILES, new PojoRange<>(
                              ExportRounding.vesselDistance().applyAsDouble(visiblePingRange.begin().getVesselDistance()),
                              ExportRounding.vesselDistance().applyAsDouble(visiblePingRange.end().getVesselDistance())))
                        .with("pingNumber", new PojoRange<>(
                              visiblePingRange.begin().getPingNumber(),
                              visiblePingRange.end().getPingNumber()))
                        .with("sa", Unit.SA,
                              ExportRounding.sa().applyAsDouble(interpretationModule.getTotalSa(false)))
                        .with("saCorrected", Unit.SA,
                              ExportRounding.sa().applyAsDouble(interpretationModule.getTotalSa(true)))
                        .build());
         }
         PingRange regionsPingRange = getSelectedRegionsPingRange();
         if (!regionsPingRange.isEmpty()) {
            builder
                  .with("selectedRegions", context.newBuilder()
                        .with("time", new PojoRange<>(
                              regionsPingRange.begin().getInstant().toString(),
                              regionsPingRange.end().getInstant().toString()))
                        .with("vesselDistance", Unit.NAUTICAL_MILES, new PojoRange<>(
                              ExportRounding.vesselDistance().applyAsDouble(regionsPingRange.begin().getVesselDistance()),
                              ExportRounding.vesselDistance().applyAsDouble(regionsPingRange.end().getVesselDistance())))
                        .with("pingNumber", new PojoRange<>(
                              regionsPingRange.begin().getPingNumber(),
                              regionsPingRange.end().getPingNumber()))
                        .with("sa", Unit.SA,
                              ExportRounding.sa().applyAsDouble(interpretationModule.getRegionSa(false)))
                        .with("saCorrected", Unit.SA,
                              ExportRounding.sa().applyAsDouble(interpretationModule.getRegionSa(true)))
                        .build());
         }
         return builder.build();
      }
   }

   /**
    * Parameter table model.
    */
   private static final class ParameterTableModel extends AbstractParameterTableModel {
      private static final int BUBBLE_CORRECTION_ROW = 0;

      private final InterpretationModuleView view;
      private final LSSS lsss;

      private ParameterTableModel(InterpretationModuleView view) {
         super(1);

         this.view = view;
         lsss = view.lsss;
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
         switch (rowIndex) {
            case BUBBLE_CORRECTION_ROW -> {
               float bubbleCorrection = lsss.getRegionManager().getBubbleCorrectionManager().getWritableBubbleCorrection(lsss.getInterpretationSettings().getPingRange());

               switch (columnIndex) {
                  case NAME_COLUMN -> {
                     String name = "Bubble corr";
                     NavigableSet<Float> bubbleCorrections = lsss.getRegionManager().getBubbleCorrectionManager().getBubbleCorrections(lsss.getInterpretationSettings().getPingRange());
                     if (bubbleCorrections.size() > 1) {
                        String toolTipText = bubbleCorrections.stream()
                              .map(Utils::numberToString)
                              .collect(Collectors.joining(", ", "<html>Bubble corrections in visible area:<br>", ""));
                        return new TableCellString.RenderSettings(name, toolTipText, MULTIPLE_VALUES_COLOR);
                     } else {
                        return name;
                     }
                  }
                  case SLIDER_COLUMN -> {
                     return new TableCellSlider.FloatSliderSetting(bubbleCorrection, 0, 2, 0.01f);
                  }
                  case VALUE_COLUMN -> {
                     return Utils.numberToString(bubbleCorrection);
                  }
                  default -> {
                     throw new IllegalArgumentException(Integer.toString(columnIndex));
                  }
               }
            }
            default -> {
               throw new IllegalArgumentException(Integer.toString(rowIndex));
            }
         }
      }

      @Override
      public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
         if (aValue instanceof String s) {
            aValue = Utils.stringToNumber(s);
         }
         if (aValue == null) {
            return;
         }

         switch (rowIndex) {
            case BUBBLE_CORRECTION_ROW -> {
               float bubbleCorrection = aValue instanceof TableCellSlider.FloatSliderSetting
                     ? ((TableCellSlider.FloatSliderSetting) aValue).getFloatValue()
                     : ((Number) aValue).floatValue();
               lsss.getRegionManager().getBubbleCorrectionManager().setBubbleCorrection(lsss.getInterpretationSettings().getPingRange(), bubbleCorrection);
            }
            default -> {
               throw new IllegalArgumentException(rowIndex + ", " + columnIndex);
            }
         }

         fireTableRowsUpdated(rowIndex, rowIndex);
         view.updateInterpretationTable();
      }

      private PojoData getPojoData(PojoData.Builder context) {
         float bubbleCorrection = lsss.getRegionManager().getBubbleCorrectionManager().getWritableBubbleCorrection(lsss.getInterpretationSettings().getPingRange());
         return context.newBuilder()
               .with("bubbleCorrection", bubbleCorrection)
               .build();
      }
   }
}
