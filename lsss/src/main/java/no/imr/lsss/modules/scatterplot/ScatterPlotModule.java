package no.imr.lsss.modules.scatterplot;

import com.google.common.collect.Lists;
import no.imr.korona.computation.categorization.Category;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.computation.categorization.GaussUtils;
import no.imr.korona.computation.categorization.Neighbor;
import no.imr.korona.computation.feature.CategoryVisualizer;
import no.imr.korona.computation.feature.Feature;
import no.imr.korona.computation.feature.FeatureExtractor;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.region.Region;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.viewer.util.FrequencySelectionPanel;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.frequencyresponse.KoronaCategoriesSelection;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.WelfordsMethod2D;
import no.imr.tools.math.linalg.Vec2;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.ViewHolder;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public final class ScatterPlotModule extends BaseViewModule implements PojoDataContainer {
   private final ViewHolder<ScatterPlotView> viewHolder = new ViewHolder<>(() -> new ScatterPlotView(this));
   private final Listener recomputeListener = newCoalescingExecListener(this::recompute);
   private final FrequencySelectionPanel frequencySelectionPanel = new FrequencySelectionPanel(recomputeListener, true);

   private final KoronaCategoriesSelection koronaCategoriesSelection = new KoronaCategoriesSelection();

   private JFreeChart chart = PlotUtils.newEmptyChart();

   public ScatterPlotModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            koronaCategoriesSelection.plotCategories,
            koronaCategoriesSelection.koronaCategories
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getInterpretationSettings().getDataFileChangeManager(), newCoalescingExecListener(this::dataFilesChanged));

      registry.add(getInterpretationSettings().getChannelChangeManager(), newCoalescingExecListener(channel -> {
         frequencySelectionPanel.setHighlighted(channel);
         recomputeListener.listen();
      }));

      registry.add(getParameters(), recomputeListener);

      registry.add(recomputeListener, List.of(
            getInterpretationSettings().getPingSampler().getNewPingsChangeManager(),
            getInterpretationSettings().getPingRangeChangeManager(),
            getRegionManager().getThresholdManager().getChangeManager(),
            getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryChangeManager(),
            getRegionManager().selectedRegions()
      ));

      registry.add(getRegionManager().getRegionDefinitionChangeManager(), newExecListener(regionEvent -> {
         if (regionEvent.regions().stream().anyMatch(Region::isSelected)) {
            recomputeListener.listen();
         }
      }));

      //---

      dataFilesChanged();
   }

   @Override
   protected void onDisable() {
      setChart(PlotUtils.newEmptyChart());
   }

   private void dataFilesChanged() {
      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
      int channel = getInterpretationSettings().getChannel();
      frequencySelectionPanel.update(rawFileConfiguration, channel);
      if (frequencySelectionPanel.isChannelSelected(channel)) {
         int nextChannel = channel % rawFileConfiguration.getTransducerCount() + 1;
         frequencySelectionPanel.setChannelSelected(nextChannel, true);
      }
      recomputeListener.listen();
   }

   private void recompute() {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         plotNoGraphs(null);
         return;
      }

      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
      int channelR = Configurator.findReferenceChannel(rawFileConfiguration);
      if (channelR < 0) {
         plotNoGraphs("No reference channel");
         return;
      }
      int channelX = frequencySelectionPanel.getChannels().stream()
            .filter(frequencySelectionPanel::isChannelSelected)
            .findFirst()
            .orElse(1);
      int channelY = getInterpretationSettings().getChannel();

      boolean xIsRef = channelR == channelX;
      boolean yIsRef = channelR == channelY;

      String xAxis = (xIsRef ? "Sv" : FeatureExtractor.FREQUENCY_FEATURE_PREFIX) + rawFileConfiguration.getTransducers().get(channelX - 1).getKHz();
      String yAxis = (yIsRef ? "Sv" : FeatureExtractor.FREQUENCY_FEATURE_PREFIX) + rawFileConfiguration.getTransducers().get(channelY - 1).getKHz();

      Configurator configurator = new Configurator(createConfigFileSettings(), rawFileConfiguration);

      XYInfo xyInfo = new XYInfo(
            new ParameterExport(xAxis, Unit.DB, ExportRounding.db()),
            new ParameterExport(yAxis, Unit.DB, ExportRounding.db()));

      Graph scatter = new Graph("scatter")
            .setXYInfo(xyInfo)
            .setColor(Color.BLACK)
            .setRenderer(() -> {
               XYDotRenderer renderer = new XYDotRenderer();
               renderer.setDotWidth(2);
               renderer.setDotHeight(2);
               return renderer;
            });
      int count = 0;
      int maxCount = configurator.thinnedScatterSize.getIntValue();

      Random random = new Random();

      List<Ping> pings = getInterpretationSettings().getPingSampler().getAvailablePings();

      for (Region region : getRegionManager().getSelectedRegions()) {
         PingRange visibleRegionPingRange = region.getPingRange().intersection(pingRange);
         if (visibleRegionPingRange.isEmpty()) {
            continue;
         }

         for (Ping ping : pings) {
            if (!visibleRegionPingRange.contains(ping)) {
               continue;
            }
            PowerData powerDataR = ping.getPowerData(channelR);
            PowerData powerDataX = ping.getPowerData(channelX);
            PowerData powerDataY = ping.getPowerData(channelY);
            if (powerDataR == null || powerDataX == null || powerDataY == null) {
               continue;
            }

            FloatRange svRange = getRegionManager().getThresholdManager().getLogSvRange(ping.getPingIndex());

            float[] svArrayR = powerDataR.getLogSv();
            float[] svArrayX = powerDataX.getLogSv();
            float[] svArrayY = powerDataY.getLogSv();

            FloatRange commonDepthRange = powerDataR.getDepthRange()
                  .intersection(powerDataX.getDepthRange())
                  .intersection(powerDataY.getDepthRange());

            for (FloatRange depthRange : getRegionManager().getDepthRangesForChannel(region, ping, channelR)) {
               depthRange = depthRange.intersection(commonDepthRange);
               int iRBegin = powerDataR.depthToClampedSampleIndex(depthRange.min());
               int iREnd = powerDataR.depthToClampedSampleIndex(depthRange.max());

               for (int iR = iRBegin; iR < iREnd; iR++) {
                  float svR = svArrayR[iR];
                  if (!svRange.contains(svR)) {
                     continue;
                  }
                  float depth = powerDataR.getSampleDepth(iR);
                  float svX = svArrayX[powerDataX.depthToClampedSampleIndex(depth)];
                  if (!svRange.contains(svX)) {
                     continue;
                  }
                  float svY = svArrayY[powerDataY.depthToClampedSampleIndex(depth)];
                  if (!svRange.contains(svY)) {
                     continue;
                  }
                  float x = xIsRef ? svR : svX - svR;
                  float y = yIsRef ? svR : svY - svR;
                  count++;
                  if (count <= maxCount) {
                     scatter.addPoint(x, y);
                  } else {
                     int i = random.nextInt(count);
                     if (i < maxCount) {
                        scatter.getPoints().set(i, new Vec2(x, y));
                     }
                  }
               }
            }
         }
      }

      Graph ellipse = new Graph("ellipse")
            .setXYInfo(xyInfo)
            .setColor(Color.BLACK)
            .setRenderer(() -> PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.LINES));
      WelfordsMethod2D stat = new WelfordsMethod2D();
      scatter.getPoints().forEach(stat::update);
      double quantile = GaussUtils.quantileValue(configurator.outlierFraction.getFloatValue(), configurator.getEnabledFeatureExtractors().size());
      GaussUtils.drawGaussEllipse(ellipse, stat.meanX(), stat.meanY(), stat.covXX(), stat.covXY(), stat.covYY(), quantile);

      List<Graph> graphs = Lists.newArrayList(ellipse, scatter);

      if (koronaCategoriesSelection.plotCategories.getBooleanValue()) {
         graphs.addAll(createCategoryGraphs(configurator, xAxis, yAxis, xyInfo, scatter.getRenderer()));
      }

      JFreeChart chart = new Plotter(graphs)
            .xAxis(xAxis + " [dB]")
            .yAxis(yAxis + " [dB]")
            .createChart();
      setChart(chart);
   }

   private ConfigFileSettings createConfigFileSettings() {
      try {
         return getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getMainSetup().createConfigFileSettings();
      } catch (IOException e) {
         return getLSSS().getKorona().createConfigFileSettings();
      }
   }

   private List<Graph> createCategoryGraphs(Configurator configurator, String xAxis, String yAxis, XYInfo xyInfo, Supplier<XYItemRenderer> renderer) {
      List<Category> categories = configurator.getNonSpecialEnabledCategories();
      koronaCategoriesSelection.update(categories);

      float xShift = CategoryVisualizer.axisShift(xAxis);
      float yShift = CategoryVisualizer.axisShift(yAxis);

      List<Graph> graphs = new ArrayList<>();
      for (Category category : categories) {
         if (!koronaCategoriesSelection.isSelected(category)) {
            continue;
         }

         Category.CategoryDistribution distribution = category.getCategoryDistribution(Category.DistributionLevel.PIXEL);

         graphs.add(CategoryVisualizer.makeGaussEllipse(category.getName() + "Ellipse", distribution.getGaussDistribution(), category.getColor(), configurator, xAxis, yAxis, xyInfo));

         Graph scatter = new Graph(category.getName() + "Scatter")
               .setXYInfo(xyInfo)
               .setRenderer(renderer)
               .setColor(category.getColor());
         for (Neighbor neighbor : distribution.getNeighborhood().getNeighbors()) {
            Feature xFeature = neighbor.getFeature(xAxis);
            Feature yFeature = neighbor.getFeature(yAxis);
            if (xFeature != null && yFeature != null) {
               float x = xFeature.value() + xShift;
               float y = yFeature.value() + yShift;
               scatter.addPoint(x, y);
            }
         }
         graphs.add(scatter);
      }
      return graphs;
   }

   private void plotNoGraphs(@Nullable String message) {
      JFreeChart chart = new Plotter(List.of())
            .createChart();
      chart.getXYPlot().setNoDataMessage(message);
      setChart(chart);
   }

   FrequencySelectionPanel getFrequencySelectionPanel() {
      return frequencySelectionPanel;
   }

   JFreeChart getChart() {
      return chart;
   }

   private void setChart(JFreeChart chart) {
      this.chart = chart;
      viewHolder.ifViewDelayed(this, ScatterPlotView::updateChart);
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .withDatasets(chart.getXYPlot())
            .build();
   }
}
