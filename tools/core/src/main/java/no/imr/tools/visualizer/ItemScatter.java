package no.imr.tools.visualizer;

import no.imr.tools.Utils;
import no.imr.tools.listening.Listener;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.RotatedLabel;
import no.imr.tools.swing.icons.MiscIcons;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

final class ItemScatter<T> extends ItemView<T> {
   private final ItemVisualizerConfig config;
   private final List<ItemFeature<T>> allFeatures;
   private final List<ItemFeature<T>> features;
   private final ItemContainer<T> itemContainer;
   private final XYPlot plot = new XYPlot();
   private final JLabel xLabel = new JLabel();
   private final JLabel yLabel = new RotatedLabel();
   private final JPanel panel = new JPanel(new BorderLayout());
   private final Preferences myPreferences;

   private ItemFeature<T> xFeature;
   private ItemFeature<T> yFeature;

   ItemScatter(ItemVisualizerConfig config, List<ItemFeature<T>> allFeatures, ItemContainer<T> itemContainer) {
      this.config = config;
      this.allFeatures = allFeatures;
      features = allFeatures.stream()
            .filter(Predicate.not(ItemFeature.Text.class::isInstance))
            .toList();
      this.itemContainer = itemContainer;

      myPreferences = config.getPreferences().node("scatter");
      xFeature = ItemUtils.findFeature(features, myPreferences.get("x", ""), 0);
      yFeature = ItemUtils.findFeature(features, myPreferences.get("y", ""), 1);

      plot.setDomainPannable(true);
      plot.setRangePannable(true);

      Listener.of(this::updateRenderers).addToAndNotify(
            config.selectedDotSize,
            config.unselectedDotSize
      );

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
            item.addActionListener(e -> {
               xFeature = feature;
               update();
            });
         }
      });
      xMenuBar.add(xMenu);
      GridBag xGridBag = new GridBag();
      xGridBag.addWithLineBreak(xLabel, Box.createHorizontalStrut(10), xMenuBar);

      yLabel.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            yFeature = Utils.shift(features, yFeature, e.isShiftDown() ? -1 : 1);
            update();
         }
      });
      JMenuBar yMenuBar = new JMenuBar();
      JMenu yMenu = MiscIcons.MENU.on(new JMenu());
      GuiUtils.autoCreateContentMenu(yMenu, () -> {
         for (ItemFeature<T> feature : features) {
            JMenuItem item = MiscIcons.check(yFeature == feature).on(yMenu.add(feature.name));
            item.addActionListener(e -> {
               yFeature = feature;
               update();
            });
         }
      });
      yMenuBar.add(yMenu);
      GridBag yGridBag = new GridBag();
      yGridBag.addWithLineBreak(yLabel);
      yGridBag.addWithLineBreak(Box.createVerticalStrut(10));
      yGridBag.addWithLineBreak(yMenuBar);

      panel.add(new ItemScatterChartPanel<>(this, new JFreeChart(null, null, plot, false)));
      panel.add(xGridBag.getPanel(), BorderLayout.SOUTH);
      panel.add(yGridBag.getPanel(), BorderLayout.WEST);
   }

   private void updateRenderers() {
      plot.setRenderer(0, createRenderer(Color.RED, config.selectedDotSize.getIntValue()));
      plot.setRenderer(1, createRenderer(Color.BLACK, config.unselectedDotSize.getIntValue()));
   }

   private static XYDotRenderer createRenderer(Color color, int dotSize) {
      XYDotRenderer renderer = new XYDotRenderer();
      renderer.setDotWidth(dotSize);
      renderer.setDotHeight(dotSize);
      renderer.setSeriesPaint(0, color);
      return renderer;
   }

   List<ItemFeature<T>> getAllFeatures() {
      return allFeatures;
   }

   ItemContainer<T> getItemContainer() {
      return itemContainer;
   }

   ItemFeature<T> getXFeature() {
      return xFeature;
   }

   ItemFeature<T> getYFeature() {
      return yFeature;
   }

   @Override
   JComponent getComponent() {
      return panel;
   }

   void select(Rectangle2D rectangle, MouseEvent mouseEvent) {
      Set<T> items = itemContainer.getAllItems().stream()
            .filter(item -> rectangle.contains(xFeature.itemToDouble.applyAsDouble(item), yFeature.itemToDouble.applyAsDouble(item)))
            .collect(Collectors.toSet());
      itemContainer.selectItems(items, mouseEvent);
   }

   @Override
   void selectFeatures(ItemFeature<T> x, ItemFeature<T> y) {
      xFeature = x;
      yFeature = y;
   }

   @Override
   void update() {
      myPreferences.put("x", xFeature.name);
      myPreferences.put("y", yFeature.name);

      xLabel.setText(xFeature.getNameAndUnit());
      yLabel.setText(yFeature.getNameAndUnit());

      plot.setDomainAxis(xFeature.toAxis(true));
      plot.setRangeAxis(yFeature.toAxis(false));

      Set<T> selectedItems = itemContainer.getSelectedItems();
      List<T> otherItems = itemContainer.getAllItems().stream()
            .filter(Predicate.not(selectedItems::contains))
            .toList();

      plot.setDataset(0, new ItemXYDataset<>("", List.copyOf(selectedItems), xFeature.itemToDouble, yFeature.itemToDouble));
      plot.setDataset(1, new ItemXYDataset<>("", otherItems, xFeature.itemToDouble, yFeature.itemToDouble));
   }
}
