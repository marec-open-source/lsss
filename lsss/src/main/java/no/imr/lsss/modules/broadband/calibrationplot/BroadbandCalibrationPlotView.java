package no.imr.lsss.modules.broadband.calibrationplot;

import no.imr.lsss.modules.BaseViewModule;
import no.imr.tools.Utils;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.swing.WrappingFlowLayout;
import org.jfree.chart.ChartPanel;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

final class BroadbandCalibrationPlotView extends BaseViewModule.BaseView {
   private final BroadbandCalibrationPlotModule module;
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final ChartPanel chartPanel;

   BroadbandCalibrationPlotView(BroadbandCalibrationPlotModule module) {
      super(module);

      this.module = module;
      chartPanel = PlotUtils.newChartPanel(module.getChart());
      module.getFrequencyPlotMarker().addMouseListener(chartPanel);
      mainPanel.setBackground(Color.WHITE);
      mainPanel.add(chartPanel);
      mainPanel.add(makeSelectionPanel(), BorderLayout.SOUTH);
   }

   @Override
   public JComponent getComponent() {
      return mainPanel;
   }

   void updateChart() {
      chartPanel.setChart(module.getChart());
   }

   private JComponent makeSelectionPanel() {
      JPanel panel = new JPanel(new WrappingFlowLayout());
      panel.setMinimumSize(new Dimension(10, 10));
      panel.setBackground(Color.WHITE);
      for (CalibrationPlotParameter parameter : CalibrationPlotParameter.values()) {
         JCheckBox checkBox = new JCheckBox(parameter.shortName, module.isSelected(parameter));
         checkBox.setBackground(Color.WHITE);
         checkBox.setToolTipText(Utils.nameAndUnit(parameter.fullName, parameter.unit));
         checkBox.addItemListener(_ -> module.setSelected(parameter, checkBox.isSelected()));
         panel.add(checkBox);
      }
      return panel;
   }
}
