package no.imr.tools.visualizer;

import no.imr.tools.Utils;
import no.imr.tools.math.Histogram1D;
import no.imr.tools.math.NiceNumber;
import no.imr.tools.plot.Histogram1DDataset;
import no.imr.tools.range.DoubleRange;
import no.imr.tools.range.DoubleRangeBuilder;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.RotatedLabel;
import no.imr.tools.swing.icons.MiscIcons;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.ui.RectangleEdge;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

final class ItemHistogram<T> extends ItemView<T> {
   private final ItemContainer<T> itemContainer;
   private final XYPlot plot = new XYPlot();
   private final JLabel xLabel = new JLabel();
   private final JPanel panel = new JPanel(new BorderLayout());
   private final Preferences myPreferences;
   private Histogram1D allHistogram = Histogram1D.fromDeltaAndBinCount(0, 0, 0);
   private String format = "%f";

   private ItemFeature<T> xFeature;

   ItemHistogram(ItemVisualizerConfig config, List<ItemFeature<T>> allFeatures, ItemContainer<T> itemContainer) {
      this.itemContainer = itemContainer;

      List<ItemFeature<T>> features = allFeatures.stream()
            .filter(Predicate.not(ItemFeature.Text.class::isInstance))
            .toList();

      myPreferences = config.getPreferences().node("histogram");
      xFeature = ItemUtils.findFeature(features, myPreferences.get("x", ""), 0);

      NumberAxis yAxis = new NumberAxis();
      yAxis.setAutoRangeIncludesZero(true);
      yAxis.setAutoRangeStickyZero(true);
      yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
      plot.setRangeAxis(yAxis);
      plot.setDomainPannable(true);

      plot.setRenderer(0, createRenderer(Color.RED));
      plot.setRenderer(1, createRenderer(Color.BLACK));

      xLabel.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            xFeature = Utils.shift(features, xFeature, e.isShiftDown() ? -1 : 1);
            update();
         }
      });
      JMenuBar xMenuBar = new JMenuBar();
      JMenu xMenu = MiscIcons.MENU.on(new JMenu());
      GuiUtils.autoCreateContentMenu(xMenu, () -> {
         for (ItemFeature<T> feature : features) {
            JMenuItem item = MiscIcons.check(xFeature == feature).on(xMenu.add(feature.name));
            item.addActionListener(_ -> {
               xFeature = feature;
               update();
            });
         }
      });
      xMenuBar.add(xMenu);
      GridBag xGridBag = new GridBag();
      xGridBag.addWithLineBreak(xLabel, Box.createHorizontalStrut(10), xMenuBar);

      GridBag yGridBag = new GridBag();
      yGridBag.addWithLineBreak(new RotatedLabel("Count"));

      panel.add(new ItemHistogramChartPanel(this, new JFreeChart(null, null, plot, false)));
      panel.add(xGridBag.getPanel(), BorderLayout.SOUTH);
      panel.add(yGridBag.getPanel(), BorderLayout.WEST);
   }

   private XYBarRenderer createRenderer(Color color) {
      XYBarRenderer renderer = new XYBarRenderer();
      renderer.setShadowVisible(false);
      renderer.setUseYInterval(true);
      renderer.setBarPainter(new StandardXYBarPainter() {
         @Override
         public void paintBar(Graphics2D g2, XYBarRenderer renderer, int row, int column, RectangularShape bar, RectangleEdge base) {
            // This is to avoid grey stripes between bars.
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            Rectangle2D rectangle = new Rectangle2D.Double(bar.getX(), bar.getY(), bar.getWidth() + 0.1, bar.getHeight());
            super.paintBar(g2, renderer, row, column, rectangle, base);
         }
      });
      renderer.setDrawBarOutline(false);
      renderer.setSeriesPaint(0, color);
      renderer.setDefaultToolTipGenerator((_, _, item) -> getToolTip(item));
      return renderer;
   }

   private String getToolTip(int index) {
      return switch (xFeature) {
         case ItemFeature.Time<?> timeFeature -> {
            yield "<html>[ " + timeFeature.dateTimeFormatter.format(Instant.ofEpochMilli(Math.round(allHistogram.indexToValue(index))))
                  + "<br>, " + timeFeature.dateTimeFormatter.format(Instant.ofEpochMilli(Math.round(allHistogram.indexToValue(index + 1))))
                  + " ) : " + allHistogram.getCounts()[index];
         }
         case ItemFeature.Category<?> categoryFeature -> {
            yield categoryFeature.getCategories().get(index)
                  + " : " + allHistogram.getCounts()[index];
         }
         default -> {
            yield "[ " + Utils.format(format, allHistogram.indexToValue(index))
                  + ", " + Utils.format(format, allHistogram.indexToValue(index + 1))
                  + " ) : " + allHistogram.getCounts()[index];
         }
      };
   }

   @Override
   JComponent getComponent() {
      return panel;
   }

   void select(DoubleRange range, MouseEvent mouseEvent) {
      Set<T> items = itemContainer.getAllItems().stream()
            .filter(item -> range.contains(xFeature.itemToDouble.applyAsDouble(item)))
            .collect(Collectors.toSet());
      itemContainer.selectItems(items, mouseEvent);
   }

   @Override
   void selectFeatures(ItemFeature<T> x, ItemFeature<T> y) {
      xFeature = x;
   }

   @Override
   void update() {
      myPreferences.put("x", xFeature.name);

      xLabel.setText(xFeature.getNameAndUnit());

      plot.setDomainAxis(xFeature.toAxis(true));

      Histogram1D selectedHistogram;
      if (xFeature instanceof ItemFeature.Category<?> categoryFeature) {
         int n = categoryFeature.getCategories().size();
         selectedHistogram = Histogram1D.fromDeltaAndBinCount(-0.5, 1, n);
         allHistogram = Histogram1D.fromDeltaAndBinCount(-0.5, 1, n);
      } else {
         DoubleRangeBuilder rangeBuilder = new DoubleRangeBuilder();
         values(itemContainer.getAllItems()).forEach(rangeBuilder::expand);
         DoubleRange valueRange = rangeBuilder.toDoubleRange().expandToIncludeMax();
         double delta = xFeature instanceof ItemFeature.Time<?>
               ? 1000 * NiceNumber.niceSecond(0.001 * valueRange.getSize() / 200, true) // valueRange is milliseconds
               : NiceNumber.niceNumber(valueRange.getSize() / 200, true);
         int precision = delta >= 1 ? 0 : (int) Math.ceil(-Math.log10(delta));
         format = "%." + precision + "f";
         selectedHistogram = Histogram1D.fromDelta(valueRange, delta);
         allHistogram = Histogram1D.fromDelta(valueRange, delta);
      }

      values(itemContainer.getSelectedItems()).forEach(selectedHistogram::addValue);
      values(itemContainer.getAllItems()).forEach(allHistogram::addValue);

      Histogram1DDataset selectedDataset = new Histogram1DDataset("", selectedHistogram);

      Histogram1DDataset otherDataset = new Histogram1DDataset("", allHistogram);
      otherDataset.setStartCounts(selectedHistogram.getCounts());

      plot.setDataset(0, selectedDataset);
      plot.setDataset(1, otherDataset);
   }

   private DoubleStream values(Collection<T> items) {
      return items.stream()
            .mapToDouble(xFeature.itemToDouble)
            .filter(v -> !Double.isNaN(v));
   }
}
