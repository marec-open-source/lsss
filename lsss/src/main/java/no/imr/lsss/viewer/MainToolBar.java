package no.imr.lsss.viewer;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.EchogramWorkingMode;
import no.imr.lsss.framework.config.application.packages.UserDefinedPackage;
import no.imr.lsss.framework.config.application.packages.pojo.ToolbarButtonInfo;
import no.imr.lsss.framework.config.survey.GridConf;
import no.imr.lsss.framework.config.survey.data.DataConf;
import no.imr.lsss.framework.packages.ActionArgument;
import no.imr.lsss.framework.packages.ActionUtils;
import no.imr.lsss.framework.packages.LsssAction;
import no.imr.tools.Utils;
import no.imr.tools.swing.GroupingToolBar;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.svg.SvgIcon;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

final class MainToolBar {
   private final LSSS lsss;
   private final GroupingToolBar toolBar = new GroupingToolBar();

   private final List<JButton> preferredHorizontalSizeButtons = new ArrayList<>();
   private final JToggleButton toolTipButton = MiscIcons.INFORMATION.on(new JToggleButton());

   private final JToggleButton workingModeEditButton = MiscIcons.EDIT.on(new JToggleButton());
   private final JToggleButton workingModeDeleteButton = MiscIcons.ERASE.on(new JToggleButton());
   private final JToggleButton workingModeZoomButton = MiscIcons.ZOOM.on(new JToggleButton());
   private final JToggleButton workingModeAddButton = MiscIcons.ADD.on(new JToggleButton());

   private final List<JComponent> packageComponents = new ArrayList<>();

   MainToolBar(LSSS lsss) {
      this.lsss = lsss;
   }

   void setup() {
      JButton stepBackwardButton = ActionUtils.newButton(lsss.getActions().previousSegment);
      GuiUtils.setAccelerator(stepBackwardButton, Shortcuts.PREVIOUS_SEGMENT);

      JButton stepForwardButton = ActionUtils.newButton(lsss.getActions().nextSegment);
      GuiUtils.setAccelerator(stepForwardButton, Shortcuts.NEXT_SEGMENT);

      toolBar.add(List.of(stepBackwardButton, stepForwardButton));

      toolBar.add(preferredHorizontalSizeButtons);

      toolBar.addSeparator(); //--------------------------------------------

      toolBar.add(new RangeChooser(lsss.getInterpretationSettings()).getToolBarComponents());

      toolBar.addSeparator(); //--------------------------------------------

      JButton goBackButton = ActionUtils.newButton(lsss.getInterpretationSettings().getNavigationHistory().backAction);
      GuiUtils.setAccelerator(goBackButton, Shortcuts.GO_BACK);

      JButton goForwardButton = ActionUtils.newButton(lsss.getInterpretationSettings().getNavigationHistory().forwardAction);
      GuiUtils.setAccelerator(goForwardButton, Shortcuts.GO_FORWARD);

      toolBar.add(List.of(goBackButton, goForwardButton));

      toolBar.addSeparator(); //--------------------------------------------

      JButton undoEditButton = ActionUtils.newButton(lsss.getActions().undo);
      GuiUtils.setAccelerator(undoEditButton, Shortcuts.UNDO);

      JButton redoEditButton = ActionUtils.newButton(lsss.getActions().redo);
      GuiUtils.setAccelerator(redoEditButton, Shortcuts.REDO);

      toolBar.add(List.of(undoEditButton, redoEditButton));

      toolBar.addSeparator(); //--------------------------------------------

      JButton selectNextRegionButton = ActionUtils.newButton(lsss.getActions().selectNextRegion);

      JButton reloadDataButton = ActionUtils.newButton(lsss.getActions().reloadData);

      GuiUtils.setAccelerator(toolTipButton, Shortcuts.TOOLTIP);
      toolTipButton.addActionListener(_ -> lsss.getActions().showTooltip.toggle());

      JButton editSurveyButton = ActionUtils.newButton(lsss.getActions().editSurvey);
      GuiUtils.setAccelerator(editSurveyButton, Shortcuts.EDIT);

      toolBar.add(List.of(selectNextRegionButton, reloadDataButton, toolTipButton, editSurveyButton));

      toolBar.addSeparator(); //--------------------------------------------

      toolBar.add(modeButtons());

      toolBar.addSeparator(); //--------------------------------------------

      List<JComponent> preprocessingButtons = new ArrayList<>();
      List<LsssAction> startPreprocessingActions = new ArrayList<>();
      lsss.getConfigurationManager().getDataConf().getAllUnitsRecursively(DataConf.class)
            .forEach(dataConf -> {
               LsssAction action = dataConf.getStartPreprocessingAction();
               if (action != null) {
                  preprocessingButtons.add(dataConf.getKoronaButton());
                  startPreprocessingActions.add(action);
               }
            });
      if (startPreprocessingActions.size() == 1) {
         preprocessingButtons.add(MiscIcons.PLAY.on(ActionUtils.newButton(startPreprocessingActions.getFirst())));
      } else {
         JButton playButton = MiscIcons.PLAY.on(new JButton());
         playButton.setToolTipText("Show menu with options for starting KORONA");
         preprocessingButtons.add(playButton);
         GuiUtils.addPopupMenuToButton(playButton, popupMenu -> {
            startPreprocessingActions.stream()
                  .map(ActionUtils::newMenuItem)
                  .forEach(popupMenu::add);
         });
      }

      toolBar.add(preprocessingButtons);

      toolBar.addSeparator(); //--------------------------------------------

      toolBar.add(new FrequencyChooser(lsss.getInterpretationSettings()).getToolBarComponents());

      toolBar.add(packageComponents);

      GuiListeners.coalescingLater(this::updatePreferredHorizontalSizeButtons).addToAndNotify(
            lsss.getConfigurationManager().getGridConf().horizontalGridUnit,
            lsss.getConfigurationManager().getGridConf().preferredHorizontalSizes,
            lsss.getInterpretationSettings().getDataFileChangeManager()
      );
      GuiListeners.coalescingLater(this::updateToolTipButton).addToAndNotify(
            lsss.getActions().showTooltip.getChangeManager()
      );
      GuiListeners.coalescingLater(this::updateModeButtons).addToAndNotify(
            lsss.getInterpretationSettings().getDataFileChangeManager(),
            lsss.getInterpretationSettings().getEchogramSettings().workingMode
      );
      GuiListeners.coalescingLater(this::updateUiFromPackages).addToAndNotify(
            lsss.getConfigurationManager().getAppMiscConf().getPackagesConf().getChangeManager()
      );
      GuiListeners.coalescingLater(this::updateVisibility).addToAndNotify(
            lsss.getActions().showToolbar.getChangeManager()
      );

      toolBar.update();
   }

   private List<JComponent> modeButtons() {
      workingModeEditButton.setToolTipText("Edit regions");
      workingModeEditButton.addItemListener(modeButtonSelectListener(EchogramWorkingMode.EDIT));

      workingModeDeleteButton.setToolTipText("Frequency-dependent deletion of data");
      workingModeDeleteButton.addMouseListener(modeButtonStickyListener(EchogramWorkingMode.DELETE));
      workingModeDeleteButton.addItemListener(modeButtonSelectListener(EchogramWorkingMode.DELETE));

      workingModeZoomButton.setToolTipText("Zoom");
      workingModeZoomButton.addMouseListener(modeButtonStickyListener(EchogramWorkingMode.ZOOM));
      workingModeZoomButton.addItemListener(modeButtonSelectListener(EchogramWorkingMode.ZOOM));

      workingModeAddButton.setToolTipText("Add region boundaries");
      workingModeAddButton.addMouseListener(modeButtonStickyListener(EchogramWorkingMode.ADD));
      workingModeAddButton.addItemListener(modeButtonSelectListener(EchogramWorkingMode.ADD));

      GuiUtils.createButtonGroup(workingModeEditButton, workingModeDeleteButton, workingModeZoomButton, workingModeAddButton);

      return List.of(workingModeEditButton, workingModeDeleteButton, workingModeZoomButton, workingModeAddButton);
   }

   private ItemListener modeButtonSelectListener(EchogramWorkingMode workingMode) {
      return e -> {
         if (e.getStateChange() == ItemEvent.SELECTED) {
            lsss.getInterpretationSettings().getEchogramSettings().workingMode.setValue(workingMode);
         }
      };
   }

   private MouseAdapter modeButtonStickyListener(EchogramWorkingMode workingMode) {
      return new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
               lsss.getInterpretationSettings().getEchogramSettings().workingMode.setValue(workingMode);
               lsss.getInterpretationSettings().getEchogramSettings().setWorkingModeSticky();
            }
         }
      };
   }

   private void updatePreferredHorizontalSizeButtons() {
      GridConf gridConf = lsss.getConfigurationManager().getGridConf();
      preferredHorizontalSizeButtons.clear();
      for (double size : gridConf.preferredHorizontalSizes.getValue()) {
         String sizeString = Utils.toString(size) + " " + gridConf.horizontalGridUnit.getValue().getUnitString();
         JButton button = new JButton(sizeString);
         button.addActionListener(_ -> lsss.getInterpretationSettings().gotoPreferredSize(size));
         button.setToolTipText("Adjust horizontal segment size to " + sizeString);
         button.setEnabled(!lsss.getInterpretationSettings().getDataFileSet().isEmpty());
         preferredHorizontalSizeButtons.add(button);
      }
      toolBar.update();
   }

   private void updateModeButtons() {
      boolean hasData = !lsss.getInterpretationSettings().getDataFileSet().isEmpty();
      workingModeEditButton.setEnabled(hasData);
      workingModeDeleteButton.setEnabled(hasData);
      workingModeZoomButton.setEnabled(hasData);
      workingModeAddButton.setEnabled(hasData);

      EchogramWorkingMode workingMode = lsss.getInterpretationSettings().getEchogramSettings().workingMode.getValue();
      workingModeEditButton.setSelected(workingMode == EchogramWorkingMode.EDIT);
      workingModeDeleteButton.setSelected(workingMode == EchogramWorkingMode.DELETE);
      workingModeZoomButton.setSelected(workingMode == EchogramWorkingMode.ZOOM);
      workingModeAddButton.setSelected(workingMode == EchogramWorkingMode.ADD);
   }

   private void updateToolTipButton() {
      boolean on = lsss.getActions().showTooltip.get();
      toolTipButton.setToolTipText("Click to " + (on ? "hide" : "show") + " tooltip");
      toolTipButton.setSelected(on);
   }

   private void updateUiFromPackages() {
      packageComponents.clear();
      for (UserDefinedPackage userDefinedPackage : lsss.getConfigurationManager().getAppMiscConf().getPackagesConf().getUserDefinedPackages()) {
         List<ToolbarButtonInfo> toolbarButtonInfos = userDefinedPackage.info.toolbarButtons;
         if (toolbarButtonInfos.isEmpty()) {
            continue;
         }
         packageComponents.add(new GroupingToolBar.ToolBarSeparator());
         for (ToolbarButtonInfo info : toolbarButtonInfos) {
            JButton button = new JButton();
            packageComponents.add(button);
            SvgIcon icon = userDefinedPackage.uiInfoToIcon(info).orElse(null);
            if (icon != null) {
               icon.on(button);
               button.setText(info.text);
            } else {
               button.setText(userDefinedPackage.uiInfoToEffectiveText(info));
            }
            button.setToolTipText(userDefinedPackage.uiInfoToToolTip(info));
            button.addActionListener(e -> userDefinedPackage.runAction(info, new ActionArgument(e)));
         }
      }
      toolBar.update();
   }

   private void updateVisibility() {
      getToolBar().setVisible(lsss.getActions().showToolbar.get());
   }

   JToolBar getToolBar() {
      return toolBar.getToolBar();
   }
}
