package no.imr.korona.computation.broadband.transferfunction;

import no.imr.tools.plot.Graph;
import no.imr.tools.plot.Plotter;
import org.apache.commons.numbers.complex.Complex;
import org.jfree.chart.ChartPanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.util.List;

final class IdealBandPassTransferFunctionMain {
   private IdealBandPassTransferFunctionMain() {
   }

   static void main() {
      SwingUtilities.invokeLater(IdealBandPassTransferFunctionMain::run);
   }

   private static void run() {
      IdealBandPassTransferFunction transferFunction = new IdealBandPassTransferFunction(10, 20, 5);
      Graph graph = new Graph();
      int n = 1000;
      for (int i = 0; i < n; i++) {
         double f = 30.0 * i / (n - 1);
         Complex gain = transferFunction.evaluateGainFunction(f);
         graph.addPoint(f, gain.getReal());
      }
      ChartPanel chartPanel = new Plotter(List.of(graph))
            .title("Bandpass filter")
            .xAxis("Frequency")
            .yAxis("Gain")
            .createChartPanel();

      JFrame frame = new JFrame();
      frame.add(chartPanel);
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.setSize(1000, 800);
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
   }
}
