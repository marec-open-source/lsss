package no.imr.lsss.modules.plankton;

import no.imr.korona.computation.plankton.editor.PlanktonGUI;
import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.lsss.framework.config.ConfigurationManager;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.ColorIcon;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

final class PlanktonModuleView extends BaseViewModule.BaseView {
   private final PlanktonModule module;
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final JPanel histogramPanel = new JPanel(new BorderLayout());
   private String selectedCategory = "";

   PlanktonModuleView(PlanktonModule module) {
      super(module);

      this.module = module;

      JCheckBox useThresholdsCheckBox = new JCheckBox("Use thresholds");
      useThresholdsCheckBox.setToolTipText("<html>When checked only pixels with S<sub>v</sub> values within the thresholds for the current frequency are used");
      useThresholdsCheckBox.setBackground(Color.WHITE);
      GuiUtils.connect(useThresholdsCheckBox, module.getUseThresholds());

      JButton editButton = new JButton("Edit...");
      editButton.setToolTipText("Shows editor for initial size distribution");
      editButton.addActionListener(e -> edit());

      JPanel bottomPanel = new JPanel(new BorderLayout());
      bottomPanel.setBackground(Color.WHITE);
      bottomPanel.add(useThresholdsCheckBox, BorderLayout.WEST);
      bottomPanel.add(editButton, BorderLayout.EAST);

      mainPanel.setBackground(Color.WHITE);
      mainPanel.add(histogramPanel);
      mainPanel.add(bottomPanel, BorderLayout.SOUTH);

      histogramPanel.setBackground(Color.WHITE);

      updateHistogramPanel();
   }

   @Override
   public JComponent getComponent() {
      return mainPanel;
   }

   private void edit() {
      try {
         Path file = module.getPlanktonFile();
         if (file == null) {
            JOptionPane.showMessageDialog(mainPanel, "No plankton file is configured", "Error", JOptionPane.ERROR_MESSAGE);

            ConfigurationManager configurationManager = module.getLSSS().getConfigurationManager();
            configurationManager.showDialog(configurationManager.getSurveyConfiguration().getPreprocessingConf());
         } else {
            PlanktonGUI.showDialog(mainPanel, file, true);
         }
      } catch (IOException e) {
         GuiUtils.showErrorDialog(mainPanel, "Could not get plankton file", e);
      }
   }

   void updateHistogramPanel() {
      histogramPanel.removeAll();

      List<HistogramDisplay> histogramDisplays = module.getHistogramDisplays();
      if (histogramDisplays.isEmpty()) {
         histogramPanel.add(new JLabel("<html><p style='color: gray;'>No plankton inversion available", JLabel.CENTER));
      } else {
         JTabbedPane tabbedPane = new JTabbedPane();
         histogramPanel.add(tabbedPane);

         for (HistogramDisplay histogramDisplay : histogramDisplays) {
            Pic0Datagram.PlanktonCategory planktonCategory = histogramDisplay.getPlanktonCategory();
            int tabIndex = tabbedPane.getTabCount();
            tabbedPane.add(histogramDisplay.getComponent());
            tabbedPane.setToolTipTextAt(tabIndex, planktonCategory.getName());
            tabbedPane.setTabComponentAt(tabIndex, new JLabel(planktonCategory.getLegend(), new ColorIcon(planktonCategory.getColor(), 8, 8), JLabel.LEFT));
            if (planktonCategory.getLegend().equals(selectedCategory)) {
               tabbedPane.setSelectedIndex(tabIndex);
            }
         }

         tabbedPane.addChangeListener(e -> {
            int i = tabbedPane.getSelectedIndex();
            if (i != -1) {
               selectedCategory = histogramDisplays.get(i).getPlanktonCategory().getLegend();
            }
         });
      }

      histogramPanel.validate();
      histogramPanel.repaint();
   }
}
