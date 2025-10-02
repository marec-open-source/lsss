package no.imr.lsss.viewer;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.region.ChannelInterpretation;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.resources.KoronaHelp;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.reports.ReportGenerator;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.backup.BackupFilesGui;
import no.imr.lsss.framework.backup.CopyRemoteFilesGui;
import no.imr.lsss.framework.config.application.packages.UserDefinedPackage;
import no.imr.lsss.framework.config.application.packages.pojo.MenuItemInfo;
import no.imr.lsss.framework.config.survey.acousticcategories.AcousticCategoryConf;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.framework.export.ExportDialog;
import no.imr.lsss.framework.packages.ActionArgument;
import no.imr.lsss.framework.packages.ActionUtils;
import no.imr.lsss.framework.packages.Actions;
import no.imr.lsss.framework.packages.LsssAction;
import no.imr.lsss.framework.wizards.appsetup.ApplicationSetupWizard;
import no.imr.lsss.modules.BaseLsssModule;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.Where;
import no.imr.lsss.modules.korona.tracking.TrackInfoModule;
import no.imr.lsss.modules.korona.tracking.TrackUtils;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.util.LabelUtils;
import no.imr.tools.Utils;
import no.imr.tools.adm.AdmService;
import no.imr.tools.help.ContextSensitiveHelp;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.misc.test.TestUtils;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MenuFilter;
import no.imr.tools.swing.MenuItems;
import no.imr.tools.swing.MenuUtils;
import no.imr.tools.swing.MultiColumnLayout;
import no.imr.tools.swing.SimpleInputDialog;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoManager;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class MainMenuBar {
   private final LSSS lsss;
   private final MainDisplay mainDisplay;
   private final JMenuBar menuBar = new JMenuBar();

   private final List<Component> packageComponents = new ArrayList<>();

   MainMenuBar(MainDisplay mainDisplay) {
      lsss = mainDisplay.getLSSS();
      this.mainDisplay = mainDisplay;
   }

   JMenuBar getMenuBar() {
      return menuBar;
   }

   private JMenu createFileMenu() {
      JMenu fileMenu = new JMenu("File");
      fileMenu.setMnemonic(KeyEvent.VK_F);

      RecentlyOpenedSurveys recentlyOpenedSurveys = new RecentlyOpenedSurveys(lsss.getSurveyManager());

      GuiUtils.autoCreateContentMenu(fileMenu, () -> {
         JMenuItem newSurveyItem = fileMenu.add(ActionUtils.newMenuItem(lsss.getActions().newSurvey));
         newSurveyItem.setMnemonic(KeyEvent.VK_N);
         newSurveyItem.setAccelerator(Shortcuts.NEW);

         JMenuItem openSurveyItem = fileMenu.add(ActionUtils.newMenuItem(lsss.getActions().openSurvey));
         openSurveyItem.setMnemonic(KeyEvent.VK_O);
         openSurveyItem.setAccelerator(Shortcuts.OPEN);

         JMenuItem editSurveyItem = fileMenu.add(ActionUtils.newMenuItem(lsss.getActions().editSurvey));
         editSurveyItem.setMnemonic(KeyEvent.VK_E);
         editSurveyItem.setAccelerator(Shortcuts.EDIT);

         JMenuItem saveSurveyItem = fileMenu.add(ActionUtils.newMenuItem(lsss.getActions().saveSurvey));
         saveSurveyItem.setMnemonic(KeyEvent.VK_S);
         saveSurveyItem.setAccelerator(Shortcuts.SAVE);

         JMenuItem saveSurveyAsItem = fileMenu.add("Save survey as...");
         saveSurveyAsItem.setToolTipText("<html>Save survey configuration files and interpretation work files as...<br>NB: Database is not stored");
         saveSurveyAsItem.setMnemonic(KeyEvent.VK_A);
         saveSurveyAsItem.setDisplayedMnemonicIndex(saveSurveyAsItem.getText().indexOf("as"));
         saveSurveyAsItem.addActionListener(e -> lsss.getSurveyManager().saveAs());

         JMenuItem recentlyOpenedSurveysMenu = fileMenu.add(recentlyOpenedSurveys.getMenu());
         recentlyOpenedSurveysMenu.setEnabled(lsss.getLsssConfig().isPrimaryLSSS);
         recentlyOpenedSurveysMenu.setMnemonic(KeyEvent.VK_U);

         JMenuItem closeSurveyItem = fileMenu.add(ActionUtils.newMenuItem(lsss.getActions().closeSurvey));
         closeSurveyItem.setMnemonic(KeyEvent.VK_L);

         fileMenu.addSeparator();

         boolean connectedToDatabase = lsss.getDatabaseManager().getDatabaseConnection().isConnected();

         JMenuItem reportItem = MiscIcons.DATABASE.on(fileMenu.add("DB: Generate report..."));
         reportItem.setEnabled(connectedToDatabase);
         reportItem.setToolTipText("Generate database report");
         reportItem.setMnemonic(KeyEvent.VK_R);
         reportItem.setDisplayedMnemonicIndex(reportItem.getText().indexOf("report"));
         reportItem.addActionListener(e -> new ReportGenerator(lsss));

         JMenuItem importDbItem = MiscIcons.IMPORT.on(fileMenu.add("Import DB..."));
         importDbItem.setEnabled(connectedToDatabase);
         importDbItem.setToolTipText("Import database");
         importDbItem.addActionListener(e -> new DatabaseImportGUI(lsss, mainDisplay.getFrame()));

         JMenuItem exportDbItem = MiscIcons.EXPORT.on(fileMenu.add("Export DB..."));
         exportDbItem.setEnabled(connectedToDatabase);
         exportDbItem.setToolTipText("Export database to DB directory");
         exportDbItem.addActionListener(e -> new DatabaseExportGUI(lsss, mainDisplay.getFrame()));

         fileMenu.addSeparator();

         JMenuItem exportFilesItem = fileMenu.add("Export to files...");
         exportFilesItem.setMnemonic(KeyEvent.VK_P);
         exportFilesItem.addActionListener(e -> {
            ExportDialog.show(lsss, mainDisplay.getFrame(), lsss.getExportManager().getExporters(),
                  lsss.getConfigurationManager().getDataConf().getDir(DataConfLSSS.EXPORT_SUB_DIR).getFile(), lsss.getExportManager().getExportSettings());
         });

         fileMenu.addSeparator();

         Path surveyFile = lsss.getSurveyManager().getSurveyFile();

         JMenuItem backupFilesItem = fileMenu.add("Backup survey data...");
         backupFilesItem.setToolTipText("Copy files to backup directory");
         backupFilesItem.setMnemonic(KeyEvent.VK_B);
         if (surveyFile != null) {
            backupFilesItem.addActionListener(e -> new BackupFilesGui(lsss, surveyFile));
         } else {
            backupFilesItem.setEnabled(false);
         }

         JMenuItem copyRemoteFilesItem = fileMenu.add("Copy remote survey data...");
         copyRemoteFilesItem.setToolTipText("Copy raw-data files to local directory structure");
         copyRemoteFilesItem.setMnemonic(KeyEvent.VK_C);
         if (surveyFile != null) {
            copyRemoteFilesItem.addActionListener(e -> new CopyRemoteFilesGui(lsss, surveyFile));
         } else {
            copyRemoteFilesItem.setEnabled(false);
         }

         fileMenu.addSeparator();

         JMenuItem exitItem = MiscIcons.POWER.on(fileMenu.add("Exit"));
         exitItem.setMnemonic(KeyEvent.VK_X);
         exitItem.addActionListener(e -> mainDisplay.shutDownIfUnmodifiedOrUserApproved());
      });

      return fileMenu;
   }

   private JMenu createViewMenu() {
      JMenu viewMenu = new JMenu("View");
      viewMenu.setMnemonic(KeyEvent.VK_V);

      GuiUtils.autoCreateContentMenu(viewMenu, () -> {
         Actions actions = lsss.getActions();

         JMenuItem toolbarItem = viewMenu.add(ActionUtils.newCheckboxMenuItem(actions.showToolbar, "Toolbar"));
         toolbarItem.setMnemonic(KeyEvent.VK_T);

         JMenuItem statusBarItem = viewMenu.add(ActionUtils.newCheckboxMenuItem(actions.showStatusBar, "Status bar"));
         statusBarItem.setMnemonic(KeyEvent.VK_S);

         viewMenu.addSeparator();

         JMenuItem showStoredMaskingItem = viewMenu.add(ActionUtils.newCheckboxMenuItem(actions.showStoredMasking, "Stored masking"));
         showStoredMaskingItem.setMnemonic(KeyEvent.VK_M);
         showStoredMaskingItem.setAccelerator(Shortcuts.STORED_MASKING);

         JMenuItem showCategorizationItem = viewMenu.add(ActionUtils.newCheckboxMenuItem(actions.showCategorization, "Categorization"));
         showCategorizationItem.setMnemonic(KeyEvent.VK_C);
         showCategorizationItem.setAccelerator(Shortcuts.CATEGORIZATION);

         JMenuItem showPlanktonItem = viewMenu.add(ActionUtils.newCheckboxMenuItem(actions.showPlankton, "Plankton"));
         showPlanktonItem.setMnemonic(KeyEvent.VK_P);
         showPlanktonItem.setAccelerator(Shortcuts.PLANKTON);

         JMenuItem showConditionalMaskingItem = viewMenu.add(ActionUtils.newCheckboxMenuItem(actions.showConditionalMasking, "Conditional masking"));
         showConditionalMaskingItem.setMnemonic(KeyEvent.VK_O);
         showConditionalMaskingItem.setAccelerator(Shortcuts.CONDITIONAL_MASKING);

         viewMenu.addSeparator();

         JMenuItem onlyEchogramItem = viewMenu.add(ActionUtils.newCheckboxMenuItem(actions.showOnlyEchogram, "Only echogram"));
         onlyEchogramItem.setMnemonic(KeyEvent.VK_E);
         onlyEchogramItem.setAccelerator(Shortcuts.ONLY_ECHOGRAM);
      });

      return viewMenu;
   }

   private JMenu createGoMenu() {
      JMenu goMenu = new JMenu("Go");
      goMenu.setMnemonic(KeyEvent.VK_G);

      GuiUtils.autoCreateContentMenu(goMenu, () -> {
         JMenuItem previousItem = goMenu.add(MiscIcons.STEP_BACK.on(ActionUtils.newMenuItem(lsss.getActions().previousSegment, "Previous segment")));
         previousItem.setMnemonic(KeyEvent.VK_P);
         previousItem.setAccelerator(Shortcuts.PREVIOUS_SEGMENT);

         JMenuItem nextItem = goMenu.add(MiscIcons.STEP_FORWARD.on(ActionUtils.newMenuItem(lsss.getActions().nextSegment, "Next segment")));
         nextItem.setMnemonic(KeyEvent.VK_N);
         nextItem.setAccelerator(Shortcuts.NEXT_SEGMENT);

         goMenu.addSeparator();

         JMenuItem backItem = goMenu.add(ActionUtils.newMenuItem(lsss.getInterpretationSettings().getNavigationHistory().backAction, "Back"));
         backItem.setMnemonic(KeyEvent.VK_B);
         backItem.setAccelerator(Shortcuts.GO_BACK);

         JMenuItem forwardItem = goMenu.add(ActionUtils.newMenuItem(lsss.getInterpretationSettings().getNavigationHistory().forwardAction, "Forward"));
         forwardItem.setMnemonic(KeyEvent.VK_F);
         forwardItem.setAccelerator(Shortcuts.GO_FORWARD);

         goMenu.addSeparator();

         JMenuItem goToVesselDistanceItem = goMenu.add("Go to vessel distance...");
         goToVesselDistanceItem.setMnemonic(KeyEvent.VK_D);
         goToVesselDistanceItem.setEnabled(!lsss.getInterpretationSettings().getDataFileSet().isEmpty());
         goToVesselDistanceItem.addActionListener(e -> {
            String text = Utils.format("%.3f", lsss.getInterpretationSettings().getCenter().getVesselDistance());
            new SimpleInputDialog<>("Go to vessel distance", "Vessel distance", text, Double::parseDouble)
                  .setUnit(Unit.NAUTICAL_MILES)
                  .show(mainDisplay.getFrame())
                  .ifPresent(value -> {
                     PingIndex pingIndex = lsss.getInterpretationSettings().getDataFileSet().getClosestPingIndex(value, PingMapping.DISTANCE);
                     lsss.getInterpretationSettings().setCenter(pingIndex);
                  });
         });
      });

      return goMenu;
   }

   private JMenu createRegionsMenu() {
      JMenu menu = new JMenu("Regions");
      menu.setMnemonic(KeyEvent.VK_R);
      GuiUtils.autoCreateContentMenu(menu, () -> {
         add(menu, lsss.getActions().selectNextRegion, KeyEvent.VK_N);
         add(menu, lsss.getActions().selectVisibleRegions, KeyEvent.VK_A);
         add(menu, lsss.getActions().selectVisibleRegionsNot100PercentAssignedOnCurrentFrequency);
         add(menu, lsss.getActions().selectVisibleRegionsNot100PercentAssignedOnStorableFrequencies);
         menu.add(createSelectRegionsByCategorySubMenu());
         menu.add(createRegionsLabelMenu());
         menu.addSeparator();
         add(menu, lsss.getActions().resetInterpretation, KeyEvent.VK_R);
         add(menu, lsss.getActions().mergeLayersWithSameInterpretation, KeyEvent.VK_M);
         menu.addSeparator();
         add(menu, lsss.getActions().setUpperBoundaryFromRange, KeyEvent.VK_U);
         add(menu, lsss.getActions().setUpperBoundaryFromThreshold, KeyEvent.VK_P);
         add(menu, lsss.getActions().setLowerBoundaryFromThreshold, KeyEvent.VK_O);
         add(menu, lsss.getActions().setLowerBoundaryFromCoordinatedBottom, KeyEvent.VK_B);
         add(menu, lsss.getActions().setLowerBoundaryFromCurrentFrequencyBottom, KeyEvent.VK_Y);
         menu.addSeparator();
         add(menu, lsss.getActions().deleteBottomDataCurrentFrequency, KeyEvent.VK_D);
         menu.addSeparator();
         add(menu, lsss.getActions().deleteAssignmentsOnOtherFrequencies, KeyEvent.VK_F);
         add(menu, lsss.getActions().deleteAssignmentsOnAllFrequencies, KeyEvent.VK_Q);
         menu.addSeparator();
         add(menu, lsss.getActions().showSchoolVisualizerDialog, KeyEvent.VK_S);
         add(menu, lsss.getActions().showCategoryEditor, KeyEvent.VK_C);
      });
      return menu;
   }

   private JMenu createSelectRegionsByCategorySubMenu() {
      JMenu menu = new JMenu("Select visible regions with acoustic category");
      menu.setMnemonic(KeyEvent.VK_W);
      GuiUtils.autoCreateContentMenu(menu, () -> {
         Map<Integer, AcousticCategory> map = lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryMap();
         List<AcousticCategory> acousticCategories = new ArrayList<>();
         for (Integer id : getAcousticCategories(lsss.getRegionManager().getVisibleRegions())) {
            AcousticCategory acousticCategory = map.get(id);
            if (acousticCategory != null) {
               acousticCategories.add(acousticCategory);
            }
         }
         acousticCategories.sort(lsss.getConfigurationManager().getLanguageUtils().acousticCategoryComparator());
         for (AcousticCategory acousticCategory : acousticCategories) {
            Integer id = acousticCategory.getCompId().getAcousticCategory();
            JMenuItem item = MiscIcons.EMPTY.on(menu.add(AcousticCategoryConf.acousticCategoryToListText(acousticCategory, lsss.getConfigurationManager().getLanguageUtils())));
            item.addActionListener(e -> {
               List<Region> regions = lsss.getRegionManager().getVisibleRegions().stream()
                     .filter(region -> region.getInterpretation().containsAcousticCategory(id))
                     .toList();
               lsss.getRegionManager().replaceSelectedRegions(regions);
            });
         }
         if (!acousticCategories.isEmpty()) {
            menu.addSeparator();
         }
         JMenuItem noCategoryItem = MiscIcons.EMPTY.on(menu.add("No category"));
         noCategoryItem.addActionListener(e -> {
            List<Region> regions = lsss.getRegionManager().getVisibleRegions().stream()
                  .filter(region -> region.getInterpretation().isEmpty())
                  .toList();
            lsss.getRegionManager().replaceSelectedRegions(regions);
         });
      });
      return menu;
   }

   private static Set<Integer> getAcousticCategories(List<Region> regions) {
      Set<Integer> ids = new HashSet<>();
      for (Region region : regions) {
         ids.addAll(region.getInterpretation().getRestSpecies());
         for (ChannelInterpretation channelInterpretation : region.getInterpretation().getChannelInterpretations()) {
            ids.addAll(channelInterpretation.getAssignments().keySet());
         }
      }
      return ids;
   }

   private JMenu createRegionsLabelMenu() {
      JMenu menu = MiscIcons.LABEL.on(new JMenu("Region labels"));
      menu.setMnemonic(KeyEvent.VK_L);

      GuiUtils.autoCreateContentMenu(menu, () -> {
         RegionManager regionManager = lsss.getRegionManager();
         List<Region> selectedRegions = regionManager.getSelectedRegions();

         Set<String> selectedLabels = getLabels(selectedRegions.stream());
         Set<String> allLabels = getLabels(regionManager.regionStream());

         JMenu addMenu = MenuUtils.multiColumn(MiscIcons.ADD.on(MenuUtils.addMenu(menu, "Add label to selected", KeyEvent.VK_A)));
         JMenu removeMenu = MenuUtils.multiColumn(MiscIcons.DELETE.on(MenuUtils.addMenu(menu, "Remove label from selected", KeyEvent.VK_R)));
         menu.addSeparator();
         JMenu selectMenu = MenuUtils.multiColumn(MenuUtils.addMenu(menu, "Select by label", KeyEvent.VK_S));
         JMenu deselectMenu = MenuUtils.multiColumn(MenuUtils.addMenu(menu, "Deselect by label", KeyEvent.VK_D));
         JMenu retainMenu = MenuUtils.multiColumn(MenuUtils.addMenu(menu, "Retain by label", KeyEvent.VK_T));

         addMenu.setEnabled(!selectedRegions.isEmpty());
         removeMenu.setEnabled(!selectedLabels.isEmpty());
         selectMenu.setEnabled(!allLabels.isEmpty());
         deselectMenu.setEnabled(!selectedLabels.isEmpty());
         retainMenu.setEnabled(!selectedLabels.isEmpty());

         if (!allLabels.isEmpty()) {
            allLabels.forEach(label -> {
               MenuUtils.addItem(addMenu, label, e -> LabelUtils.add(selectedRegions, label));
               MenuUtils.addItem(selectMenu, label, e -> LabelUtils.select(regionManager, label));
            });
            addMenu.addSeparator();
         }

         MenuUtils.addItem(addMenu, "New label...", KeyEvent.VK_N, e -> {
            new SimpleInputDialog<>("Add new label", "Label", "", Function.identity())
                  .show(mainDisplay.getFrame())
                  .ifPresent(label -> LabelUtils.add(selectedRegions, label));
         });
         addMenu.addSeparator();
         JMenuItem acousticCategoriesItem = MenuUtils.addItem(addMenu, "Acoustic categories", KeyEvent.VK_A, e -> LabelUtils.addAcousticCategories(selectedRegions, lsss));
         acousticCategoriesItem.setToolTipText("<html>For each region add labels for<br>acoustic categories assigned on current channel");

         selectedLabels.forEach(label -> {
            MenuUtils.addItem(removeMenu, label, e -> LabelUtils.remove(selectedRegions, label));
            MenuUtils.addItem(deselectMenu, label, e -> LabelUtils.deselect(regionManager, label));
            MenuUtils.addItem(retainMenu, label, e -> LabelUtils.retain(regionManager, label));
         });

         removeMenu.addSeparator();
         MenuUtils.addItem(removeMenu, "All labels", KeyEvent.VK_A, e -> LabelUtils.removeAll(selectedRegions));
      });

      return menu;
   }

   private static NavigableSet<String> getLabels(Stream<Region> regions) {
      return regions
            .flatMap(region -> region.getLabels().stream())
            .collect(Collectors.toCollection(TreeSet::new));
   }

   private JMenu createTracksMenu() {
      JMenu tracksMenu = new JMenu("Tracks");
      tracksMenu.setMnemonic(KeyEvent.VK_T);
      GuiUtils.autoCreateContentMenu(tracksMenu, () -> {
         TrackInfoModule trackInfoModule = lsss.getModuleManager().getModule(TrackInfoModule.class);

         tracksMenu.add(trackInfoModule.getTrackLabelling().createLabelsMenu());

         tracksMenu.addSeparator();

         UndoManager undoManager = trackInfoModule.getTrackEditing().getUndoManager();
         JMenuItem undoItem = MiscIcons.UNDO.on(MenuUtils.addItem(tracksMenu, "Undo track edit", KeyEvent.VK_U, e -> undoManager.undo()));
         undoItem.setEnabled(undoManager.canUndo());
         JMenuItem redoItem = MiscIcons.REDO.on(MenuUtils.addItem(tracksMenu, "Redo track edit", KeyEvent.VK_R, e -> undoManager.redo()));
         redoItem.setEnabled(undoManager.canRedo());

         tracksMenu.addSeparator();

         tracksMenu.add(TrackUtils.menuItemCreateRegionsForSelected(trackInfoModule));
         tracksMenu.add(TrackUtils.createMenuItemRegionsForPingRange(trackInfoModule));
      });
      return tracksMenu;
   }

   private JMenu createWindowMenu() {
      JMenu windowMenu = new JMenu("Window");
      windowMenu.setMnemonic(KeyEvent.VK_W);
      MultiColumnLayout multiColumnLayout = new MultiColumnLayout();
      multiColumnLayout.setHorizontalFill(true);
      windowMenu.getPopupMenu().setLayout(multiColumnLayout);
      MenuFilter menuFilter = new MenuFilter(windowMenu);
      GuiUtils.autoCreateContentMenu(windowMenu, () -> {
         windowMenu.add(menuFilter.getComponent());
         windowMenu.addSeparator();
         Map<String, List<BaseViewModule>> pluginToModules = lsss.getModuleManager().getViewModules().entrySet().stream()
               .filter(entry -> entry.getKey() != Where.UNSPECIFIED)
               .flatMap(entry -> entry.getValue().stream())
               .sorted(Utils.comparingIgnoringCase(BaseLsssModule::getDisplayName))
               .collect(Collectors.groupingBy(module -> module.getPlugin().getMainPluginId()));

         final class MenuItemGroup {
            private final JPopupMenu.Separator separator;
            private final JLabel label;
            private final List<JMenuItem> items;

            private MenuItemGroup(JPopupMenu.Separator separator, JLabel label, List<JMenuItem> items) {
               this.separator = separator;
               this.label = label;
               this.items = items;
            }
         }

         List<MenuItemGroup> groups = new ArrayList<>();
         HierarchyListener hierarchyListener = e -> {
            boolean previousVisible = false;
            for (MenuItemGroup group : groups) {
               boolean visible = group.items.stream().anyMatch(JMenuItem::isVisible);
               group.separator.setVisible(visible && previousVisible);
               group.label.setVisible(visible);
               previousVisible |= visible;
            }
         };
         for (FeaturePlugin plugin : lsss.getPluginManager().getFeaturePlugins()) {
            List<BaseViewModule> modules = pluginToModules.get(plugin.getPersistentName());
            if (modules == null) {
               continue;
            }
            JPopupMenu.Separator separator = new JPopupMenu.Separator();
            JLabel label = MenuItems.label(plugin.getIconOrEmpty(), plugin.getName().displayName(), true);
            if (!(plugin instanceof BaseSystemFeaturePlugin)) {
               windowMenu.add(separator);
               windowMenu.add(label);
            }
            List<JMenuItem> items = modules.stream()
                  .map(module -> {
                     String displayName = module.getDisplayName();
                     JMenuItem item = MiscIcons.check(module.isEnabled()).on(windowMenu.add(displayName));
                     item.setToolTipText(new HtmlStringBuilder()
                           .text("Show or hide ").html("<b>").text(displayName).html("</b>")
                           .html("<p>")
                           .text(module.getDescription())
                           .build());
                     item.addActionListener(__ -> module.setEnabled(!module.isEnabled()));
                     item.addHierarchyListener(hierarchyListener);
                     return item;
                  })
                  .toList();
            groups.add(new MenuItemGroup(separator, label, items));
         }

         windowMenu.addSeparator();
         windowMenu.add(makeWindowSelectionsMenu());

         JRootPane rootPane = mainDisplay.getFrame().getRootPane();
         int menuMaxY = SwingUtilities.convertPoint(windowMenu, 0, windowMenu.getHeight(), rootPane).y;
         multiColumnLayout.setPreferredMaxHeight(rootPane.getHeight() - menuMaxY);
         windowMenu.getPopupMenu().pack();
      });
      return windowMenu;
   }

   private JMenu makeWindowSelectionsMenu() {
      JMenu windowSelectionsMenu = MiscIcons.EMPTY.on(new JMenu("Saved window selections"));
      GuiUtils.autoCreateContentMenu(windowSelectionsMenu, () -> {
         Path windowSelectionsFile = LSSS.getApplicationDataDir().resolve("config").resolve("windowSelections.xml");
         WindowSelections windowSelections = new WindowSelections(windowSelectionsFile);

         JMenu deleteMenu = MiscIcons.DELETE.on(new JMenu("Delete"));
         if (!windowSelections.nameToDisplayElement.isEmpty()) {
            for (String name : windowSelections.sortedNames()) {
               JMenuItem restoreItem = windowSelectionsMenu.add(name);
               restoreItem.setToolTipText("Restores the previously saved \"" + name + "\" window selection");
               restoreItem.addActionListener(e -> {
                  mainDisplay.fromXml(windowSelections.nameToDisplayElement.get(name));
               });
               JMenuItem deleteItem = deleteMenu.add(name);
               deleteItem.addActionListener(e -> {
                  windowSelections.nameToDisplayElement.remove(name);
                  windowSelections.save();
               });
            }
            windowSelectionsMenu.addSeparator();
         }

         JMenuItem saveCurrentItem = MiscIcons.SAVE.on(windowSelectionsMenu.add("Save current window selection..."));
         saveCurrentItem.setToolTipText("Saves the current window selection so it can be restored later");
         saveCurrentItem.addActionListener(e -> {
            new SimpleInputDialog<>("Save the current window selection", "Name", "", Function.identity())
                  .show(mainDisplay.getFrame())
                  .ifPresent(name -> {
                     windowSelections.nameToDisplayElement.put(name, mainDisplay.toXml());
                     windowSelections.save();
                  });
         });
         if (!windowSelections.nameToDisplayElement.isEmpty()) {
            windowSelectionsMenu.add(deleteMenu);
         }
      });
      return windowSelectionsMenu;
   }

   private static final class WindowSelections {
      private final Path file;
      private final Map<String, Element> nameToDisplayElement = new HashMap<>();

      private WindowSelections(Path file) {
         this.file = file;
         Document document;
         try {
            document = XmlUtils.readDocumentIfExists(file);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error reading " + file, e);
            document = null;
         }
         if (document != null) {
            for (Element element : document.getRootElement().elements()) {
               String name = element.attributeValue("name");
               Element displayElement = element.element(MainDisplay.XML_DISPLAY);
               XmlUtils.removeBlankMixedContentText(displayElement);
               nameToDisplayElement.put(name, displayElement);
            }
         }
      }

      private List<String> sortedNames() {
         return nameToDisplayElement.keySet().stream()
               .sorted(String.CASE_INSENSITIVE_ORDER)
               .toList();
      }

      private void save() {
         Element root = DocumentHelper.createElement("windowsSelections");
         for (String name : sortedNames()) {
            root.addElement("windowSelection")
                  .addAttribute("name", name)
                  .add(nameToDisplayElement.get(name).detach());
         }
         try {
            XmlUtils.writeDocument(root, file);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error writing " + file, e);
         }
      }
   }

   private JMenu createHelpMenu() {
      JMenu helpMenu = new JMenu("Help");
      helpMenu.setMnemonic(KeyEvent.VK_H);

      GuiUtils.autoCreateContentMenu(helpMenu, () -> {
         helpMenu.add(lsss.getHelpSystem().createHelpMenuItem());

         MenuUtils.addItem(helpMenu, "Context help", KeyEvent.VK_C, e -> ContextSensitiveHelp.run());

         MenuUtils.addItem(helpMenu, "Preprocessing (KORONA) help", KeyEvent.VK_P, e -> KoronaHelp.HELP_SET.getTopHelpID().show());

         helpMenu.addSeparator();

         helpMenu.add(MenuItems.releaseNotes(LSSS.getInstallationDir()));

         helpMenu.add(MenuItems.logFile(LSSS.LOGGING_MANAGER));

         helpMenu.addSeparator();

         int countBeforeAdmExtend = helpMenu.getMenuComponentCount();
         AdmService.INSTANCE.addUpdateLicenseMenuItem(helpMenu);
         AdmService.INSTANCE.addCheckForUpdateMenuItem(helpMenu, LSSS.APPLICATION_INFO);
         if (countBeforeAdmExtend != helpMenu.getMenuComponentCount()) {
            helpMenu.addSeparator();
         }

         MenuUtils.addItem(helpMenu, "Run LSSS setup wizard...", e -> new ApplicationSetupWizard(lsss).show());

         helpMenu.addSeparator();

         helpMenu.add(MenuItems.about(LSSS.APPLICATION_INFO));
      });

      return helpMenu;
   }

   void setup() {
      menuBar.add(createFileMenu());
      menuBar.add(createViewMenu());
      menuBar.add(createGoMenu());
      menuBar.add(createRegionsMenu());
      menuBar.add(createTracksMenu());
      menuBar.add(createWindowMenu());
      menuBar.add(createHelpMenu());
      if (Utils.useTestFeatures()) {
         menuBar.add(TestUtils.createDebugMenu(LSSS.LOGGING_MANAGER));
      }

      addGlobalShortcuts();

      lsss.getConfigurationManager().getAppMiscConf().getPackagesConf().getChangeManager().addListener(GuiListeners.coalescingLater(this::updateUiFromPackages));
      updateUiFromPackages();
   }

   private void addGlobalShortcuts() {
      GuiUtils.setAccelerator(menuBar, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), () -> {
         lsss.getInterpretationSettings().mouseover().setFrozen(!lsss.getInterpretationSettings().mouseover().isFrozen());
      });
   }

   private void updateUiFromPackages() {
      packageComponents.forEach(menuBar::remove);
      packageComponents.clear();
      for (UserDefinedPackage userDefinedPackage : lsss.getConfigurationManager().getAppMiscConf().getPackagesConf().getUserDefinedPackages()) {
         for (MenuItemInfo menuItemInfo : userDefinedPackage.info.menus) {
            JMenu menu = new JMenu(menuItemInfo.text);
            menu.setToolTipText("Package: " + userDefinedPackage.getEffectiveLabel());
            setMnemonic(menu, menuItemInfo);
            packageComponents.add(menu);
            GuiUtils.autoCreateContentMenu(menu, () -> {
               addUserDefinedMenuItems(userDefinedPackage, menu, menuItemInfo.items);
            });
         }
      }
      if (!packageComponents.isEmpty()) {
         packageComponents.add(0, new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
               g.setColor(GuiUtils.SEPARATOR_COLOR);
               g.drawLine(0, 4, 0, getHeight() - 4);
            }
         });
         packageComponents.add(1, Box.createHorizontalStrut(1));
         packageComponents.add(GuiUtils.createHorizontalFiller());
      }
      packageComponents.forEach(menuBar::add);
      menuBar.repaint();
   }

   private static void addUserDefinedMenuItems(UserDefinedPackage userDefinedPackage, JMenu menu, List<MenuItemInfo> menuItemInfos) {
      if (menuItemInfos.isEmpty()) {
         JMenuItem emptyItem = MiscIcons.EMPTY.on(menu.add("<Empty>"));
         emptyItem.setEnabled(false);
      }
      for (MenuItemInfo info : menuItemInfos) {
         if (info.isMenu()) {
            JMenu subMenu = new JMenu(info.text);
            menu.add(subMenu);
            setMnemonic(subMenu, info);
            addUserDefinedMenuItems(userDefinedPackage, subMenu, info.items);
         } else {
            JMenuItem menuItem = userDefinedPackage.uiInfoToIcon(info).orElse(MiscIcons.EMPTY).on(menu.add(userDefinedPackage.uiInfoToEffectiveText(info)));
            setMnemonic(menuItem, info);
            menuItem.setToolTipText(userDefinedPackage.uiInfoToToolTip(info));
            menuItem.addActionListener(e -> userDefinedPackage.runAction(info, new ActionArgument(e)));
         }
      }
   }

   private static void setMnemonic(JMenuItem menuItem, MenuItemInfo menuItemInfo) {
      if (menuItemInfo.mnemonic != null) {
         menuItem.setMnemonic(KeyEvent.getExtendedKeyCodeForChar(menuItemInfo.mnemonic));
      }
   }

   private static JMenuItem add(JMenu menu, LsssAction action) {
      return menu.add(ActionUtils.newMenuItem(action));
   }

   private static JMenuItem add(JMenu menu, LsssAction action, int mnemonic) {
      JMenuItem item = add(menu, action);
      item.setMnemonic(mnemonic);
      return item;
   }
}
