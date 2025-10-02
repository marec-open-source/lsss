package no.imr.tools.plot;

import no.imr.tools.logging.Log;
import no.imr.tools.swing.GuiUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jspecify.annotations.Nullable;

import java.awt.Graphics;
import java.util.logging.Level;

public class PlotChartPanel extends ChartPanel {
   public PlotChartPanel(@Nullable JFreeChart chart) {
      this(chart, true);
   }

   public PlotChartPanel(@Nullable JFreeChart chart, boolean mouseWheelZoom) {
      super(chart, false);

      // Avoid scaling
      setMinimumDrawWidth(10);
      setMinimumDrawHeight(10);
      setMaximumDrawWidth(10_000);
      setMaximumDrawHeight(10_000);

      if (mouseWheelZoom) {
         PlotUtils.enableMouseWheelZooming(this);
      }
   }

   @Override
   public void paintComponent(Graphics g) {
      try {
         super.paintComponent(g);
      } catch (Exception e) {
         String info = GuiUtils.getInfoProperty(this);
         if (info == null) {
            throw e;
         }
         Log.global.log(Level.WARNING, "Error painting " + info, e);
      }
   }
}
