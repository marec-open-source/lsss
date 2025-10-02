package no.imr.tools.misc.test;

import no.imr.tools.math.ComplexArray;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.Plotter;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.function.IntToDoubleFunction;

public final class TestPlot {
   private TestPlot() {
   }

   public static void show(List<Graph> graphs) {
      Plotter plotter = new Plotter(graphs)
            .title("Debug")
            .xAxis("Index")
            .yAxis("Value");
      if (graphs.stream().anyMatch(graph -> !graph.getName().isEmpty())) {
         plotter.showLegends();
      }
      SwingUtilities.invokeLater(() -> {
         JFrame frame = new JFrame("Debug");
         frame.getContentPane().add(plotter.createChartPanel());
         frame.setSize(1000, 1000);
         frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
         frame.setVisible(true);
      });
   }

   public static void show(Graph... graphs) {
      show(List.of(graphs));
   }

   public static void show(float[] values) {
      show(graph("", values));
   }

   public static void show(double[] values) {
      show(graph("", values));
   }

   public static void show(ComplexArray values) {
      show(reGraph(values), imGraph(values), absGraph(values));
   }

   public static Graph graph(String name, int endIndex, IntToDoubleFunction indexToValue) {
      Graph graph = new Graph(name);
      for (int i = 0; i < endIndex; i++) {
         graph.addPoint(i, indexToValue.applyAsDouble(i));
      }
      return graph;
   }

   public static Graph graph(String name, float[] values) {
      return graph(name, values.length, i -> values[i]);
   }

   public static Graph graph(String name, double[] values) {
      return graph(name, values.length, i -> values[i]);
   }

   public static Graph reGraph(ComplexArray values) {
      return graph("re", values.length(), values::re);
   }

   public static Graph imGraph(ComplexArray values) {
      return graph("im", values.length(), values::im);
   }

   public static Graph absGraph(ComplexArray values) {
      return graph("abs", values.length(), values::abs);
   }
}
