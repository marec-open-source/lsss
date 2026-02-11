package no.imr.lsss.modules.ctd;

import no.imr.korona.util.ExportRounding;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.tools.Utils;
import no.imr.tools.geo.Earth;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.icons.MiscIcons;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jspecify.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

public final class CTDViewModule extends BaseViewModule implements PojoDataContainer {
   static final DateTimeFormatter DATE_TIME_FORMATTER = Utils.createUTCDateTimeFormatter("yyyy.MM.dd HH:mm");

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   private final Supplier<CTDDataModule> ctdDataModule = moduleSupplier(CTDDataModule.class);
   private @Nullable CTDData currentCTDData;
   private int currentCTDDataIndex;
   private int activeColumn;
   private JFreeChart chart = PlotUtils.newEmptyChart();

   public CTDViewModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(ctdDataModule.get().getChangeManager(), newCoalescingExecListener(this::resetCurrentCTDDataIndex));

      registry.add(getInterpretationSettings().mouseover().pingIndex(), newCoalescingExecListener(optionalPingIndex -> {
         optionalPingIndex.ifPresent(pingIndex -> {
            setCurrentCTDDataIndex(getClosestCTDDataIndex(pingIndex.getTimeInMillis()));
         });
      }));

      //---

      resetCurrentCTDDataIndex();
   }

   @Override
   protected void onDisable() {
      setCurrentCTDDataIndex(-1);
   }

   private void resetCurrentCTDDataIndex() {
      if (ctdDataModule.get().getCTDDatas().isEmpty()) {
         setCurrentCTDDataIndex(-1);
      } else {
         setCurrentCTDDataIndex(0);
      }
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private void plotData() {
      if (currentCTDData == null) {
         plotEmptyPlot();
         return;
      }

      String name = currentCTDData.columnNames().get(activeColumn);
      Graph graph = new Graph(name)
            .setXYInfo(new XYInfo(
                  new ParameterExport(name, Unit.NONE, ExportTransform.identity()),
                  new ParameterExport("depth", Unit.METER, ExportRounding.depth())))
            .setColor(Color.RED);

      for (float[] row : currentCTDData.rows()) {
         float value = row[activeColumn];
         float depth = row[currentCTDData.depthColumn()];
         graph.addPoint(value, depth);
      }

      JFreeChart chart = new Plotter(List.of(graph))
            .xAxis(currentCTDData.columnNames().get(activeColumn))
            .yAxis("Depth [m]")
            .createChart();
      chart.getXYPlot().getRangeAxis().setInverted(true);

      String text = Utils.format("%s   Station:%s   %s",
            DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(currentCTDData.timeInMillis())),
            currentCTDData.stationNumber(),
            Earth.formatGeoPoint(currentCTDData.geographicalPosition(), "%.2f"));
      chart.addSubtitle(PlotUtils.newTextTitle(text, HorizontalAlignment.RIGHT));

      setChart(chart);
   }

   private void plotEmptyPlot() {
      JFreeChart chart = new Plotter(List.of())
            .createChart();
      setChart(chart);
   }

   private void setChart(JFreeChart chart) {
      this.chart = chart;
      viewHolder.ifViewDelayed(this, View::updateChart);
   }

   private void setCurrentCTDDataIndex(int index) {
      CTDData ctdData = index == -1 ? null : ctdDataModule.get().getCTDDatas().get(index);
      if (currentCTDDataIndex == index && currentCTDData == ctdData) {
         return;
      }

      if (ctdData != null) {
         if (currentCTDData == null) {
            activeColumn = CTDDataModule.findColumnIndex(ctdData.columnNames(), "temp", 0);
         }

         // If number of columns is less than selected column, then use last column.
         // This can happen if different ctd formats has been read
         if (activeColumn >= ctdData.columnNames().size()) {
            activeColumn = ctdData.columnNames().size() - 1;
         }
      }

      currentCTDDataIndex = index;
      currentCTDData = ctdData;

      viewHolder.ifView(View::updateGUI);
      plotData();
   }

   private int getClosestCTDDataIndex(long targetTimeInMillis) {
      int ctdDataIndex = -1;
      long smallestDiff = Long.MAX_VALUE;

      List<CTDData> ctdDatas = ctdDataModule.get().getCTDDatas();
      for (int i = 0; i < ctdDatas.size(); i++) {
         CTDData currentCTDData = ctdDatas.get(i);

         long ctdTimeInMillis = currentCTDData.timeInMillis();
         long diff = Math.abs(ctdTimeInMillis - targetTimeInMillis);

         if (diff < smallestDiff) {
            smallestDiff = diff;
            ctdDataIndex = i;
         }
      }

      return ctdDataIndex;
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .withDatasets(chart.getXYPlot())
            .build();
   }

   private static final class View extends BaseView {
      private final CTDViewModule module;

      private final JPanel mainPanel = new JPanel(new BorderLayout());
      private final ChartPanel chartPanel;

      private final JButton prevButton = MiscIcons.NAVIGATE_PREVIOUS.on(new JButton());
      private final JButton nextButton = MiscIcons.NAVIGATE_NEXT.on(new JButton());
      private final JComboBox<String> columnComboBox = new JComboBox<>();

      private View(CTDViewModule module) {
         super(module);

         this.module = module;
         chartPanel = PlotUtils.newChartPanel(module.chart);
         prevButton.addActionListener(_ -> module.setCurrentCTDDataIndex(module.currentCTDDataIndex - 1));
         nextButton.addActionListener(_ -> module.setCurrentCTDDataIndex(module.currentCTDDataIndex + 1));

         Insets margin = new Insets(0, 3, 0, 3);
         nextButton.setMargin(margin);
         prevButton.setMargin(margin);

         columnComboBox.setMinimumSize(new Dimension(50, 0));
         columnComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
               module.activeColumn = columnComboBox.getSelectedIndex();
               module.plotData();
            }
         });

         JPanel buttonPanel = new JPanel(new BorderLayout());
         buttonPanel.setBackground(Color.WHITE);
         buttonPanel.add(prevButton, BorderLayout.WEST);
         buttonPanel.add(columnComboBox);
         buttonPanel.add(nextButton, BorderLayout.EAST);

         mainPanel.add(chartPanel);
         mainPanel.add(buttonPanel, BorderLayout.SOUTH);

         updateGUI();
      }

      @Override
      public void addToFloatableModuleMenu(JPopupMenu popupMenu) {
         JMenuItem visualizerItem = MiscIcons.SCATTER_PLOT.on(popupMenu.add("Visualizer dialog..."));
         visualizerItem.addActionListener(_ -> new CTDVisualizerDialog(module.ctdDataModule.get(), mainPanel));

         CTDData ctdData = module.currentCTDData;
         Path file = ctdData != null ? ctdData.file() : null;
         if (file != null) {
            JMenuItem openItem = popupMenu.add("Open " + file.getFileName());
            openItem.addActionListener(_ -> GuiUtils.desktopOpen(file, mainPanel));
         }
      }

      @Override
      public JComponent getComponent() {
         return mainPanel;
      }

      private void updateChart() {
         chartPanel.setChart(module.chart);
      }

      private void updateGUI() {
         updateButtonsEnabledState();

         if (module.currentCTDData != null) {
            columnComboBox.setModel(new DefaultComboBoxModel<>(module.currentCTDData.columnNames().toArray(String[]::new)));
            columnComboBox.setSelectedIndex(module.activeColumn);
         } else {
            columnComboBox.setModel(new DefaultComboBoxModel<>());
         }
      }

      private void updateButtonsEnabledState() {
         columnComboBox.setEnabled(module.currentCTDData != null);
         nextButton.setEnabled(module.currentCTDDataIndex < module.ctdDataModule.get().getCTDDatas().size() - 1);
         prevButton.setEnabled(module.currentCTDDataIndex > 0);
      }
   }
}
