package no.imr.lsss.util;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.tools.swing.GuiUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

public final class FrequencyPlotMarker {
   private final InterpretationSettings interpretationSettings;
   private ValueMarker marker = new ValueMarker(Double.NaN, Color.DARK_GRAY, GuiUtils.STROKE_1);
   private boolean inside;

   public FrequencyPlotMarker(LSSS lsss) {
      interpretationSettings = lsss.getInterpretationSettings();
   }

   public void addMarker(XYPlot plot) {
      // Create new marker to avoid memory leak, #1423.
      marker = new ValueMarker(Double.NaN, Color.DARK_GRAY, GuiUtils.STROKE_1);
      updateMarker();
      plot.addDomainMarker(marker);
   }

   public void updateMarker() {
      Float kHz = inside ? null : interpretationSettings.mouseover().getKHz();
      double value = kHz != null ? kHz : Double.NaN;
      if (Double.compare(marker.getValue(), value) != 0) { // Use compare (not ==) to handle NaN.
         marker.setValue(value);
      }
   }

   public void addMouseListener(ChartPanel chartPanel) {
      FrequencyMouseListener mouseListener = new FrequencyMouseListener(chartPanel);
      chartPanel.addMouseListener(mouseListener);
      chartPanel.addMouseMotionListener(mouseListener);
   }

   private final class FrequencyMouseListener extends MouseAdapter {
      private final ChartPanel chartPanel;

      private FrequencyMouseListener(ChartPanel chartPanel) {
         this.chartPanel = chartPanel;
      }

      @Override
      public void mouseEntered(MouseEvent e) {
         inside = true;
      }

      @Override
      public void mouseExited(MouseEvent e) {
         inside = false;
         interpretationSettings.mouseover().setKHz(null);
      }

      @Override
      public void mouseDragged(MouseEvent e) {
         mouseMoved(e);
      }

      @Override
      public void mouseMoved(MouseEvent e) {
         Float kHz = getKHz(e);
         interpretationSettings.mouseover().setKHz(kHz);
      }

      private @Nullable Float getKHz(MouseEvent e) {
         JFreeChart chart = chartPanel.getChart();
         if (chart == null) {
            return null;
         }
         XYPlot plot = chart.getXYPlot();
         if (plot == null) {
            return null;
         }
         Rectangle2D screenDataArea = chartPanel.getScreenDataArea();
         if (!screenDataArea.contains(e.getX(), e.getY())) {
            return null;
         }
         ValueAxis domainAxis = plot.getDomainAxis();
         if (domainAxis == null) {
            return null;
         }
         return (float) domainAxis.java2DToValue(e.getX(), screenDataArea, plot.getDomainAxisEdge());
      }
   }
}
