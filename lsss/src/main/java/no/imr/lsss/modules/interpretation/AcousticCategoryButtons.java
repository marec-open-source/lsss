package no.imr.lsss.modules.interpretation;

import no.imr.korona.region.ChannelInterpretation;
import no.imr.korona.region.Interpretation;
import no.imr.korona.region.InterpretationContainer;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WrappingFlowLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A panel with toggle buttons for the acoustic categories selected for the current survey.
 */
public final class AcousticCategoryButtons {
   private final JPanel buttonsPanel = new JPanel(new WrappingFlowLayout(FlowLayout.LEFT, InterpretationModuleView.STORE_PANEL_HGAP, InterpretationModuleView.STORE_PANEL_VGAP));
   private boolean enabled;
   private final Map<AcousticCategory, JToggleButton> acousticCategoryButtons = new HashMap<>();
   private final LSSS lsss;
   private final InterpretationManager interpretationManager;
   private final ChangeManager changeManager = new ChangeManager();

   public AcousticCategoryButtons(LSSS lsss, InterpretationManager interpretationManager) {
      this.lsss = lsss;
      this.interpretationManager = interpretationManager;

      buttonsPanel.setBackground(InterpretationModuleView.BACKGROUND_COLOR);

      GuiListeners.later(this::rebuildAcousticCategoryButtons).addToAndNotify(
            lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryChangeManager(),
            lsss.getConfigurationManager().getAppMiscConf().useEnglish
      );
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public Map<AcousticCategory, JToggleButton> getMap() {
      return acousticCategoryButtons;
   }

   public JComponent getComponent() {
      return buttonsPanel;
   }

   private void rebuildAcousticCategoryButtons() {
      acousticCategoryButtons.clear();
      buttonsPanel.removeAll();

      List<AcousticCategory> acousticCategories = lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getSelectedCategories();
      if (acousticCategories.isEmpty()) {
         JLabel label = new JLabel("No acoustic categories selected");
         label.setEnabled(false);
         buttonsPanel.add(label);
      }
      for (AcousticCategory acousticCategory : acousticCategories) {
         JToggleButton toggleButton = new JToggleButton(lsss.getConfigurationManager().getLanguageUtils().getAcCatInitials(acousticCategory));
         acousticCategoryButtons.put(acousticCategory, toggleButton);
         buttonsPanel.add(toggleButton);
         toggleButton.setEnabled(enabled);
         toggleButton.setMargin(InterpretationModuleView.EMPTY_INSETS);
         toggleButton.setBackground(InterpretationModuleView.BACKGROUND_COLOR);
         String toolTipText = new HtmlStringBuilder()
               .text(lsss.getConfigurationManager().getLanguageUtils().getAcCatName(acousticCategory))
               .text(" (" + acousticCategory.getCompId().getAcousticCategory() + ")")
               .html("<br>").text("Click to add with remaining assignment")
               .html("<br>").text("Shift + Click to add as rest category")
               .build();
         toggleButton.setToolTipText(toolTipText);

         toggleButton.addActionListener(e -> {
            interpretationManager.getInterpretationContainers().stream()
                  .filter(InterpretationContainer::isWritable)
                  .map(InterpretationContainer::getInterpretation)
                  .forEach(toggleButton.isSelected()
                        ? interpretation -> addAcousticCategory(interpretation, acousticCategory, e)
                        : interpretation -> interpretation.removeAcousticCategory(acousticCategory.getCompId().getAcousticCategory()));

            interpretationManager.interpretationChanged();
            changeManager.notifyListeners();
         });
      }

      Container parent = buttonsPanel.getParent();
      if (parent != null) {
         parent.validate();
         parent.repaint();
      }
   }

   private void addAcousticCategory(Interpretation interpretation, AcousticCategory acousticCategory, ActionEvent actionEvent) {
      ChannelInterpretation channelInterpretation = interpretation.getChannelInterpretation(interpretationManager.getChannel());
      channelInterpretation.setInitialized(true);
      switch (GuiUtils.getKeyModifiers(actionEvent)) {
         case ActionEvent.SHIFT_MASK -> {
            interpretation.addRestSpecies(acousticCategory.getCompId().getAcousticCategory());
         }
         default -> {
            float totalAssignment = channelInterpretation.getTotalAssignment();
            if (totalAssignment < 1) {
               channelInterpretation.setAssignment(acousticCategory.getCompId().getAcousticCategory(), 1 - totalAssignment);
            }
         }
      }
   }

   public void clearSelection() {
      for (JToggleButton toggleButton : acousticCategoryButtons.values()) {
         toggleButton.setSelected(false);
      }
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      for (JToggleButton toggleButton : acousticCategoryButtons.values()) {
         toggleButton.setEnabled(enabled);
      }
   }
}
