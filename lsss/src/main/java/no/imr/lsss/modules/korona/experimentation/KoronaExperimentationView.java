package no.imr.lsss.modules.korona.experimentation;

import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleEditor;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.survey.preprocessing.PreprocessingSetup;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.swing.ComboBoxListModel;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

final class KoronaExperimentationView extends BaseViewModule.BaseView {
   private final KoronaExperimentationModule module;
   private final LSSS lsss;
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final JPanel processLabelPanel = GuiUtils.createPanel(new FlowLayout(FlowLayout.LEFT), Color.WHITE, new JLabel("Select region(s) to process"));
   private final JButton editButton = new JButton("Edit");
   private final JComboBox<String> preprocessingSetupComboBox = new JComboBox<>();
   private final JProgressBar progressBar = new JProgressBar(0, 1_000_000_000);

   KoronaExperimentationView(KoronaExperimentationModule module) {
      super(module);

      this.module = module;
      lsss = module.getLSSS();

      JButton processButton = new JButton("Process");
      processButton.setToolTipText("Start processing on the selected regions");
      processButton.addActionListener(_ -> module.startProcessing());

      editButton.setToolTipText("Edit the selected processing setup");
      editButton.addActionListener(_ -> showEditor());

      preprocessingSetupComboBox.setRenderer(new DefaultListCellRenderer() {
         @Override
         public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setToolTipText(getProcessingTooltip(index));
            return this;
         }
      });
      preprocessingSetupComboBox.addActionListener(_ -> module.preprocessingSetup.setIntValue(preprocessingSetupComboBox.getSelectedIndex() + 1));

      progressBar.setStringPainted(true);
      progressBar.setString("");

      Box box = Box.createVerticalBox();
      box.add(GuiUtils.createPanel(new FlowLayout(FlowLayout.LEFT), Color.WHITE, preprocessingSetupComboBox));
      box.add(GuiUtils.createPanel(new FlowLayout(FlowLayout.LEFT), Color.WHITE, processButton, editButton));
      box.add(processLabelPanel);
      box.add(GuiUtils.createVerticalFiller());

      mainPanel.setBackground(Color.WHITE);
      mainPanel.add(box);
      mainPanel.add(progressBar, BorderLayout.SOUTH);
      mainPanel.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseEntered(MouseEvent e) {
            module.validateSetup();
         }
      });

      update();
   }

   @Override
   public JComponent getComponent() {
      return mainPanel;
   }

   void update() {
      boolean canProcess = !lsss.getRegionManager().getSelectedRegions().isEmpty();
      processLabelPanel.setVisible(!canProcess);

      List<PreprocessingSetup> preprocessingSetups = getPreprocessingSetups();
      if (preprocessingSetupComboBox.getItemCount() != preprocessingSetups.size()) {
         preprocessingSetupComboBox.setModel(new ComboBoxListModel<>(null,
               IntStream.rangeClosed(1, preprocessingSetups.size())
                     .mapToObj(i -> "Preprocessing setup # " + i)
                     .toList()));
      }
      preprocessingSetupComboBox.setSelectedIndex(module.preprocessingSetup.getIntValue() - 1);
   }

   private List<PreprocessingSetup> getPreprocessingSetups() {
      return lsss.getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getPreprocessingSetups();
   }

   private @Nullable String getProcessingTooltip(int index) {
      List<PreprocessingSetup> preprocessingSetups = getPreprocessingSetups();
      if (index < 0 || index >= preprocessingSetups.size()) {
         return null;
      }
      PreprocessingSetup preprocessingSetup = preprocessingSetups.get(index);
      HtmlStringBuilder tooltip = new HtmlStringBuilder();
      String comment = preprocessingSetup.comment.getValue();
      if (!comment.isEmpty()) {
         tooltip.text(comment).html("<br>");
      }
      Path cfsFile = preprocessingSetup.cfsFile.getFile();
      if (cfsFile != null) {
         tooltip.text(cfsFile.getFileName().toString());
      }
      return tooltip.build();
   }

   private void showEditor() {
      PreprocessingSetup preprocessingSetup = module.getPreprocessingSetup();
      ModuleContainer moduleContainer;
      try {
         moduleContainer = preprocessingSetup.createModuleContainer();
      } catch (IOException e) {
         lsss.showError(mainPanel, "Error loading module setup", e);
         return;
      }

      Path cfsFile = moduleContainer.getConfigFileSettings().getFile();
      if (cfsFile == null) {
         lsss.getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().showInConfigurationDialog();
         return;
      }

      Path cdsFile = moduleContainer.getConfigFileSettings().getModuleConfigurationFile();
      if (cdsFile == null) {
         preprocessingSetup.cfsFile.getEditor().edit(mainPanel, true);
         return;
      }

      editButton.setEnabled(false);
      preprocessingSetupComboBox.setEnabled(false);
      module.setEditingModuleContainer(moduleContainer);
      new ModuleEditor(moduleContainer, true, mainPanel)
            .setOnClose(ok -> {
               editButton.setEnabled(true);
               preprocessingSetupComboBox.setEnabled(true);
               if (ok) {
                  try {
                     moduleContainer.writeConfiguration(cdsFile);
                     moduleContainer.getConfigFileSettings().save(cfsFile);
                  } catch (IOException e) {
                     lsss.showError(mainPanel, "Error saving module setup", e);
                  }
               }
               module.setEditingModuleContainer(null);
            })
            .setModeless()
            .show();
   }

   void setProgress(double progress, boolean showText) {
      int max = progressBar.getMaximum();
      int value = (int) (progress * max);
      progressBar.setValue(value);
      progressBar.setString(showText ? 100L * value / max + " %" : "");
   }
}
