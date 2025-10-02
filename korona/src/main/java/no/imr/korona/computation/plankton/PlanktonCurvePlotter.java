package no.imr.korona.computation.plankton;

import no.imr.korona.computation.plankton.models.BackscatterModel;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WhenShowingListening;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jspecify.annotations.Nullable;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

final class PlanktonCurvePlotter {
   private final PlanktonInversionModule module;
   private final @Nullable PlanktonFile planktonFile;
   private final XYSeriesCollection plotData = new XYSeriesCollection();
   private final JFreeChart chart = ChartFactory.createXYLineChart("Reduced TS curves", "ka", "TSr [dB]", plotData, PlotOrientation.VERTICAL, true, true, false);

   PlanktonCurvePlotter(PlanktonInversionModule module) {
      this.module = module;
      planktonFile = loadPlanktonFile(module);

      JDialog dialog = new JDialog((Window) null, "Backscatter models");
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.getContentPane().add(PlotUtils.newChartPanel(chart));
      dialog.pack();
      dialog.setVisible(true);

      chart.getXYPlot().setDomainPannable(true);
      chart.getXYPlot().setRangePannable(true);

      WhenShowingListening.connect(dialog, module.getParameters(), this::updatePlotData);
   }

   private static @Nullable PlanktonFile loadPlanktonFile(PlanktonInversionModule module) {
      Path file = module.getModuleContainer().getConfigFileSettings().getFile(PlanktonFileService.NAME);
      if (file == null) {
         return null;
      }
      try {
         return new PlanktonFile(file);
      } catch (IOException e) {
         JOptionPane.showMessageDialog(null, "Error loading plankton file:\n" + file, "Error", JOptionPane.ERROR_MESSAGE);
         return null;
      }
   }

   private void updatePlotData() {
      plotData.removeAllSeries();

      double highestWaveNumber = module.plotMaxRange.getDoubleValue();

      double soundSpeed = 1500;
      double piOverC = Math.PI / soundSpeed;
      double samplingRate = module.plotResolution.getDoubleValue();

      double smallestFreq = Double.MAX_VALUE;
      double largestFreq = -Double.MAX_VALUE;
      for (PlanktonInversionModule.FrequencyParameter frequencyParameter : module.activeFrequencies.getParameters()) {
         if (module.useAllFrequencies.getBooleanValue() || frequencyParameter.getBooleanValue()) {
            int kHz = frequencyParameter.getKHz();
            smallestFreq = Math.min(smallestFreq, kHz * 1000);
            largestFreq = Math.max(largestFreq, kHz * 1000);
         }
      }

      int seriesNumber = 0;
      for (PlanktonScatterer<? extends BackscatterModel> planktonScatterer : module.getSelectedScatterers()) {
         seriesNumber = addBackscatterCurve(planktonScatterer, smallestFreq, largestFreq, piOverC, samplingRate, highestWaveNumber, seriesNumber);
      }
   }

   private int addBackscatterCurve(PlanktonScatterer<?> planktonScatterer, double smallestFreq,
                                   double largestFreq, double piOverC, double samplingRate, double highestWaveNumber,
                                   int seriesNumber) {
      boolean plotActiveRange = planktonFile != null;
      String legend = planktonScatterer.getDescription();
      XYSeries series = new XYSeries(legend);
      double smallestUsedKa = Double.MAX_VALUE;
      double largestUsedKa = -Double.MAX_VALUE;
      if (plotActiveRange) {
         List<PlanktonRectangle> planktonRectangles = planktonFile.getPlanktonRectangles().get(planktonScatterer.getPlanktonCategory().getLegend());

         if (planktonRectangles != null) {
            for (PlanktonRectangle planktonRectangle : planktonRectangles) {
               if (planktonRectangle.isUse()) {
                  double[] dividers = planktonRectangle.getSizeHistogram().getDividers();
                  smallestUsedKa = Math.min(smallestUsedKa, dividers[0] * smallestFreq * 2 * piOverC);
                  largestUsedKa = Math.max(largestUsedKa, dividers[dividers.length - 1] * largestFreq * 2 * piOverC);
               }
            }
         }
      }
      XYSeries activeSeries = new XYSeries(legend + " Used part");

      for (double ka = samplingRate; ka < highestWaveNumber; ka += samplingRate) {
         double value = planktonScatterer.getBackscatterModel().getReducedTS(ka);
         series.add(ka, value);
         if (plotActiveRange && ka >= smallestUsedKa && ka <= largestUsedKa) {
            activeSeries.add(ka, value);
         }
      }
      plotData.addSeries(series);
      chart.getXYPlot().getRenderer().setSeriesPaint(seriesNumber, planktonScatterer.getPlanktonCategory().getColor());
      seriesNumber++;

      if (plotActiveRange) {
         plotData.addSeries(activeSeries);
         chart.getXYPlot().getRenderer().setSeriesPaint(seriesNumber, planktonScatterer.getPlanktonCategory().getColor());
         chart.getXYPlot().getRenderer().setSeriesStroke(seriesNumber, GuiUtils.STROKE_3);
         chart.getXYPlot().getRenderer().setSeriesVisibleInLegend(seriesNumber, false);
         seriesNumber++;
      }
      return seriesNumber;
   }
}
