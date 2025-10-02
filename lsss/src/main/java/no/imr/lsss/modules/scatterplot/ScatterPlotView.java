package no.imr.lsss.modules.scatterplot;

import no.imr.lsss.modules.BaseViewModule;
import no.imr.tools.plot.PlotUtils;
import org.jfree.chart.ChartPanel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

final class ScatterPlotView extends BaseViewModule.BaseView {
   private final ScatterPlotModule module;
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final ChartPanel chartPanel;

   ScatterPlotView(ScatterPlotModule module) {
      super(module);

      this.module = module;
      chartPanel = PlotUtils.newChartPanel(module.getChart());
      mainPanel.add(chartPanel);
      mainPanel.add(module.getFrequencySelectionPanel().getComponent(), BorderLayout.SOUTH);
   }

   @Override
   public JComponent getComponent() {
      return mainPanel;
   }

   void updateChart() {
      chartPanel.setChart(module.getChart());
   }
}
