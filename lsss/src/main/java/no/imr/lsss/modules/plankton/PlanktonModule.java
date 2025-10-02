package no.imr.lsss.modules.plankton;

import no.imr.korona.computation.plankton.PlanktonFile;
import no.imr.korona.computation.plankton.PlanktonFileService;
import no.imr.korona.computation.plankton.PlanktonRectangle;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.korona.data.datagrams.Pid0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.region.Region;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.survey.preprocessing.PreprocessingConf;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.tools.listening.ListenableProperty;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.ViewHolder;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * For visualizing the results of plankton inversion.
 */
public final class PlanktonModule extends BaseViewModule implements PojoDataContainer {
   private final ViewHolder<PlanktonModuleView> viewHolder = new ViewHolder<>(() -> new PlanktonModuleView(this));
   private List<HistogramDisplay> histogramDisplays = List.of();
   private final ListenableProperty<Boolean> useThresholds = new ListenableProperty<>(false);
   private final Listener refreshListener = newCoalescingExecListener(this::refresh);
   private final Map<Region, RegionCache> regionMap = new HashMap<>();

   public PlanktonModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(newCoalescingExecListener(this::recompute), List.of(
            getConfigurationManager().getGridConf().horizontalGridUnit,
            getInterpretationSettings().getChannelChangeManager(),
            getInterpretationSettings().getPingRangeChangeManager(),
            getRegionManager().getThresholdManager().getChangeManager(),
            useThresholds
      ));

      registry.add(getInterpretationSettings().getPingSampler().getNewPingsChangeManager(), newExecListener(this::processPings));
      registry.add(getRegionManager().selectedRegions(), refreshListener);
      registry.add(getRegionManager().getRegionDeletedChangeManager(), newExecListener(regions -> {
         regionMap.keySet().removeAll(regions);
         refreshListener.listen();
      }));
      registry.add(getRegionManager().getRegionDefinitionChangeManager(), newExecListener(regionEvent -> {
         PingRange pingRange = regionEvent.pingRange();
         for (Region region : regionEvent.regions()) {
            RegionCache regionCache = regionMap.get(region);
            if (regionCache != null) {
               regionCache.getPingMap().subMap(pingRange.begin(), pingRange.end()).clear();
            }
         }
         refreshListener.listen();
      }));

      registry.add(getInterpretationSettings().getDataFileChangeManager(), newCoalescingExecListener(this::createHistogramDisplays));

      //---

      createHistogramDisplays();
      recompute();
   }

   @Override
   protected void onDisable() {
      regionMap.clear();
   }

   List<HistogramDisplay> getHistogramDisplays() {
      return histogramDisplays;
   }

   ListenableProperty<Boolean> getUseThresholds() {
      return useThresholds;
   }

   @Nullable
   Path getPlanktonFile() throws IOException {
      PreprocessingConf preprocessingConf = getConfigurationManager().getSurveyConfiguration().getPreprocessingConf();
      ConfigFileSettings configFileSettings = preprocessingConf.getMainSetup().createConfigFileSettings();
      return configFileSettings.getFile(PlanktonFileService.NAME);
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private void recompute() {
      regionMap.clear();
      refreshListener.listen();
   }

   private void refresh() {
      processPings(getInterpretationSettings().getPingSampler().getAvailablePings());
   }

   private void processPings(List<Ping> pings) {
      PingRange pingRange = getInterpretationSettings().getPingRange();

      if (pingRange.isEmpty()) {
         plot(new HistogramMap());
         return;
      }

      PingMapping pingMapping = getConfigurationManager().getGridConf().horizontalGridUnit.getValue();

      int channel = getInterpretationSettings().getChannel();

      HistogramMap histogramMap = new HistogramMap();

      for (Region region : getRegionManager().getSelectedRegions()) {
         PingRange visibleRegionPingRange = region.getPingRange().intersection(pingRange);
         if (visibleRegionPingRange.isEmpty()) {
            continue;
         }

         RegionCache regionCache = regionMap.get(region);
         if (regionCache == null) {
            regionCache = new RegionCache();
            regionMap.put(region, regionCache);
         }

         boolean regionNeedUpdate = false;

         for (Ping ping : pings) {
            PingIndex pingIndex = ping.getPingIndex();

            if (!visibleRegionPingRange.contains(pingIndex)) {
               continue;
            }

            PingCache pingCache = regionCache.getPingMap().get(pingIndex);
            if (pingCache == null) {
               PowerData powerData = ping.getPowerData(channel);
               Pic0Datagram pic0 = ping.getPingConfiguration().getConfigurationItem(Pic0Datagram.class);
               Pid0Datagram pid0 = ping.getPingItem(Pid0Datagram.class);
               if (powerData == null || pic0 == null || pid0 == null) {
                  continue;
               }
               FloatRange svRange = useThresholds.getValue() ? getRegionManager().getThresholdManager().getLinearSvRange(pingIndex) : FloatRange.ALL;
               List<FloatRange> depthRanges = getRegionManager().getDepthRangesForChannel(region, ping, channel).getFloatRanges();
               pingCache = new PingCache(depthRanges, pic0, pid0, powerData, svRange);
               regionCache.getPingMap().put(pingIndex, pingCache);
               regionNeedUpdate = true;
            }
         }

         if (regionNeedUpdate) {
            regionCache.update(visibleRegionPingRange, pingMapping);
         }

         histogramMap.accumulate(regionCache.getHistogramMap(), 1);
      }

      convertToFixedBins(histogramMap);

      plot(histogramMap);
   }

   private void convertToFixedBins(HistogramMap histogramMap) {
      for (HistogramDisplay histogramDisplay : histogramDisplays) {
         Pic0Datagram.PlanktonCategory planktonCategory = histogramDisplay.getPlanktonCategory();
         Histogram histogram = histogramMap.getHistogram(planktonCategory);
         float binSize = histogramDisplay.getFixedBinSize();
         histogram = histogram.toFixedBinHistogram(binSize);
         histogramMap.getHistograms().put(planktonCategory, histogram);
      }
   }

   private void plot(HistogramMap histogramMap) {
      for (HistogramDisplay histogramDisplay : histogramDisplays) {
         histogramDisplay.setHistogram(histogramMap.getHistogram(histogramDisplay.getPlanktonCategory()));
      }
   }

   private void createHistogramDisplays() {
      Pic0Datagram pic0 = getInterpretationSettings().getDataFileSet().getConfigurationItem(Pic0Datagram.class);
      if (pic0 != null) {
         histogramDisplays = pic0.getPlanktonCategories().stream()
               .filter(Predicate.not(Pic0Datagram.PlanktonCategory::isSpecial))
               .map(HistogramDisplay::new)
               .toList();
      } else {
         histogramDisplays = List.of();
      }

      updateHistogramRanges();

      viewHolder.ifView(PlanktonModuleView::updateHistogramPanel);
   }

   private void updateHistogramRanges() {
      if (histogramDisplays.isEmpty()) {
         // If nothing to display, then avoid possibly trigger IO errors due to missing plankton file.
         return;
      }

      Path file;
      try {
         file = getPlanktonFile();
      } catch (IOException e) {
         SwingUtilities.invokeLater(() -> getLSSS().showError(getComponent(), "Could not get plankton file", e));
         return;
      }

      if (file == null) {
         SwingUtilities.invokeLater(() -> getLSSS().showError(getComponent(), "No plankton file is configured"));
         return;
      }

      PlanktonFile planktonFile;
      try {
         planktonFile = new PlanktonFile(file);
      } catch (IOException e) {
         SwingUtilities.invokeLater(() -> getLSSS().showError(getComponent(), "Error reading plankton file " + file, e));
         return;
      }

      for (HistogramDisplay histogramDisplay : histogramDisplays) {
         List<PlanktonRectangle> planktonRectangles = planktonFile.getPlanktonRectangles().get(histogramDisplay.getPlanktonCategory().getLegend());
         if (planktonRectangles != null) {
            histogramDisplay.updateRange(planktonRectangles);
         }
      }
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .withDatasets(histogramDisplays.stream().map(HistogramDisplay::getPlot))
            .build();
   }
}
