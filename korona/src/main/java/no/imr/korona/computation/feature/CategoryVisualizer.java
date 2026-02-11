package no.imr.korona.computation.feature;

import no.imr.korona.computation.categorization.Category;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.computation.categorization.ConfiguratorEditor;
import no.imr.korona.computation.categorization.GaussDistribution;
import no.imr.korona.computation.categorization.GaussUtils;
import no.imr.korona.computation.categorization.Neighbor;
import no.imr.korona.computation.categorization.Neighborhood;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.datagrams.RegionBorderDatagram;
import no.imr.korona.data.datagrams.RegionInfoDatagram;
import no.imr.korona.data.datagrams.TBR0Datagram;
import no.imr.korona.data.datagrams.TNF0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.incubator.KoronaIncubatorFeatureToggles;
import no.imr.korona.resources.KoronaHelp;
import no.imr.korona.util.KoronaPreferences;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.Utils;
import no.imr.tools.listening.Listener;
import no.imr.tools.logging.Log;
import no.imr.tools.math.linalg.Vec2;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.PlotChartPanel;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeBuilder;
import no.imr.tools.swing.ComboBoxListModel;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.VerticalScrollablePanel;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.WrappingFlowLayout;
import no.imr.tools.swing.icons.ColorIcon;
import no.imr.tools.swing.icons.MiscIcons;
import no.marec.lsss.api.util.observing.Subscription;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Visualizes the extracted points together with the points stored in the
 * configuration file categorization.xml.
 * Includes functionality for modifying the stored points,
 * and saving them to file.
 */
public final class CategoryVisualizer {
   /**
    * The size of the visualization window.
    */
   private static final Dimension PREFERRED_SIZE = new Dimension(1000, 600);

   private @Nullable MisClassMatrixView misClassMatrixView;
   private Category.DistributionLevel distributionLevel = Category.DistributionLevel.PIXEL;

   private final class CategoryConfiguration {
      private final Set<Category.CategoryNeighborhood> visibleNeighborhoods = new HashSet<>();
      private boolean thinnedNeighborhoodVisible = true;
      private boolean plottable;
      private final CategoryPopupDialog popup;

      private CategoryConfiguration(Category category) {
         plottable = getSelectedCategoriesPreferences().getBoolean(category.getName(), true);
         popup = new CategoryPopupDialog(category, this);
      }

      private static Preferences getSelectedCategoriesPreferences() {
         return KoronaPreferences.node("selectedCategories");
      }

      private JDialog getPopup() {
         popup.refresh();
         return popup;
      }

      private Category getCategory() {
         return popup.category;
      }

      private void refreshPopup() {
         popup.refresh();
      }

      private void closePopup() {
         popup.setVisible(false);
      }

      private boolean isThinnedNeighborhoodVisible() {
         return thinnedNeighborhoodVisible;
      }

      private void setThinnedNeighborhoodVisible(boolean visible) {
         thinnedNeighborhoodVisible = visible;
      }

      private void addNeighborhood(Category.CategoryNeighborhood neighborhood) {
         visibleNeighborhoods.add(neighborhood);
      }

      private void removeNeighborhood(Category.CategoryNeighborhood neighborhood) {
         visibleNeighborhoods.remove(neighborhood);
      }

      private Set<Category.CategoryNeighborhood> getVisibleNeighborhoods() {
         return visibleNeighborhoods;
      }

      private boolean isPlottable() {
         return plottable;
      }

      private void setPlottable(boolean plottable) {
         this.plottable = plottable;
         getSelectedCategoriesPreferences().putBoolean(getCategory().getName(), plottable);
      }
   }

   private final class CategoryPopupDialog extends JDialog {
      private final class CategoryPopupCheckbox extends JCheckBox {
         private final Category.CategoryNeighborhood categoryNeighborhood;

         private CategoryPopupCheckbox(Category.CategoryNeighborhood categoryNeighborhood) {
            super(categoryNeighborhood.getDirectory().getFileName().toString(), categoryConfiguration.getVisibleNeighborhoods().contains(categoryNeighborhood));

            this.categoryNeighborhood = categoryNeighborhood;

            setBackground(Color.WHITE);

            addActionListener(_ -> {
               if (categoryConfiguration.getVisibleNeighborhoods().contains(categoryNeighborhood)) {
                  categoryConfiguration.removeNeighborhood(categoryNeighborhood);
               } else {
                  categoryConfiguration.addNeighborhood(categoryNeighborhood);
               }
               redraw(true);
            });
            addMouseListener(new PopupMenuMouseListener(_ -> makeDeletePopupMenu()));
         }

         private JPopupMenu makeDeletePopupMenu() {
            JPopupMenu popup = new JPopupMenu();
            setEnabled(!categoryNeighborhood.isDeleted());
            if (categoryNeighborhood.isDeleted()) {
               JMenuItem undelete = popup.add("undelete");
               undelete.addActionListener(_ -> {
                  categoryNeighborhood.setDeleted(false);
                  redraw(true);
                  makeDeletePopupMenu();
               });
            } else {
               JMenuItem delete = popup.add("delete");
               delete.addActionListener(_ -> {
                  categoryNeighborhood.setDeleted(true);
                  redraw(true);
                  makeDeletePopupMenu();
               });
            }
            return popup;
         }
      }

      private final Category category;
      private final CategoryConfiguration categoryConfiguration;

      private CategoryPopupDialog(Category category, CategoryConfiguration categoryConfiguration) {
         super(mainDialog);

         this.category = category;
         this.categoryConfiguration = categoryConfiguration;
      }

      private void refresh() {
         JPanel box = new VerticalScrollablePanel(new BorderLayout());
         box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
         box.setBackground(Color.WHITE);
         addThinnedNeighborhood(box);
         box.add(new JSeparator());
         addCategoryNeighborhoods(box);

         JPanel panel = new JPanel(new BorderLayout());
         panel.add(box); // Wrapping in panel with BorderLayout to get the box to expand horizontally when dialog is resized

         getContentPane().removeAll();
         getContentPane().add(new JScrollPane(panel));
         setTitle(category.getName());
         getContentPane().setBackground(Color.WHITE);
         pack();
         GuiUtils.clampToScreen(this);
      }

      private void addThinnedNeighborhood(JComponent component) {
         JCheckBox cb = new JCheckBox("Thinned pointset", categoryConfiguration.isThinnedNeighborhoodVisible());
         cb.setBackground(Color.WHITE);
         cb.addActionListener(_ -> {
            categoryConfiguration.setThinnedNeighborhoodVisible(!categoryConfiguration.isThinnedNeighborhoodVisible());
            redraw(true);
         });
         component.add(cb);
      }

      private void addCategoryNeighborhoods(JComponent component) {
         category.getCategoryNeighborhoods().forEach(categoryNeighborhood -> {
            component.add(new CategoryPopupCheckbox(categoryNeighborhood));
         });
      }
   }

   private JCheckBox createCategoryCheckBox(Category category, Map<Category, JCheckBox> categoryToCheckBox) {
      CategoryConfiguration categoryConfiguration = categoryConfigMap.get(category);
      JCheckBox checkBox = new JCheckBox(showCategoryNames ? category.getName() : category.getLegend(), categoryConfiguration.isPlottable());
      HtmlStringBuilder tooltip = new HtmlStringBuilder()
            .text(category.getName());
      String comment = category.getComment();
      if (comment != null) {
         tooltip.html("<br><br>").multilineText(comment);
      }
      checkBox.setToolTipText(tooltip.build());
      checkBox.setForeground(category.getColor());
      checkBox.setBackground(Color.WHITE);
      checkBox.addActionListener(_ -> {
         categoryConfiguration.setPlottable(checkBox.isSelected());
         redraw(true);
      });
      checkBox.addMouseListener(new PopupMenuMouseListener(e -> {
         JPopupMenu menu = new JPopupMenu();

         BiFunction<Map<Category, CategoryConfiguration>, Boolean, ActionListener> setSelectedActionListener = (map, selected) -> {
            return _ -> {
               for (Category c : map.keySet()) {
                  categoryToCheckBox.get(c).setSelected(selected);
                  categoryConfigMap.get(c).setPlottable(selected);
               }
               redraw(true);
            };
         };

         JMenuItem allOffItem = menu.add("All off");
         allOffItem.addActionListener(setSelectedActionListener.apply(categoryConfigMap, false));

         JMenuItem allOnItem = menu.add("All on");
         allOnItem.addActionListener(setSelectedActionListener.apply(categoryConfigMap, true));

         menu.addSeparator();

         JMenuItem allBeforeOffItem = menu.add("All before off");
         allBeforeOffItem.addActionListener(setSelectedActionListener.apply(categoryConfigMap.headMap(category), false));

         JMenuItem allBeforeOnItem = menu.add("All before on");
         allBeforeOnItem.addActionListener(setSelectedActionListener.apply(categoryConfigMap.headMap(category), true));

         menu.addSeparator();

         JMenuItem allAfterOffItem = menu.add("All after off");
         allAfterOffItem.addActionListener(setSelectedActionListener.apply(categoryConfigMap.tailMap(category), false));

         JMenuItem allAfterOnItem = menu.add("All after on");
         allAfterOnItem.addActionListener(setSelectedActionListener.apply(categoryConfigMap.tailMap(category), true));

         menu.addSeparator();

         JMenuItem showTrainingDataItem = menu.add("Show training data");
         showTrainingDataItem.addActionListener(_ -> {
            JDialog popup = categoryConfiguration.getPopup();
            if (popup.isVisible()) {
               popup.setVisible(true);
               return;
            }
            popup.setLocation(e.getX(), e.getY());
            popup.setVisible(true);
         });

         return menu;
      }));
      return checkBox;
   }

   private @Nullable Category showCategorySelectionDialog() {
      if (extractionCategory == null) {
         return null;
      }
      List<Category> categories = new ArrayList<>(configurator.getNonSpecialEnabledCategories());
      Map<Category, Float> overlaps = new HashMap<>();
      for (Category category : categories) {
         float overlap = GaussUtils.computeOverlap(category.getPixelCategoryDistribution().getGaussDistribution(), extractionCategory.getPixelCategoryDistribution().getNeighborhood(),
               configurator.outlierFraction.getFloatValue(), toFeatureNames(configurator.getEnabledFeatureExtractors()));
         overlaps.put(category, overlap);
      }
      categories.sort(Comparator.<Category>comparingDouble(overlaps::get).reversed().thenComparing(Category::getName));

      JComboBox<Category> comboBox = new JComboBox<>(new ComboBoxListModel<>(null, categories));
      comboBox.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
      comboBox.setRenderer(new DefaultListCellRenderer() {
         @Override
         public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Category category = (Category) value;
            super.getListCellRendererComponent(list, category.getName(), index, isSelected, cellHasFocus);
            setIcon(categoryIcon(category));
            return this;
         }
      });
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(new JLabel("Select category:"), BorderLayout.NORTH);
      panel.add(comboBox);

      int answer = JOptionPane.showConfirmDialog(mainPanel, panel, "Add to category", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
      return answer == JOptionPane.OK_OPTION ? (Category) comboBox.getSelectedItem() : null;
   }

   private static ColorIcon categoryIcon(Category category) {
      return new ColorIcon(category.getColor(), 10, 10);
   }

   private final class MyChartPanel extends PlotChartPanel {
      private @Nullable Rectangle2D selectionRectangle;
      private @Nullable Point2D referencePoint;

      private MyChartPanel() {
         super(null);
      }

      @Override
      public void mousePressed(MouseEvent e) {
         if (!SwingUtilities.isLeftMouseButton(e)) {
            super.mousePressed(e);
            return;
         }
         if (selectedPlotType != PlotType.SCATTER) {
            return;
         }
         referencePoint = e.getPoint();
      }

      @Override
      public void mouseDragged(MouseEvent e) {
         if (referencePoint == null) {
            super.mouseDragged(e);
            return;
         }
         Graphics2D g2 = (Graphics2D) getGraphics();

         // use XOR to erase the previous rectangle (if any)...
         g2.setXORMode(Color.GRAY);
         if (selectionRectangle != null) {
            g2.draw(selectionRectangle);
         } else {
            selectionRectangle = new Rectangle2D.Double();
         }
         Rectangle2D screenDataArea = getScreenDataArea();
         double x = Math.clamp(e.getX(), screenDataArea.getMinX(), screenDataArea.getMaxX());
         double y = Math.clamp(e.getY(), screenDataArea.getMinY(), screenDataArea.getMaxY());
         selectionRectangle.setFrameFromDiagonal(referencePoint.getX(), referencePoint.getY(), x, y);

         // use XOR to draw the new rectangle...
         g2.draw(selectionRectangle);

         g2.dispose();
      }

      @Override
      public void mouseReleased(MouseEvent e) {
         if (referencePoint == null) {
            super.mouseReleased(e);
            return;
         }
         if (selectionRectangle != null) {
            Rectangle2D selection = PlotUtils.screenToData(this, selectionRectangle);
            markSelectedPoints(selection, e);
            selectionRectangle = null;
         } else {
            // create a rectangle around the starting point
            Rectangle2D rect = new Rectangle2D.Double(referencePoint.getX() - 2, referencePoint.getY() - 2, 5, 5);
            Rectangle2D selection = PlotUtils.screenToData(this, rect);
            markSingleGriddedPoint(selection, e);
         }
         referencePoint = null;
         repaint();
      }
   }

   private @Nullable Category extractionCategory;

   private final NavigableMap<Category, CategoryConfiguration> categoryConfigMap = new TreeMap<>((o1, o2) -> {
      if (o1 == o2) {
         return 0;
      }
      if (o1 == extractionCategory) {
         return -1;
      }
      if (o2 == extractionCategory) {
         return 1;
      }
      return Byte.compare(o1.getNumber(), o2.getNumber());
   }) {
      @Override
      public CategoryConfiguration remove(Object key) {
         CategoryConfiguration categoryConfiguration = super.remove(key);
         categoryConfiguration.closePopup();
         return categoryConfiguration;
      }
   };

   private static final Color EXTRACTION_COLOR = Color.BLACK;
   private static final Color MARKING_COLOR = Color.RED;

   private final @Nullable EchogramWindow echogramWindow;
   private final Configurator configurator;

   private final JDialog mainDialog;
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final ChartPanel chartPanel = new MyChartPanel();
   //private float quantile;
   private String xAxisFeature = "";
   private String yAxisFeature = "";
   private boolean showCategoryNames = getPreferences().getBoolean("showCategoryNames", true);
   private boolean drawConfidenceIntervals;
   private FrequencyMapping frequencyMapping = FrequencyMapping.SQRT;
   private FrequencyResponseAxis frequencyResponseAxis = FrequencyResponseAxis.SQRT;

   private enum PlotType {
      ECHOGRAM("Echogram"),
      SCATTER("Scatter"),
      FREQUENCY_RESPONSE("Frequency response"),
      PROBABILITIES("Probabilities");

      private final String label;

      PlotType(String label) {
         this.label = label;
      }

      @Override
      public String toString() {
         return label;
      }
   }

   private PlotType selectedPlotType = PlotType.SCATTER;

   private static final String EXTRACTED = "Extracted points";

   private final @Nullable EchogramVisualizer echogramVisualizer;
   private final CollectiveFeatureComputation collectiveFeatureComputation;
   private boolean didSave;

   /**
    * Creates a CategoryVisualizer without an EchogramWindow.
    *
    * @param configurator       a configurator
    * @param referenceComponent the owner for the dialog created
    * @param modalityType       modality type
    */
   public CategoryVisualizer(Configurator configurator, @Nullable Component referenceComponent, Dialog.ModalityType modalityType) {
      this.configurator = configurator;
      echogramVisualizer = null;
      echogramWindow = null;
      collectiveFeatureComputation = new CollectiveFeatureComputation(); //empty collective computation
      mainDialog = makeDialog(referenceComponent, modalityType);
      init();
   }

   public CategoryVisualizer(EchogramWindow echogramWindow, ConfigFileSettings configFileSettings, @Nullable Component referenceComponent, Dialog.ModalityType modalityType) {
      this.echogramWindow = echogramWindow;
      configurator = echogramWindow.getConfigurator();
      echogramVisualizer = new EchogramVisualizer(echogramWindow, configFileSettings);
      echogramVisualizer.getChangeManager().addListener(() -> {
         redefineExtraction();
         redraw(true);
      });
      selectedPlotType = PlotType.ECHOGRAM;
      collectiveFeatureComputation = new CollectiveFeatureComputation(echogramWindow);
      mainDialog = makeDialog(referenceComponent, modalityType);
      init();
   }

   private JDialog makeDialog(@Nullable Component referenceComponent, Dialog.ModalityType modalityType) {
      StringBuilder title = new StringBuilder("Category visualizer");
      if (echogramWindow != null) {
         NumberFormat nf = Utils.createDecimalFormat("#.#");
         title.append(": Depth: " + nf.format(echogramWindow.getMinDepth())
               + " - " + nf.format(echogramWindow.getMaxDepth()) + " m," +
               " Ping: " + echogramWindow.getPingOffset()
               + " - " + (echogramWindow.getPingOffset() + echogramWindow.getPings().size() - 1));
      }

      return new JDialog(GuiUtils.windowForComponent(referenceComponent), title.toString(), modalityType);
   }

   private void init() {
      mainDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      mainDialog.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosed(WindowEvent e) {
            closePopups();
         }
      });
      mainDialog.setSize(PREFERRED_SIZE);
      mainDialog.getContentPane().add(mainPanel);

      initConfig();
      initAxes();
      redraw(false);
      mainDialog.setLocationRelativeTo(mainDialog.getParent());
      mainDialog.setVisible(true);

      collectiveFeatureComputation.getChangeManager().addListener(this::redefineExtraction);
   }

   private static Preferences getPreferences() {
      return KoronaPreferences.node("categoryVisualizer");
   }

   public boolean getDidSave() {
      return didSave;
   }

   private void refreshPopups() {
      for (CategoryConfiguration ccf : categoryConfigMap.values()) {
         ccf.refreshPopup();
      }
   }

   private void closePopups() {
      for (CategoryConfiguration ccf : categoryConfigMap.values()) {
         ccf.closePopup();
      }
   }

   private void markSingleGriddedPoint(Rectangle2D selectionRectangle, MouseEvent e) {
      boolean setMarked = true;
      boolean clearMarking = false;
      if ((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
         setMarked = false;
      } else if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == 0) {
         clearMarking = true;
      }
      collectiveFeatureComputation.markSelectedGridCell(selectionRectangle, xAxisFeature, yAxisFeature, clearMarking, setMarked);

      redraw(true);
   }

   private void markSelectedPoints(Rectangle2D selectedRegion, MouseEvent e) {
      if (selectedRegion.getWidth() == 0 || selectedRegion.getHeight() == 0 || echogramWindow == null || extractionCategory == null) {
         return;
      }

      boolean setMarked = true;
      if ((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
         setMarked = false;
      } else if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == 0) {
         echogramWindow.clearMarking();
      }

      Category.CategoryNeighborhood categoryNeighborhood = extractionCategory.getCategoryNeighborhoods().iterator().next();
      Neighborhood neighborhood = categoryNeighborhood.getPixelNeighborhoodData().getNeighborhood();
      for (Neighbor neighbor : neighborhood.getNeighbors()) {
         EchogramWindow.IndexedNeighbor indexedNeighbor = (EchogramWindow.IndexedNeighbor) neighbor;
         Feature xFeature = neighbor.getFeature(xAxisFeature);
         Feature yFeature = neighbor.getFeature(yAxisFeature);
         if (xFeature != null && yFeature != null && selectedRegion.contains(xFeature.value(), yFeature.value())) {
            echogramWindow.mark(indexedNeighbor.getI(), indexedNeighbor.getJ(), setMarked);
         }
      }

      redraw(true);
   }

   private void redraw(boolean keepAxes) {
      if (selectedPlotType != PlotType.ECHOGRAM) {
         Plotter plotter = makePlot();
         if (keepAxes && chartPanel.getChart() != null) {
            plotter.xRange(chartPanel.getChart().getXYPlot().getDomainAxis().getRange());
            plotter.yRange(chartPanel.getChart().getXYPlot().getRangeAxis().getRange());
         }
         chartPanel.setChart(plotter.createChart());
      }

      JPanel plotPanel = new JPanel(new BorderLayout());

      switch (selectedPlotType) {
         case ECHOGRAM -> {
            if (echogramVisualizer == null) {
               break;
            }
            plotPanel.add(echogramVisualizer.redraw());
         }
         case SCATTER -> {
            plotPanel.add(chartPanel);
            plotPanel.add(makeXAxisButtons(), BorderLayout.SOUTH);
            plotPanel.add(makeYAxisButtons(), BorderLayout.WEST);
            plotPanel.add(makeDistributionLevelButtons(), BorderLayout.EAST);
         }
         case FREQUENCY_RESPONSE -> {
            plotPanel.add(chartPanel);
            plotPanel.add(makeFreqRespPlotControls(), BorderLayout.SOUTH);
            plotPanel.add(makeDistributionLevelButtons(), BorderLayout.EAST);
         }
         case PROBABILITIES -> {
            plotPanel.add(chartPanel);
            plotPanel.add(makeXAxisButtons(), BorderLayout.SOUTH);
            plotPanel.add(makeDistributionLevelButtons(), BorderLayout.EAST);
         }
      }

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(plotPanel);
      panel.add(makeMenus(), BorderLayout.NORTH);

      GuiUtils.replaceContent(mainPanel, panel);

      if (misClassMatrixView != null) {
         misClassMatrixView.redraw();
      }

      //todo hack to make F1 and del buttons to work correctly
      SwingUtilities.invokeLater(mainPanel::requestFocus);
   }

   private JPanel makeFreqRespPlotControls() {
      JPanel panel = new JPanel(new FlowLayout());
      panel.setBackground(Color.WHITE);
      panel.add(Box.createHorizontalGlue());

      JComboBox<FrequencyMapping> xAxisComboBox = new JComboBox<>(FrequencyMapping.values());
      xAxisComboBox.setSelectedItem(frequencyMapping);
      xAxisComboBox.addActionListener(_ -> {
         frequencyMapping = (FrequencyMapping) Objects.requireNonNull(xAxisComboBox.getSelectedItem());
         redraw(false);
      });
      panel.add(new JLabel("x-axis: "));
      panel.add(xAxisComboBox);
      panel.add(Box.createHorizontalStrut(10));

      JComboBox<FrequencyResponseAxis> yAxisComboBox = new JComboBox<>(FrequencyResponseAxis.values());
      yAxisComboBox.setSelectedItem(frequencyResponseAxis);
      yAxisComboBox.addActionListener(_ -> {
         frequencyResponseAxis = (FrequencyResponseAxis) Objects.requireNonNull(yAxisComboBox.getSelectedItem());
         redraw(false);
      });
      panel.add(new JLabel("y-axis: "));
      panel.add(yAxisComboBox);
      panel.add(Box.createHorizontalStrut(10));

      JCheckBox drawConfidenceIntervalsCheckBox = new JCheckBox("Draw confidence intervals", drawConfidenceIntervals);
      drawConfidenceIntervalsCheckBox.setBackground(Color.WHITE);
      drawConfidenceIntervalsCheckBox.setSelected(drawConfidenceIntervals);
      drawConfidenceIntervalsCheckBox.addActionListener(_ -> {
         drawConfidenceIntervals = drawConfidenceIntervalsCheckBox.isSelected();
         redraw(false);
      });
      panel.add(drawConfidenceIntervalsCheckBox);

      panel.add(Box.createHorizontalGlue());
      return panel;
   }

   private JComponent makeDistributionLevelButtons() {
      Box box = Box.createVerticalBox();
      box.setBackground(Color.WHITE);
      box.add(Box.createVerticalGlue());
      JRadioButton pixelButton = createDistributionLevelButton("Pixel", Category.DistributionLevel.PIXEL);
      box.add(pixelButton);
      JRadioButton cellButton = createDistributionLevelButton("Cell", Category.DistributionLevel.CELL);
      box.add(cellButton);
      JRadioButton schoolButton = createDistributionLevelButton("School", Category.DistributionLevel.SCHOOL);
      box.add(schoolButton);
      GuiUtils.createButtonGroup(pixelButton, cellButton, schoolButton);
      box.add(Box.createVerticalGlue());
      JPanel panel = new JPanel(new BorderLayout());
      panel.setBackground(Color.WHITE);
      panel.add(box);
      return panel;
   }

   private JRadioButton createDistributionLevelButton(String text, Category.DistributionLevel level) {
      JRadioButton button = new JRadioButton(text);
      button.setSelected(distributionLevel == level);
      button.setBackground(Color.WHITE);
      button.addActionListener(_ -> {
         distributionLevel = level;
         redraw(false);
      });
      return button;
   }

   private JComponent makeXAxisButtons() {
      JPanel panel = new JPanel(new FlowLayout());
      panel.setBackground(Color.WHITE);

      for (FeatureExtractor featureExtractor : configurator.getEnabledFeatureExtractors()) {
         String featureName = featureExtractor.getFeatureName();
         JRadioButton button = new JRadioButton(featureName);
         button.setBackground(Color.WHITE);
         button.setSelected(xAxisFeature.equals(featureName));
         button.addActionListener(_ -> {
            xAxisFeature = featureName;
            redraw(false);
         });
         panel.add(button);
      }

      return panel;
   }

   private JComponent makeYAxisButtons() {
      Box box = Box.createVerticalBox();
      box.setBackground(Color.WHITE);
      box.add(Box.createVerticalGlue());
      for (FeatureExtractor featureExtractor : configurator.getEnabledFeatureExtractors()) {
         String featureName = featureExtractor.getFeatureName();
         JRadioButton button = new JRadioButton(featureName);
         button.setBackground(Color.WHITE);
         button.setSelected(yAxisFeature.equals(featureName));
         button.addActionListener(_ -> {
            yAxisFeature = featureName;
            redraw(false);
         });
         box.add(button);
      }
      box.add(Box.createVerticalGlue());
      JPanel panel = new JPanel(new BorderLayout());
      panel.setBackground(Color.WHITE);
      panel.add(box);
      return panel;
   }

   private Plotter makePlot() {
      return switch (selectedPlotType) {
         case ECHOGRAM -> {
            throw new IllegalStateException();
         }
         case SCATTER -> {
            yield new Plotter(makeScatter())
                  .xAxis(xAxisFeature)
                  .yAxis(yAxisFeature);
         }
         case FREQUENCY_RESPONSE -> {
            List<Graph> graphs = makeFreqResp(FloatRange.ALL);

            FloatRangeBuilder yRangeBuilder = new FloatRangeBuilder();
            NavigableSet<Integer> kHzSet = new TreeSet<>();
            for (Graph graph : graphs) {
               for (Vec2 point : graph.getPoints()) {
                  kHzSet.add(Math.round(point.x()));
                  yRangeBuilder.expand(point.y());
               }
            }
            FloatRange yRange = yRangeBuilder.toFloatRange();

            FloatRange kHzRange = kHzSet.isEmpty() ? FloatRange.EMPTY_RANGE : FloatRange.of(kHzSet.first(), kHzSet.last());

            yield new Plotter(graphs)
                  .xAxis(() -> {
                     return frequencyMapping.axis(kHzRange, kHzSet);
                  })
                  .yAxis(() -> {
                     return frequencyResponseAxis.axis(yRange);
                  });
         }
         case PROBABILITIES -> {
            yield new Plotter(makeProbabilitiesPlot())
                  .xAxis(xAxisFeature)
                  .yAxis("probability");
         }
      };
   }

   private JComponent fileMenu() {
      JMenu menu = new JMenu("File");
      menu.setMnemonic(KeyEvent.VK_F);

      JMenuItem saveItem = MiscIcons.SAVE.on(menu.add("Save"));
      saveItem.addActionListener(_ -> {
         configurator.save();
         didSave = true;
      });

      menu.addSeparator();

      JMenuItem closeItem = menu.add("Close");
      closeItem.addActionListener(_ -> mainDialog.dispose());

      return menu;
   }

   public static List<String> toFeatureNames(Collection<FeatureExtractor> featureExtractors) {
      List<String> featureNames = new ArrayList<>();
      for (FeatureExtractor featureExtractor : featureExtractors) {
         featureNames.add(featureExtractor.getFeatureName());
      }
      return featureNames;
   }

   Map<Category, CategoryConfiguration> getCategoryConfigMap() {
      return categoryConfigMap;
   }

   private Collection<Category> getPlottableCategories() {
      Collection<Category> plottableCategories = new ArrayList<>(categoryConfigMap.size());
      for (CategoryConfiguration categoryConfiguration : categoryConfigMap.values()) {
         if (categoryConfiguration.isPlottable()) {
            plottableCategories.add(categoryConfiguration.getCategory());
         }
      }
      return plottableCategories;
   }

   private void updateCategoryConfiguration() {
      Collection<Category> configCategories = configurator.getNonSpecialEnabledCategories();
      configCategories.forEach(category -> {
         categoryConfigMap.computeIfAbsent(category, CategoryConfiguration::new);
      });
      categoryConfigMap.keySet().removeIf(category -> {
         return category != extractionCategory && !configCategories.contains(category);
      });
   }

   private void outlierFractionUpdated() {
      //quantile = GaussUtils.quantileValue(configurator.outlierFraction.getFloatValue(), 1);
      redraw(true);
   }

   private void thinnedScatterSizeChanged() {
      if (extractionCategory == null) {
         return;
      }
      int n = extractionCategory.getCategoryNeighborhoods().size();
      for (Category category : configurator.getAllCategories()) {
         n += category.getCategoryNeighborhoods().size() * category.getCategoryDistributions().size() * GaussDistribution.MAX_EM_ITERATIONS;
      }

      ProgressView progressView = new ProgressView("Recomputing thinned scatter", configurator.getAllCategories().size())
            .useSecondaryProgress();
      new WorkerDialog(mainDialog, progressView.getComponent())
            .start(asyncHandle -> {
               progressView.incrementMainProgress(extractionCategory.getName());
               extractionCategory.recompute(asyncHandle, progressView.getSecondaryProgressHandler());
               for (Category category : configurator.getAllCategories()) {
                  if (asyncHandle.isCancelled()) {
                     break;
                  }
                  progressView.incrementMainProgress(category.getName());
                  category.recompute(asyncHandle, progressView.getSecondaryProgressHandler());
               }
               SwingUtilities.invokeLater(() -> redraw(true));
            });
   }

   private void showParameterDialog() {
      List<BaseParameter<?>> parameters = List.of(
            configurator.outlierFraction,
            configurator.thinnedScatterSize,
            configurator.deltaMinMaxSv38
      );
      ParameterEditor parameterEditor = new ParameterEditor(parameters);

      List<Subscription> subscriptions = List.of(
            configurator.outlierFraction.subscribe(Listener.of(this::outlierFractionUpdated)),
            configurator.thinnedScatterSize.subscribe(Listener.of(this::thinnedScatterSizeChanged))
      );

      new ConfigurableGUIDialog(mainDialog, "Categorization parameters", new ParameterCollection(parameters))
            .setHelpID(KoronaHelp.CATEGORIZATION_LIBRARY)
            .setCloseOnOk(parameterEditor::commitEdits)
            .setGUI(parameterEditor.getEditorComponent())
            .show();

      subscriptions.forEach(Subscription::unsubscribe);
   }

   private void possiblyChangeAxis() {
      if (isFeatureExtractorEnabled(xAxisFeature) && isFeatureExtractorEnabled(yAxisFeature)) {
         redraw(true);
      } else {
         initAxes();
         redraw(false);
      }
   }

   private boolean isFeatureExtractorEnabled(String featureName) {
      FeatureExtractor featureExtractor = configurator.getFeatureExtractor(featureName);
      return featureExtractor != null && featureExtractor.isEnabled();
   }

   private JComponent editMenu() {
      JMenu menu = new JMenu("Edit");
      menu.setMnemonic(KeyEvent.VK_E);
      menu.setToolTipText("Modify the set of categories");

      JMenuItem categorizationConfigurationItem = MiscIcons.SETTINGS.on(menu.add("Categorization configuration..."));
      categorizationConfigurationItem.addActionListener(_ -> {
         ConfiguratorEditor.editCategorizationConfiguration(mainPanel, configurator);
         updateCategoryConfiguration();
         refreshPopups();
         possiblyChangeAxis();
      });

      JMenuItem categorizationParametersItem = menu.add("Categorization parameters...");
      categorizationParametersItem.addActionListener(_ -> showParameterDialog());

      JMenuItem griddingConfigurationItem = menu.add("Gridding configuration...");
      griddingConfigurationItem.addActionListener(_ -> {
         CollectiveFeatureComputation.editGriddingSettings(mainPanel);
         possiblyChangeAxis();
      });

      menu.addSeparator();

      JMenuItem addItem = MiscIcons.ADD.on(menu.add("Add to category..."));
      addItem.setEnabled(extractionCategory != null
            && !configurator.getNonSpecialEnabledCategories().isEmpty());
      addItem.addActionListener(_ -> {
         Category category = showCategorySelectionDialog();
         if (category != null) {
            addExtractionToCategory(category);
            redraw(true);
         }
      });

      menu.addSeparator();

      JMenu markMenu = makeMarkingMenu(true);
      markMenu.setEnabled(extractionCategory != null);
      menu.add(markMenu);

      JMenu unmarkMenu = makeMarkingMenu(false);
      unmarkMenu.setEnabled(extractionCategory != null);
      menu.add(unmarkMenu);

      JMenuItem clearMarkingItem = menu.add("Clear marking");
      clearMarkingItem.setEnabled(extractionCategory != null);
      clearMarkingItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      clearMarkingItem.addActionListener(_ -> {
         assert echogramWindow != null;
         echogramWindow.clearMarking();
         redraw(true);
      });

      JMenuItem invertMarkingItem = menu.add("Invert marking");
      invertMarkingItem.setEnabled(extractionCategory != null);
      invertMarkingItem.addActionListener(_ -> {
         assert echogramWindow != null;
         echogramWindow.invertMarking();
         redraw(true);
      });

      menu.addSeparator();

      JMenuItem deleteMarkedPointsItem = MiscIcons.DELETE.on(menu.add("Delete marked points"));
      deleteMarkedPointsItem.setEnabled(extractionCategory != null);
      deleteMarkedPointsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
      deleteMarkedPointsItem.addActionListener(_ -> deleteMarkedPoints());

      JMenuItem deleteUnmarkedPointsItem = menu.add("Delete unmarked points");
      deleteUnmarkedPointsItem.setEnabled(extractionCategory != null);
      deleteUnmarkedPointsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.ALT_DOWN_MASK));
      deleteUnmarkedPointsItem.addActionListener(_ -> deleteUnmarkedPoints());

      menu.addSeparator();

      JMenuItem undoItem = MiscIcons.UNDO.on(menu.add("Undo all modifications"));
      undoItem.addActionListener(_ -> {
         configurator.init();
         if (echogramWindow != null) {
            echogramWindow.clearMarking();
            echogramWindow.clearInteractiveMask();
         }
         initConfig();
         redraw(true);
      });

      return menu;
   }

   private JMenu makeMarkingMenu(boolean marking) {
      JMenu menu = new JMenu(marking ? "Mark" : "Unmark");

      if (extractionCategory != null) {
         Category.CategoryNeighborhood categoryNeighborhood = extractionCategory.getCategoryNeighborhoods().iterator().next();

         menu.add(makeMarkOutliersMenu(false, marking, categoryNeighborhood));
         menu.add(makeMarkOutliersMenu(true, marking, categoryNeighborhood));

         menu.addSeparator();

         menu.add(makeMarkClassificationsMenu(false, marking, categoryNeighborhood));
         menu.add(makeMarkClassificationsMenu(true, marking, categoryNeighborhood));

         Collection<Float> regionThresholds = getRegionThresholds();
         if (!regionThresholds.isEmpty()) {
            menu.addSeparator();
            menu.add(makeMarkRegionMenu(regionThresholds, false, marking));
            menu.add(makeMarkRegionMenu(regionThresholds, true, marking));
         }

         Set<Integer> trackIds = getTrackIds();
         if (!trackIds.isEmpty()) {
            menu.addSeparator();

            JMenuItem insideItem = menu.add("Inside track");
            insideItem.addActionListener(_ -> markTrack(trackIds, false, marking));

            JMenuItem outsideItem = menu.add("Outside track");
            outsideItem.addActionListener(_ -> markTrack(trackIds, true, marking));
         }
      }

      return menu;
   }

   private JMenu makeMarkClassificationsMenu(boolean invert, boolean marking, Category.CategoryNeighborhood categoryNeighborhood) {
      JMenu menu = MiscIcons.EMPTY.on(new JMenu(invert ? "Not classified as" : "Classified as"));

      Neighborhood extractionNeighborhood = categoryNeighborhood.getPixelNeighborhoodData().getNeighborhood();

      Collection<Category> categories = new ArrayList<>(categoryConfigMap.keySet());
      Collection<@Nullable Category> categoriesAndNull = new ArrayList<>(categoryConfigMap.keySet());
      categoriesAndNull.add(null);
      for (Category category : categoriesAndNull) {
         JMenuItem menuItem = menu.add(category != null ? category.getName() : Configurator.UNKNOWN_CATEGORY_NAME);
         if (category != null) {
            menuItem.setIcon(categoryIcon(category));
         }
         menuItem.addActionListener(_ -> markClassifications(extractionNeighborhood, categories, category, invert, marking));
      }
      return menu;
   }

   private JMenu makeMarkOutliersMenu(boolean invert, boolean marking, Category.CategoryNeighborhood categoryNeighborhood) {
      JMenu menu = MiscIcons.EMPTY.on(new JMenu(invert ? "Inliers" : "Outliers"));

      Neighborhood extractionNeighborhood = categoryNeighborhood.getPixelNeighborhoodData().getNeighborhood();

      for (Category category : categoryConfigMap.keySet()) {
         JMenuItem menuItem = menu.add(category.getName());
         menuItem.setIcon(categoryIcon(category));
         menuItem.addActionListener(_ -> {
            GaussDistribution distribution = category.getPixelCategoryDistribution().getGaussDistribution();
            markOutliers(extractionNeighborhood, distribution, invert, marking);
         });
      }
      return menu;
   }

   private JMenu makeMarkRegionMenu(Collection<Float> regionThresholds, boolean invert, boolean marking) {
      JMenu menu = new JMenu(invert ? "Outside region" : "Inside region");

      for (float threshold : regionThresholds) {
         JMenuItem menuItem = menu.add("Threshold: " + threshold);
         menuItem.addActionListener(_ -> markRegion(threshold, invert, marking));
      }
      return menu;
   }

   private void markClassifications(Neighborhood neighborhood, Collection<Category> categories,
                                    @Nullable Category target, boolean invert, boolean marking) {
      assert echogramWindow != null;
      float outlierFraction = configurator.outlierFraction.getFloatValue();
      Map<@Nullable Category, Collection<Neighbor>> classification = neighborhood.getClassificationMap(outlierFraction, configurator.getEnabledFeatureExtractors(), categories);
      Collection<Neighbor> neighbors = classification.get(target);
      EchogramWindow.Predicate predicate = new EchogramWindow.Predicate(echogramWindow);
      predicate.setTrue(neighbors);
      applyPredicate(predicate, invert, marking);
   }

   private void markOutliers(Neighborhood neighborhood, GaussDistribution distribution, boolean invert, boolean marking) {
      assert echogramWindow != null;
      float outlierFraction = configurator.outlierFraction.getFloatValue();
      Collection<Neighbor> outliers = neighborhood.getOutliers(outlierFraction, configurator.getEnabledFeatureExtractors(), distribution);
      EchogramWindow.Predicate predicate = new EchogramWindow.Predicate(echogramWindow);
      predicate.setTrue(outliers);
      applyPredicate(predicate, invert, marking);
   }

   private void markRegion(float threshold, boolean invert, boolean marking) {
      assert echogramWindow != null;
      List<Ping> pings = echogramWindow.getPings();

      Set<Integer> ids = getRegionIds();

      EchogramWindow.Predicate predicate = new EchogramWindow.Predicate(echogramWindow);
      for (int i = 0; i < pings.size(); i++) {
         Ping ping = pings.get(i);
         for (RegionBorderDatagram regionBorder : ping.getPingItems(RegionBorderDatagram.class).toList()) {
            if (regionBorder.getThreshold() != threshold) {
               continue;
            }

            for (RegionBorderDatagram.BorderInfo borderInfo : regionBorder.getBorderInfos()) {
               if (!ids.contains(borderInfo.id())) {
                  continue;
               }
               int begin = Math.max(0, echogramWindow.depthToIndex(borderInfo.startDepth()));
               int end = Math.min(echogramWindow.getHeight(), echogramWindow.depthToIndex(borderInfo.endDepth()) + 1);
               for (int j = begin; j < end; j++) {
                  predicate.setTrue(i, j);
               }
            }
         }
      }
      applyPredicate(predicate, invert, marking);
   }

   private void markTrack(Set<Integer> trackIds, boolean invert, boolean marking) {
      assert echogramWindow != null;
      List<Ping> pings = echogramWindow.getPings();

      EchogramWindow.Predicate predicate = new EchogramWindow.Predicate(echogramWindow);
      for (int i = 0; i < pings.size(); i++) {
         Ping ping = pings.get(i);
         for (TBR0Datagram tbr0Datagram : ping.getPingItems(TBR0Datagram.class).toList()) {
            if (!trackIds.contains(tbr0Datagram.getId())) {
               continue;
            }
            int begin = Math.max(0, echogramWindow.depthToIndex(tbr0Datagram.getDepthRange().min()));
            int end = Math.min(echogramWindow.getHeight(), echogramWindow.depthToIndex(tbr0Datagram.getDepthRange().max()) + 1);
            for (int j = begin; j < end; j++) {
               predicate.setTrue(i, j);
            }
         }
      }
      applyPredicate(predicate, invert, marking);
   }

   private Set<Integer> getRegionIds() {
      if (echogramWindow == null) {
         return Set.of();
      }
      return echogramWindow.getPings().stream()
            .flatMap(ping -> ping.getPingItems(RegionInfoDatagram.class))
            .filter(RegionInfoDatagram::isAccepted)
            .flatMap(regionInfoDatagram -> Arrays.stream(regionInfoDatagram.getBorderIds()).boxed())
            .collect(Collectors.toSet());
   }

   private Collection<Float> getRegionThresholds() {
      if (echogramWindow == null) {
         return List.of();
      }
      return echogramWindow.getPings().stream()
            .flatMap(ping -> ping.getPingItems(RegionInfoDatagram.class))
            .filter(RegionInfoDatagram::isAccepted)
            .map(RegionInfoDatagram::getThreshold)
            .distinct()
            .sorted()
            .toList();
   }

   private Set<Integer> getTrackIds() {
      if (echogramWindow == null) {
         return Set.of();
      }
      return echogramWindow.getPings().stream()
            .flatMap(ping -> ping.getPingItems(TNF0Datagram.class))
            .filter(TNF0Datagram::isValid)
            .map(TNF0Datagram::getId)
            .collect(Collectors.toSet());
   }

   private void applyPredicate(EchogramWindow.Predicate predicate, boolean invert, boolean marking) {
      if (invert) {
         predicate.invert();
      }
      predicate.apply(marking);
      redraw(true);
   }

   private void deleteMarkedPoints() {
      assert echogramWindow != null;
      for (int i = 0; i < echogramWindow.getWidth(); i++) {
         for (int j = 0; j < echogramWindow.getHeight(); j++) {
            if (echogramWindow.isMarked(i, j)) {
               echogramWindow.setInteractiveMask(i, j, false);
            }
         }
      }
      echogramWindow.getChangeManager().notifyListeners();
      redefineExtraction();
      redraw(true);
   }

   private void deleteUnmarkedPoints() {
      assert echogramWindow != null;
      for (int i = 0; i < echogramWindow.getWidth(); i++) {
         for (int j = 0; j < echogramWindow.getHeight(); j++) {
            if (!echogramWindow.isMarked(i, j)) {
               echogramWindow.setInteractiveMask(i, j, false);
            }
         }
      }
      echogramWindow.getChangeManager().notifyListeners();
      redefineExtraction();
      redraw(true);
   }

   private void redefineExtraction() {
      if (extractionCategory == null) {
         return;
      }
      assert echogramWindow != null;
      Category.CategoryNeighborhood categoryNeighborhood = extractionCategory.getCategoryNeighborhoods().iterator().next();
      categoryNeighborhood.setNeighborhood(new Category.NeighborhoodInitialization(echogramWindow, collectiveFeatureComputation));
   }

   private void addExtractionToCategory(Category category) {
      categoryConfigMap.remove(extractionCategory);
      extractionCategory = null;

      assert echogramWindow != null;
      category.addNeighborhood(new Category.NeighborhoodInitialization(echogramWindow, collectiveFeatureComputation));

      CategoryConfiguration ccf = categoryConfigMap.get(category);
      ccf.thinnedNeighborhoodVisible = true;
      ccf.setPlottable(true);
      ccf.refreshPopup();
   }

   private JComponent viewMenu() {
      JMenu menu = new JMenu("View");
      menu.setMnemonic(KeyEvent.VK_V);
      menu.setToolTipText("Which type of plot");

      GuiUtils.autoCreateContentMenu(menu, () -> {
         for (int i = 0; i < PlotType.values().length; i++) {
            PlotType plotType = PlotType.values()[i];
            JMenuItem plotItem = MiscIcons.check(selectedPlotType == plotType).on(menu.add(plotType.toString()));
            plotItem.addActionListener(_ -> {
               selectedPlotType = plotType;
               redraw(false);
            });
            if (plotType == PlotType.ECHOGRAM) {
               plotItem.setEnabled(echogramWindow != null);
            }
         }

         menu.addSeparator();

         JMenuItem namesItem = MiscIcons.checkBox(showCategoryNames).on(menu.add("Show full category names"));
         namesItem.addActionListener(_ -> {
            showCategoryNames = !showCategoryNames;
            getPreferences().putBoolean("showCategoryNames", showCategoryNames);
            redraw(true);
         });

         menu.addSeparator();

         JMenuItem misclassificationItem = menu.add("Misclassification matrix...");
         misclassificationItem.addActionListener(_ -> {
            if (misClassMatrixView == null) {
               misClassMatrixView = new MisClassMatrixView(this, configurator, mainPanel);
            }
            misClassMatrixView.showMisClassDialog();
         });

         menu.addSeparator();

         JMenuItem rescaleAxesItem = menu.add("Rescale axes");
         rescaleAxesItem.addActionListener(_ -> redraw(false));
      });

      return menu;
   }

   private static JComponent helpMenu() {
      JMenu menu = new JMenu("Help");
      menu.setMnemonic(KeyEvent.VK_H);
      JMenuItem helpItem = MiscIcons.HELP.on(menu.add("Categorization"));
      KoronaHelp.CATEGORIZATION_LIBRARY.enableHelpKeyMenuItem(helpItem);
      return menu;
   }

   private JPanel categoryToggles() {
      JPanel togglePanel = new JPanel(new WrappingFlowLayout());
      Map<Category, JCheckBox> categoryToCheckBox = new HashMap<>();
      for (Category category : categoryConfigMap.keySet()) {
         if (!category.getPixelCategoryDistribution().getNeighborhood().getNeighbors().isEmpty()) {
            JCheckBox checkBox = createCategoryCheckBox(category, categoryToCheckBox);
            categoryToCheckBox.put(category, checkBox);
            togglePanel.add(checkBox);
         }
      }
      togglePanel.setBackground(Color.WHITE);
      return togglePanel;
   }

   private JComponent makeMenus() {
      JMenuBar menuBar = new JMenuBar();
      menuBar.add(fileMenu());
      menuBar.add(editMenu());
      menuBar.add(viewMenu());
      menuBar.add(helpMenu());
      menuBar.add(Box.createHorizontalGlue());
      JComboBox<Category.Type> categoryTypeSelector = new JComboBox<>(Category.Type.values());
      categoryTypeSelector.setSelectedItem(configurator.getCategoryType());
      categoryTypeSelector.addItemListener(e -> {
         if (e.getStateChange() == ItemEvent.SELECTED) {
            Category.Type categoryType = (Category.Type) categoryTypeSelector.getSelectedItem();
            if (categoryType != null) {
               configurator.setCategoryType(categoryType);
               if (echogramWindow != null) {
                  echogramWindow.reset();
               }
               initConfig();
               redraw(false);
            }
         }
      });
      if (KoronaIncubatorFeatureToggles.USE_TRACK_CATEGORIZATION) {
         menuBar.add(categoryTypeSelector);
      }
      menuBar.add(Box.createHorizontalGlue());
      Path file = configurator.getCategorizationFile();
      String text = file != null ? file.toString() : "<no categorization file selected>";
      menuBar.add(new JLabel(text));
      menuBar.add(Box.createHorizontalGlue());
      mainDialog.setJMenuBar(menuBar);

      Box vBox = Box.createVerticalBox();
      //vBox.add(menuBar);
      if (selectedPlotType != PlotType.ECHOGRAM) {
         vBox.add(categoryToggles());
      }
      return vBox;
   }

   private void initAxes() {
      List<FeatureExtractor> enabledFeatureExtractors = configurator.getEnabledFeatureExtractors();
      if (enabledFeatureExtractors.isEmpty()) {
         Log.global.warning("No enabled features in configurator");
         return;
      }
      xAxisFeature = enabledFeatureExtractors.get(0).getFeatureName();
      yAxisFeature = enabledFeatureExtractors.size() > 1 ? enabledFeatureExtractors.get(1).getFeatureName() : xAxisFeature;

      List<FeatureExtractor> operationalExtractors = configurator.getOperationalFeatureExtractors();
      if (!operationalExtractors.isEmpty()) {
         xAxisFeature = operationalExtractors.get(0).toString();
         yAxisFeature = operationalExtractors.size() > 1 ? operationalExtractors.get(1).toString() : xAxisFeature;
      }
   }

   private void initConfig() {
      closePopups();

      categoryConfigMap.clear(); // nb: after closePopups

      if (echogramWindow == null) {
         extractionCategory = null;
      } else {
         extractionCategory = new Category(configurator, EXTRACTED, EXTRACTION_COLOR);
         extractionCategory.addNeighborhood(new Category.NeighborhoodInitialization(echogramWindow, collectiveFeatureComputation));
         categoryConfigMap.put(extractionCategory, new CategoryConfiguration(extractionCategory));
      }

      for (Category category : configurator.getNonSpecialEnabledCategories()) {
         categoryConfigMap.put(category, new CategoryConfiguration(category));
      }

      //quantile = GaussUtils.quantileValue(configurator.outlierFraction.getFloatValue(), 1);
   }

   public static List<Graph> responseGraph(Category category, GaussDistribution distribution, Configurator configurator, FloatRange kHzRange, boolean drawConfidenceIntervals, @Nullable XYInfo xyInfo) {
      Graph meanGraph = new Graph(category.getName())
            .setXYInfo(xyInfo)
            .setColor(category.getColor());

      Graph stddevGraphA = new Graph(category.getName())
            .setXYInfo(xyInfo)
            .setColor(category.getColor())
            .setDashed();

      Graph stddevGraphB = new Graph(category.getName())
            .setXYInfo(xyInfo)
            .setColor(category.getColor())
            .setDashed();

      float workaroundMaxValue = 10000;
      //todo: this is a workaround for a java 1.6 bug:
      //Long dashed lines takes a very long time to draw and might result in vm crash.
      //https://bugs.openjdk.org/browse/JDK-6568969
      //remove when bug is fixed.

      boolean addedReferencePoint = false;
      int referenceKHz = Utils.hzToKHz(configurator.getReferenceFrequency());
      for (FeatureExtractor featureExtractor : configurator.getFeatureExtractors()) {
         String featureName = featureExtractor.getFeatureName();
         if (featureExtractor.isAdditional()) {
            continue;
         }
         if (!distribution.hasMean(featureName)) {
            continue;
         }
         FeatureExtractor.FrequencyFeatureExtractor ffe = (FeatureExtractor.FrequencyFeatureExtractor) featureExtractor;
         if (!addedReferencePoint && ffe.getKHz() > referenceKHz && kHzRange.containsIncludingEnd(referenceKHz)) {
            float referenceX = referenceKHz;
            meanGraph.addPoint(referenceX, 1);
            if (drawConfidenceIntervals) {
               stddevGraphA.addPoint(referenceX, 1);
               stddevGraphB.addPoint(referenceX, 1);
            }
            addedReferencePoint = true;
         }
         if (!kHzRange.containsIncludingEnd(ffe.getKHz())) {
            continue;
         }
         float x = ffe.getKHz();
         float mean = distribution.getMean(featureName);
         meanGraph.addPoint(x, Math.min(workaroundMaxValue, KoronaUtils.fromDB(mean)));
         if (drawConfidenceIntervals && distribution.isValid(featureName)) {
            float variance = distribution.getVariance(featureName);
            float quantile = distribution.getQuantile(configurator.outlierFraction.getFloatValue(), configurator.getEnabledFeatureExtractors());
            double delta = Math.sqrt(variance) * quantile;
            stddevGraphA.addPoint(x, Math.min(workaroundMaxValue, KoronaUtils.fromDB(mean + delta)));
            stddevGraphB.addPoint(x, Math.min(workaroundMaxValue, KoronaUtils.fromDB(mean - delta)));
         }
      }

      if (drawConfidenceIntervals) {
         return List.of(meanGraph, stddevGraphA, stddevGraphB);
      } else {
         return List.of(meanGraph);
      }
   }

   private Collection<Graph> probabilityGraphs(Neighborhood neighborhood, GaussDistribution distribution, Color color) {
      float xShift = axisShift(xAxisFeature);

      List<Float> values = new ArrayList<>(neighborhood.getNeighbors().size());
      float xMin = Float.POSITIVE_INFINITY;
      float xMax = Float.NEGATIVE_INFINITY;
      for (Neighbor neighbor : neighborhood.getNeighbors()) {
         Feature feature = neighbor.getFeature(xAxisFeature);
         if (feature != null) {
            float x = feature.value() + xShift;
            values.add(x);
            xMin = Math.min(xMin, x);
            xMax = Math.max(xMax, x);
         }
      }

      if (!distribution.isValid(xAxisFeature)) {
         return List.of();
      }

      Graph graphA = new Graph()
            .setColor(color);
      for (int i = 0, iMax = 100; i <= iMax; i++) {
         float x = xMin + i * (xMax - xMin) / iMax;
         Feature feature = new Feature(xAxisFeature, x - xShift);
         GaussDistribution.Probability probability = distribution.probability(List.of(feature));
         if (probability != null) {
            float y = probability.normalized();
            graphA.addPoint(x, y);
         }
      }

      Graph graphC = probabilityDensityGraph(values, xMax, xMin, color);

      return List.of(graphA, graphC);
   }

   private static Graph probabilityDensityGraph(List<Float> values, float xMax, float xMin, Color color) {
      Graph graph = new Graph()
            .setColor(color)
            .setDashed();

      int n = (int) Math.pow(values.size(), 0.5);
      float dx = (xMax - xMin) / n;
      int[] counts = new int[n + 1];
      for (float x : values) {
         int i = Math.round((x - xMin) / dx);
         if (i >= 0 && i < counts.length) {
            counts[i]++;
         }
      }
      for (int i = 0; i < counts.length; i++) {
         float x = xMin + i * dx;
         float y = counts[i] / (values.size() * dx);
         graph.addPoint(x, y);
      }
      graph.smooth();
      return graph;
   }

   private List<Graph> makeProbabilitiesPlot() {
      List<Graph> graphs = new ArrayList<>();
      for (Category category : getPlottableCategories()) {
         CategoryConfiguration ccf = categoryConfigMap.get(category);
         if (ccf.isThinnedNeighborhoodVisible()) {
            graphs.addAll(probabilityGraphs(category.getCategoryDistribution(distributionLevel).getNeighborhood(), category.getCategoryDistribution(distributionLevel).getGaussDistribution(), category.getColor()));
         }
         for (Category.CategoryNeighborhood categoryNeighborhood : ccf.visibleNeighborhoods) {
            if (!categoryNeighborhood.isDeleted()) {
               graphs.addAll(probabilityGraphs(categoryNeighborhood.getNeighborhoodData(distributionLevel).getNeighborhood(),
                     categoryNeighborhood.getNeighborhoodData(distributionLevel).getGaussDistribution(), category.getColor()));
            }
         }
      }
      return graphs;
   }

   private List<Graph> makeFreqResp(FloatRange kHzRange) {
      List<Graph> graphs = new ArrayList<>();
      for (Category category : getPlottableCategories()) {
         CategoryConfiguration ccf = categoryConfigMap.get(category);
         if (ccf.isThinnedNeighborhoodVisible()) {
            graphs.addAll(responseGraph(category, category.getCategoryDistribution(distributionLevel).getGaussDistribution(), configurator, kHzRange, drawConfidenceIntervals, null));
         }
         for (Category.CategoryNeighborhood categoryNeighborhood : ccf.visibleNeighborhoods) {
            if (!categoryNeighborhood.isDeleted()) {
               graphs.addAll(responseGraph(category, categoryNeighborhood.getNeighborhoodData(distributionLevel).getGaussDistribution(), configurator, kHzRange, drawConfidenceIntervals, null));
            }
         }
      }
      return graphs;
   }

   private List<Graph> makeScatter() {
      List<Graph> graphs = new ArrayList<>();

      for (Category category : getPlottableCategories()) {
         CategoryConfiguration ccf = categoryConfigMap.get(category);
         if (ccf.isThinnedNeighborhoodVisible()) {
            graphs.addAll(addScatter(category.getCategoryDistribution(distributionLevel).getNeighborhood(), category.getColor(), distributionLevel != Category.DistributionLevel.PIXEL));
            graphs.add(makeGaussEllipse(category.getCategoryDistribution(distributionLevel).getGaussDistribution(), category.getColor()));
         }
         for (Category.CategoryNeighborhood categoryNeighborhood : ccf.visibleNeighborhoods) {
            if (!categoryNeighborhood.isDeleted()) {
               graphs.addAll(addScatter(categoryNeighborhood.getNeighborhoodData(distributionLevel).getNeighborhood(), category.getColor(), distributionLevel != Category.DistributionLevel.PIXEL));
               graphs.add(makeGaussEllipse(categoryNeighborhood.getNeighborhoodData(distributionLevel).getGaussDistribution(), category.getColor()));
            }
         }
      }
      //plot cell averages for the extracted points
      if (extractionCategory != null && distributionLevel == Category.DistributionLevel.PIXEL &&
            categoryConfigMap.get(extractionCategory).isPlottable()) {
         Graph averagedScatter = new Graph()
               .setRenderer(() -> PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.SHAPES))
               .setColor(CollectiveFeatureComputation.CELL_COLOR);
         for (Neighbor neighbor : collectiveFeatureComputation.getCellNeighborhood().getNeighbors()) {
            Feature xFeature = neighbor.getFeature(xAxisFeature);
            Feature yFeature = neighbor.getFeature(yAxisFeature);
            if (xFeature != null && yFeature != null) {
               averagedScatter.addPoint(xFeature.value(), yFeature.value());
            }
         }
         //graphs.add(averagedScatter);
         //GaussDistribution gauss = new GaussDistribution(collectiveFeatureComputation.getCellNeighborhood());
         //graphs.addAll(addGaussEllipse(gauss, collectiveFeatureComputation.getCellColor()));
      }
      return graphs;
   }

   private Collection<Graph> addScatter(Neighborhood neighborhood, Color color, boolean useLargePoints) {
      Graph markedScatter = new Graph()
            .setRenderer(() -> PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.SHAPES))
            .setColor(MARKING_COLOR);

      Graph scatter = new Graph()
            .setRenderer(() -> {
               XYDotRenderer renderer = new XYDotRenderer();
               if (useLargePoints) {
                  renderer.setDotWidth(4);
                  renderer.setDotHeight(4);
               } else {
                  renderer.setDotWidth(2);
                  renderer.setDotHeight(2);
               }
               return renderer;
            })
            .setColor(color);

      float xShift = axisShift(xAxisFeature);
      float yShift = axisShift(yAxisFeature);

      for (Neighbor nb : neighborhood.getNeighbors()) {
         Feature xFeature = nb.getFeature(xAxisFeature);
         Feature yFeature = nb.getFeature(yAxisFeature);
         if (xFeature != null && yFeature != null) {
            boolean marked = echogramWindow != null
                  && nb instanceof EchogramWindow.IndexedNeighbor inb
                  && echogramWindow.isMarked(inb.getI(), inb.getJ());

            float x = xFeature.value() + xShift;
            float y = yFeature.value() + yShift;
            if (marked) {
               markedScatter.addPoint(x, y);
            } else {
               scatter.addPoint(x, y);
            }
         }
      }

      Collection<Graph> graphs = new ArrayList<>();
      if (!scatter.getPoints().isEmpty()) {
         graphs.add(scatter);
      }
      if (!markedScatter.getPoints().isEmpty()) {
         graphs.add(markedScatter);
      }
      return graphs;
   }

   public static Graph makeGaussEllipse(String name, GaussDistribution distribution, Color color, Configurator configurator,
                                        String xAxis, String yAxis, @Nullable XYInfo xyInfo) {
      float meanX = distribution.getMean(xAxis) + axisShift(xAxis);
      float meanY = distribution.getMean(yAxis) + axisShift(yAxis);
      float covXX = distribution.getVariance(xAxis);
      float covXY = distribution.getCovariance(xAxis, yAxis);
      float covYY = distribution.getVariance(yAxis);

      Graph ellipse = new Graph(name)
            .setXYInfo(xyInfo)
            .setRenderer(() -> PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.LINES))
            .setColor(color);
      float quantile = distribution.getQuantile(configurator.outlierFraction.getFloatValue(),
            configurator.getEnabledFeatureExtractors());
      //todo: should draw the projection of the n-dim ellipse into current plane
      //GaussUtils.drawEllipse(ellipse, meanX, meanY,
      //      quantile * Math.sqrt(covXX), quantile * Math.sqrt(covYY), 0);
      GaussUtils.drawGaussEllipse(ellipse, meanX, meanY,
            covXX, covXY, covYY, quantile);
      return ellipse;
   }

   private Graph makeGaussEllipse(GaussDistribution distribution, Color color) {
      return makeGaussEllipse("", distribution, color, configurator, xAxisFeature, yAxisFeature, null);
   }

   public static float axisShift(String featureName) {
      // Correcting Sv38 with IMR_CONSTANT
      return featureName.equals(FeatureExtractor.ADDITIONAL_FEATURE_SV38) ? (float) -KoronaUtils.toDB(PowerData.IMR_CONSTANT) : 0;
   }
}
