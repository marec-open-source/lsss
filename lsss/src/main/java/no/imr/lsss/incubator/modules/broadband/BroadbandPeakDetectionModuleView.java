package no.imr.lsss.incubator.modules.broadband;

import no.imr.korona.Korona;
import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFilterModuleConfig;
import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFiltersFileService;
import no.imr.korona.computation.broadband.notchfilter.BroadbandTemporalNotchFilterConfig;
import no.imr.korona.config.ConfigFileSettingsUtils;
import no.imr.korona.resources.KoronaHelp;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterTableGUI;
import no.imr.tools.parameter.gui.ParameterTableModel;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.SuffixFileFilter;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.WrappingFlowLayout;
import no.imr.tools.xml.XmlUtils;
import org.jfree.chart.ChartPanel;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Path;

final class BroadbandPeakDetectionModuleView extends BaseViewModule.BaseView {
   private Path lastNotchFile = Utils.getUserHome().resolve(BroadbandNotchFiltersFileService.BROADBAND_NOTCH_FILTER_XML_FILE);
   private Path lastSpectralFile = Korona.getInstallationConfigDir().resolve("NoiseSpecter.xml");

   private final BroadbandPeakDetectionModule module;
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final ChartPanel chartPanel = PlotUtils.newChartPanel(null);

   BroadbandPeakDetectionModuleView(BroadbandPeakDetectionModule module) {
      super(module);

      this.module = module;

      JPanel buttonsPanel = new JPanel(new WrappingFlowLayout(FlowLayout.LEFT));
      buttonsPanel.setBackground(Color.WHITE);

      JButton computePeaks = new JButton("Compute peaks");
      computePeaks.addActionListener(_ -> computePeaks());
      buttonsPanel.add(computePeaks);

      JButton openEditor = new JButton("Edit...");
      openEditor.addActionListener(_ -> openEditor());
      buttonsPanel.add(openEditor);

      JButton saveAs = new JButton("Save as...");
      saveAs.addActionListener(_ -> saveDialog());
      buttonsPanel.add(saveAs);

      JButton saveSpecter = new JButton("Save specter...");
      saveSpecter.addActionListener(_ -> saveSpecterDialog());
      buttonsPanel.add(saveSpecter);

      mainPanel.setBackground(Color.WHITE);
      mainPanel.setMinimumSize(new Dimension(10, 10));
      mainPanel.add(buttonsPanel, BorderLayout.NORTH);
      mainPanel.add(chartPanel);

      module.getFrequencyPlotMarker().addMouseListener(chartPanel);

      updateChart();
   }

   @Override
   public JComponent getComponent() {
      return mainPanel;
   }

   private void computePeaks() {
      ProgressView progressView = new ProgressView("Computing peaks...", 1000)
            .mainProgressAsPercentage();
      new WorkerDialog(getComponent(), progressView.getComponent())
            .start(asyncHandle -> {
               module.updateNotchFilterModuleConfig(asyncHandle, progressView.getMainProgressHandler());
            });
   }

   private void openEditor() {
      BroadbandNotchFilterModuleConfig config = module.getBroadbandNotchFilterModuleConfig();
      ParameterTableModel<BroadbandTemporalNotchFilterConfig> tableModel = new ParameterTableModel<>(BroadbandTemporalNotchFilterConfig::new, config.getBroadbandTemporalNotchFilterConfigs())
            .setEditable(true);
      ParameterTableGUI<BroadbandTemporalNotchFilterConfig> tableGUI = new ParameterTableGUI<>(tableModel);
      boolean ok = new ConfigurableGUIDialog(getComponent(), "Broadband notch filters", config)
            .setHelpID(KoronaHelp.BROADBAND_NOTCH_FILTERS)
            .setMinimumSize(800, 0)
            .setScrollable(false)
            .setGUI(tableGUI.createScrollPane())
            .show();
      if (ok) {
         module.setBroadbandNotchFilterModuleConfig(config);
      }
   }

   private JFileChooser createNotchFileChooser() {
      Path surveyFile = module.getLSSS().getSurveyManager().getSurveyFile();
      Path surveyDir = surveyFile != null ? surveyFile.getParent() : null;
      if (surveyDir != null && !FileUtils.isInDir(lastNotchFile, surveyDir)) {
         lastNotchFile = surveyDir.resolve(ConfigFileSettingsUtils.REFERENCE_FILES).resolve(BroadbandNotchFiltersFileService.BROADBAND_NOTCH_FILTER_XML_FILE);
      }
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setSelectedFile(FileUtils.toFile(lastNotchFile));
      fileChooser.setFileFilter(new SuffixFileFilter(BroadbandNotchFiltersFileService.NAME.displayName(), ".xml"));
      return fileChooser;
   }

   private void saveDialog() {
      JFileChooser fileChooser = createNotchFileChooser();
      int returnVal = fileChooser.showSaveDialog(getComponent());
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         Path selectedFile = fileChooser.getSelectedFile().toPath();
         lastNotchFile = selectedFile;
         try {
            XmlUtils.writeDocument(module.getBroadbandNotchFilterModuleConfig().toXml(), selectedFile);
         } catch (IOException e) {
            GuiUtils.showErrorDialog(getComponent(), "Error saving " + selectedFile, e);
         }
      }
   }

   private JFileChooser createSpectralFileChooser() {
      Path surveyFile = module.getLSSS().getSurveyManager().getSurveyFile();
      Path surveyDir = surveyFile != null ? surveyFile.getParent() : null;
      if (surveyDir != null && !FileUtils.isInDir(lastSpectralFile, surveyDir)) {
         lastSpectralFile = surveyDir.resolve("NoiseSpecter.xml");
      }
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setSelectedFile(FileUtils.toFile(lastSpectralFile));
      fileChooser.setFileFilter(new SuffixFileFilter("Spectral file", ".xml"));
      return fileChooser;
   }

   private void saveSpecterDialog() {
      JFileChooser fileChooser = createSpectralFileChooser();
      int returnVal = fileChooser.showSaveDialog(getComponent());
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         Path selectedFile = fileChooser.getSelectedFile().toPath();
         lastSpectralFile = selectedFile;
         try {
            XmlUtils.writeDocument(module.specterToXml(), selectedFile);
         } catch (IOException e) {
            GuiUtils.showErrorDialog(getComponent(), "Error saving " + selectedFile, e);
         }
      }
   }

   void updateChart() {
      chartPanel.setChart(module.getChart());
   }
}
