package no.imr.lsss.modules.interpretation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.util.geometry.depth.CoordinatedBottomDepthTransform;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.region.ChannelInterpretation;
import no.imr.korona.region.ConditionalPingMask;
import no.imr.korona.region.Region;
import no.imr.korona.region.storing.StoringIntervalConfig;
import no.imr.korona.util.KoronaUtils;
import no.imr.lsss.database.tables.QualityEnum;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.InterpretationZSettings;
import no.imr.lsss.framework.config.survey.GridConf;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.echogram.overlays.GridOverlay;
import no.imr.lsss.modules.integration.IntegrationArea;
import no.imr.lsss.modules.integration.RegionIntegrationModule;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.tools.ResourceUtils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.IntCsvListParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.DoubleRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.range.RangeSet;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.WorkerDialog;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The module for interpreting regions.
 */
public final class InterpretationModule extends BaseViewModule implements PojoDataContainer {
   private final InterpretationSummary interpretationSummary;
   private boolean canStoreSaved;
   private boolean canStoreNew;
   private boolean mouseOverStoreButton;
   private boolean mouseOverDeleteButton;
   private @Nullable Runnable resetDepthTransform;
   private final Listener updateGridOverlaysListener = newCoalescingExecListener(this::updateGridOverlays);
   private boolean storing;

   private final ViewHolder<InterpretationModuleView> viewHolder = new ViewHolder<>(() -> new InterpretationModuleView(this));

   private int previouslySelectedChannel = -1;

   public final FrequencyResponseFunctionParameter frequencyResponseFunction = new FrequencyResponseFunctionParameter(
         new Name("FrequencyResponseFunction", "Frequency response function"));

   public final BooleanParameter showQualityOptions = new BooleanParameter(
         new Name("ShowQualityOptions", "Show quality options"),
         false,
         "Show quality options in module instead of in dialog when storing");

   public final ObjectParameter<QualityEnum> quality = new ObjectParameter<>(
         new Name("Quality"),
         QualityEnum.HIGH, QualityEnum.values(),
         "Currently selected quality");

   public final IntCsvListParameter frequencies = new IntCsvListParameter(
         new Name("Frequencies"),
         List.of(), Unit.KHZ,
         "Frequencies to store");

   private final Supplier<RegionIntegrationModule> regionIntegrationModule = moduleSupplier(RegionIntegrationModule.class);

   private final InterpretationModuleStoreTask interpretationModuleStoreTask = new InterpretationModuleStoreTask(this);
   private final List<StoreTask> storeTasks = new ArrayList<>();

   public InterpretationModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      interpretationSummary = getLSSS().getInterpretationSummary();
      addStoreTask(interpretationModuleStoreTask);

      frequencies.subscribe(_ -> normalizeFrequencies());
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            frequencyResponseFunction,
            showQualityOptions,
            quality,
            frequencies
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(viewHolder.coalescingListener(InterpretationModuleView::updateQualityLabel), List.of(
            getInterpretationSettings().getPingRangeChangeManager(),
            getRegionManager().getStoringConfigManager().getChangeManager()
      ));
      registry.add(viewHolder.coalescingListener(InterpretationModuleView::updateQualityPanel), List.of(
            showQualityOptions,
            quality
      ));
      registry.add(viewHolder.coalescingListener(InterpretationModuleView::updateFrequencyPanel), List.of(
            frequencies,
            getInterpretationSettings().getDataFileChangeManager()
      ));

      registry.add(newExecListener(this::updateStoreDelete), List.of(
            getInterpretationSettings().getPingRangeChangeManager(),
            getConfigurationManager().getGridConf().horizontalGridUnit,
            getConfigurationManager().getGridConf().horizontalGridSize,
            getConfigurationManager().getGridConf().minHorizontalGridSize,
            interpretationSummary.getChangeManager(),
            getRegionManager().getStoringConfigManager().getChangeManager(),
            getRegionManager().getExclusionManager().getChangeManager(),
            getConfigurationManager().getSurveyConf().mSurvey
      ));

      registry.add(interpretationSummary.getChangeManager(),
            viewHolder.coalescingListener(InterpretationModuleView::updateAcousticCategoryButtons));

      registry.add(getRegionManager().getInterpretationChangeManager(), source -> {
         if (source != this) {
            viewHolder.ifView(InterpretationModuleView::interpretationChanged);
         }
      });

      registry.add(viewHolder.coalescingListener(InterpretationModuleView::updateNbPanel), List.of(
            getConfigurationManager().getGridConf().horizontalGridSize,
            getConfigurationManager().getGridConf().schoolHorizontalGridSize,
            getConfigurationManager().getGridConf().horizontalGridUnit,
            getInterpretationSettings().getPingMappingChangeManager(),
            getInterpretationSettings().getPingRangeChangeManager(),
            getInterpretationSettings().getPelagicZSettings().getZoomedChangeManager(),
            getInterpretationSettings().getBottomZSettings().getZoomedChangeManager(),
            getConfigurationManager().getSurveyMiscConf().pelagicMode,
            getConfigurationManager().getSurveyMiscConf().showWarningIfNotUsingPreferredLowerThreshold,
            getConfigurationManager().getSurveyMiscConf().showWarningIfVerticallyZoomed,
            getRegionManager().getThresholdManager().getChangeManager()
      ));

      registry.add(viewHolder.coalescingListener(InterpretationModuleView::updatePreferredHorizontalSizeButtons), List.of(
            getConfigurationManager().getGridConf().horizontalGridUnit,
            getConfigurationManager().getGridConf().preferredHorizontalSizes,
            getInterpretationSettings().getDataFileChangeManager()
      ));

      registry.add(getInterpretationSettings().getDataFileChangeManager(), newExecListener(() -> {
         previouslySelectedChannel = -1;
         normalizeFrequencies();
         if (frequencies.getValue().isEmpty()) {
            int mainKHz = KoronaUtils.hzToKHz(getConfigurationManager().getSurveyMiscConf().mainFrequency.getFloatValue());
            if (getInterpretationSettings().getDataFileSet().getRawFileConfiguration().getTransducers().stream().anyMatch(transducer -> transducer.getKHz() == mainKHz)) {
               frequencies.setValue(List.of(mainKHz));
            }
         }
      }));

      registry.add(getInterpretationSettings().getPingSampler().getNewPingsChangeManager(), updateGridOverlaysListener);

      registry.add(getInterpretationSettings().getChannelChangeManager(), newExecListener(channel -> {
         inheritInterpretation(previouslySelectedChannel, channel);
         previouslySelectedChannel = channel;
      }));

      registry.add(viewHolder.coalescingListener(InterpretationModuleView::updateInterpretationTable), List.of(
            getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryChangeManager(),
            regionIntegrationModule.get().getRegionIntegrationChangeManager()
      ));

      registry.add(viewHolder.coalescingListener(InterpretationModuleView::updateStatusTable), List.of(
            getRegionManager().getRegionDefinitionChangeManager(),
            regionIntegrationModule.get().getRegionIntegrationChangeManager(),
            getInterpretationSettings().getPingRangeChangeManager(),
            getInterpretationSettings().getChannelChangeManager()
      ));

      registry.add(viewHolder.coalescingListener(InterpretationModuleView::updateParameterTable), List.of(
            getInterpretationSettings().getPingRangeChangeManager(),
            getRegionManager().getBubbleCorrectionManager().getChangeManager()
      ));

      registry.add(getRegionManager().selectedRegions(), newExecListener(this::selectedRegionsChanged));

      //---

      previouslySelectedChannel = -1;
      updateStoreDelete();
      selectedRegionsChanged();
      updateGridOverlaysListener.listen();
      viewHolder.ifView(InterpretationModuleView::updateAll);
   }

   @Override
   public String getInfoText() {
      return "<h2>" + getDisplayName() +
            "</h2><table cellpadding=0><tr valign=top><td>The frequency response function can be applied on the selected region " +
            "by pressing \"F\" button found in the upper right corner in the of the Interpretation window. " +
            "Place the mouse cursor over the button and press the Spacebar " +
            "to switch between the different modes (\"F\", \"*\", ...) of the function.</td>" +
            "<td><img src='" + ResourceUtils.getUrl("no/imr/lsss/resources/modules/interpretation/koronaColumn.png") + "'></td></tr></table>" +
            "<p>Examples of manually specified functions:" +
            "<ol>" +
            "  <li>Equal backscatter on all frequencies: \"1\"</li>" +
            "  <li>Swimbladdered fish (herring): \"pow(38000 / F, 0.4)\"</li>" +
            "  <li>Atlantic mackerel: \"-0.03 + pow(38000/F,0.5) + pow(F/125000,3) - pow(F/200000,5.5)\"</li>" +
            "</ol>";
   }

   public QualityEnum getQuality() {
      return quality.getValue();
   }

   public void setQuality(QualityEnum qualityEnum) {
      quality.setValue(qualityEnum);
   }

   private void normalizeFrequencies() {
      Set<Integer> availableKHz = getInterpretationSettings().getDataFileSet().getRawFileConfiguration().getTransducers().stream()
            .map(RawFileTransducer::getKHz)
            .collect(Collectors.toSet());
      NavigableSet<Integer> frequencySet = new TreeSet<>(frequencies.getValue());
      if (!availableKHz.isEmpty()) {
         frequencySet.retainAll(availableKHz);
      }
      frequencies.setValue(List.copyOf(frequencySet));
   }

   List<Region> getSelectedRegions() {
      return getRegionManager().getSelectedRegions();
   }

   boolean canStoreSaved() {
      return canStoreSaved;
   }

   boolean canStoreNew() {
      return canStoreNew;
   }

   void setMouseOverStoreButton(boolean mouseOverStoreButton) {
      this.mouseOverStoreButton = mouseOverStoreButton;
      updateGridOverlaysListener.listen();
   }

   void setMouseOverDeleteButton(boolean mouseOverDeleteButton) {
      this.mouseOverDeleteButton = mouseOverDeleteButton;
      updateGridOverlaysListener.listen();
   }

   private void updateGridOverlays() {
      if (getInterpretationSettings().getPingRange().isEmpty()) {
         resetDepthTransform = null;
         updateGridOverlays(List.of(), null, null);
         return;
      }

      if (mouseOverStoreButton || mouseOverDeleteButton) {
         if (resetDepthTransform == null) {
            InterpretationZSettings.Bottom bottomZSettings = getInterpretationSettings().getBottomZSettings();
            DepthTransform depthTransform = bottomZSettings.getDepthTransform();
            int channel = getInterpretationSettings().getChannel();
            resetDepthTransform = () -> bottomZSettings.setDepthTransform(depthTransform, channel);
            bottomZSettings.setDepthTransform(new CoordinatedBottomDepthTransform(getLSSS().getDataManager()), channel);
         }
      } else {
         if (resetDepthTransform != null) {
            resetDepthTransform.run();
            resetDepthTransform = null;
         }
      }

      if (mouseOverStoreButton) {
         GridConf gridConf = getConfigurationManager().getGridConf();
         double distance = gridConf.horizontalGridUnit.getValue().distance(getInterpretationSettings().getPingRange());
         double horizontalGridSize = gridConf.horizontalGridSize.getDoubleValue();
         if (distance / horizontalGridSize > getInterpretationSettings().getPingSettings().getWidth()) {
            updateGridOverlays(List.of(), null, "Grid too dense for display");
         } else if (!canStoreSaved && frequencies.getValue().isEmpty()) {
            updateGridOverlays(List.of(), null, "No frequencies selected");
         } else {
            GridParameters gridParameters = new GridParameters(this);
            boolean skipSchoolGrids = distance / gridConf.schoolHorizontalGridSize.getDoubleValue() > getInterpretationSettings().getPingSettings().getWidth();
            Stream<StoreInput> storeInputStream = canStoreSaved ? findSavedStoreInputs() : findNewStoreInputs();
            List<GridIntegrator> gridIntegrators = storeInputStream
                  .parallel()
                  .map(storeInput -> {
                     GridIntegrator gridIntegrator = new GridIntegrator(new GridParameters.PerInterval(gridParameters, storeInput));
                     if (skipSchoolGrids) {
                        gridIntegrator.skipSchoolGrids();
                     }
                     List<Ping> pings = getInterpretationSettings().getPingSampler().getAvailablePings().stream()
                           .filter(storeInput.gridColumnInterval.pingRange()::contains)
                           .toList();
                     gridIntegrator.integrate(pings, null, new AsyncHandle());
                     return gridIntegrator;
                  })
                  .toList();
            updateGridOverlays(gridIntegrators, null, skipSchoolGrids ? "School grid too dense for display" : null);
         }
      } else if (mouseOverDeleteButton) {
         InterpretationSummary.ScatterSet scatterSet = interpretationSummary.getScatterSet(getInterpretationSettings().getPingRange());
         if (scatterSet.isEmpty()) {
            updateGridOverlays(List.of(), null, null);
         } else if (scatterSet.isEmptyForFrequency(getInterpretationSettings().getFrequency())) {
            updateGridOverlays(List.of(), null, "Nothing stored on " + KoronaUtils.hzToKHz(getInterpretationSettings().getFrequency()) + " kHz");
         } else {
            updateGridOverlays(List.of(), scatterSet, null);
         }
      } else {
         updateGridOverlays(List.of(), null, null);
      }
   }

   private void updateGridOverlays(List<GridIntegrator> gridIntegrators, InterpretationSummary.@Nullable ScatterSet scatterSet, @Nullable String message) {
      getModuleManager().getModules(GridOverlay.class).forEach(gridOverlay -> {
         gridOverlay.setGridData(gridIntegrators, scatterSet, message);
      });
   }

   InterpretationModuleStoreTask getInterpretationModuleStoreTask() {
      return interpretationModuleStoreTask;
   }

   List<StoreTask> getStoreTasks() {
      return storeTasks;
   }

   public void addStoreTask(StoreTask storeTask) {
      storeTasks.add(storeTask);
      viewHolder.ifView(InterpretationModuleView::updateQualityPanel);
   }

   private Stream<StoreInput> storeInputStream(StoreAction storeAction) {
      return switch (storeAction) {
         case AUTO -> canStoreSaved ? findSavedStoreInputs() : findNewStoreInputs();
         case ONLY_PREVIOUSLY_STORED -> findSavedStoreInputs();
         case ALL -> findAllStoreInputs();
      };
   }

   public boolean store(StoreAction storeAction) {
      List<StoreInput> storeInputs = storeInputStream(storeAction).toList();
      RangeSet<PingIndex> storedPingRangeSet = new ArrayRangeSet<>();
      storeInputs.forEach(storeInput -> {
         getRegionManager().getStoringConfigManager().put(storeInput.gridColumnInterval.pingRange(), storeInput.intervalConfig);
         storableGridColumnIntervals(storeInput.gridColumnInterval).forEach(gridColumnInterval -> {
            storedPingRangeSet.add(gridColumnInterval.pingRange());
         });
      });
      storedPingRangeSet.forEach(pingRange -> getRegionManager().isolate(pingRange, false));
      getRegionManager().getSchoolManager().removeAllUndoEdits();

      int echogramObjectNumber = getLSSS().getSurveyManager().getWorkData().nextObjectNumber();

      // Do everything leading to modified files before saving.

      if (getLSSS().getSurveyManager().isOpen()) {
         getLSSS().getSurveyManager().save();
      }

      storing = true;
      try {
         return batchStore(storeInputs, echogramObjectNumber);
      } finally {
         storing = false;
      }
   }

   boolean isStoring() {
      return storing;
   }

   private Stream<GridColumnInterval> findGridColumnIntervals() {
      GridConf gridConf = getConfigurationManager().getGridConf();
      PingRange pingRange = getInterpretationSettings().getPingRange();
      return new GridIndexFinder(getInterpretationSettings().getDataFileSet(), pingRange,
            gridConf.horizontalGridSize.getDoubleValue(), gridConf.horizontalGridUnit.getValue()).gridColumnIntervals();
   }

   private Stream<StoreInput> findNewStoreInputs() {
      return findAllStoreInputs()
            .filter(Predicate.not(StoreInput::saved));
   }

   Stream<StoreInput> findSavedStoreInputs() {
      return findAllStoreInputs()
            .filter(StoreInput::saved);
   }

   private Stream<StoreInput> findAllStoreInputs() {
      StoringIntervalConfig newIntervalConfig = createIntervalConfig();

      return findGridColumnIntervals()
            .filter(gridColumnInterval -> storableGridColumnIntervals(gridColumnInterval).findFirst().isPresent())
            .map(gridColumnInterval -> {
               StoringIntervalConfig intervalConfig = getRegionManager().getStoringConfigManager().get(gridColumnInterval.pingRange().begin());
               boolean saved = intervalConfig != null;
               if (intervalConfig == null) {
                  intervalConfig = newIntervalConfig;
               }
               return new StoreInput(gridColumnInterval, intervalConfig, saved);
            });
   }

   private StoringIntervalConfig createIntervalConfig() {
      return new StoringIntervalConfig(
            ImmutableSortedSet.copyOf(frequencies.getValue()),
            switch (getLSSS().getDataSetManager().getSelectedDataType()) {
               case RAW -> StoringIntervalConfig.DATA_DIR_RAW;
               case PROCESSED -> StoringIntervalConfig.DATA_DIR_KORONA;
            },
            getConfigurationManager().getSurveyMiscConf().pelagicMode.getBooleanValue(),
            quality.getValue().value);
   }

   private boolean batchStore(List<StoreInput> storeInputs, int echogramObjectNumber) {
      int totalPingCount = storeInputs.stream()
            .mapToInt(storeInput -> storeInput.gridColumnInterval.pingRange().getPingCount())
            .sum();
      ProgressView progressView = new ProgressView("Storing to database", totalPingCount)
            .mainProgressAsPercentage()
            .showRemainingTime();
      WorkerDialog.Result result = new WorkerDialog(getLSSS().getFrame(), progressView.getComponent())
            .start(asyncHandle -> {
               getConfigurationManager().getSurveyMiscConf().getIcesConf().saveToDatabase();
               GridParameters gridParameters = new GridParameters(this);
               Queue<StoreInput> storeInputsQueue = new ArrayDeque<>(storeInputs);
               Queue<Future<StoreResult>> futures = new ArrayDeque<>();
               while (true) {
                  while (getLSSS().getDatabaseManager().getDatabaseConnection().getWaitingCount() >= DatabaseConnection.MAX_WAITING_COUNT && !asyncHandle.isCancelled()) {
                     asyncHandle.sleep(10);
                  }

                  while (!storeInputsQueue.isEmpty() && futures.size() < Runtime.getRuntime().availableProcessors()) {
                     StoreInput storeInput = storeInputsQueue.remove();
                     futures.add(Exec.CACHED_THREAD_POOL.submit(() -> {
                        GridParameters.PerInterval perInterval = new GridParameters.PerInterval(gridParameters, storeInput);
                        DataFileSet dataFileSet = perInterval.dataFileSet;
                        List<Ping> pings = dataFileSet.getPingIndexStream(storeInput.gridColumnInterval.pingRange())
                              .map(dataFileSet::getPing)
                              .toList();
                        convertConditionalMaskingToEraser(pings, perInterval.channels);
                        GridIntegrator gridIntegrator = new GridIntegrator(perInterval);
                        gridIntegrator.integrate(pings, echogramObjectNumber, asyncHandle);
                        GridIntegratorResult gridIntegratorResult = gridIntegrator.createScatters();
                        return new StoreResult(storeInput, gridIntegratorResult);
                     }));
                  }

                  if (futures.isEmpty()) {
                     return;
                  }
                  StoreResult storeResult = futures.remove().get();
                  if (asyncHandle.isCancelled()) {
                     return;
                  }
                  interpretationSummary.store(storeResult.gridIntegratorResult);
                  progressView.addMainProgress(storeResult.storeInput.gridColumnInterval.pingRange().getPingCount(), "");
               }
            });
      return result.success();
   }

   record StoreInput(
         GridColumnInterval gridColumnInterval,
         StoringIntervalConfig intervalConfig,
         boolean saved
   ) {
   }

   private record StoreResult(StoreInput storeInput, GridIntegratorResult gridIntegratorResult) {
   }

   private void convertConditionalMaskingToEraser(List<Ping> pings, Set<Integer> channels) {
      ConditionalPingMask conditionalPingMask = getRegionManager().getConditionalPingMask();
      if (conditionalPingMask.isEmpty()) {
         return;
      }
      Map<PingIndex, FloatRangeSet> mask = new HashMap<>();
      for (Ping ping : pings) {
         FloatRangeSet pingMask = conditionalPingMask.getMask(ping);
         if (!pingMask.isEmpty()) {
            mask.put(ping.getPingIndex(), pingMask);
         }
      }
      if (!mask.isEmpty()) {
         getRegionManager().getMaskingManager().mask(mask, channels);
      }
   }

   private InterpretationSummary.ScatterSet getVisibleScatterSet() {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      return interpretationSummary.getScatterSet(pingRange);
   }

   boolean canDeleteFromDatabase() {
      return !getVisibleScatterSet().isEmpty();
   }

   public void delete(DeleteAction deleteAction) {
      switch (deleteAction) {
         case AUTO -> {
            if (!deleteFromDatabase()) {
               deleteStoringSettings();
            }
         }
         case KEEP_STORING_SETTINGS -> {
            deleteFromDatabase();
         }
         case ALL -> {
            deleteFromDatabase();
            deleteStoringSettings();
         }
      }
   }

   private boolean deleteFromDatabase() {
      InterpretationSummary.ScatterSet scatterSet = getVisibleScatterSet();
      if (scatterSet.isEmpty()) {
         return false;
      }
      RangeSet<PingIndex> storedPingsBefore = interpretationSummary.getStoredPings();
      interpretationSummary.delete(scatterSet);
      RangeSet<PingIndex> storedPingsAfter = interpretationSummary.getStoredPings();
      RangeSet<PingIndex> storedPingsDifference = new ArrayRangeSet<>();
      storedPingsDifference.addAll(storedPingsBefore);
      storedPingsDifference.removeAll(storedPingsAfter);

      storedPingsDifference.forEach(pingRange -> {
         PingIndex beginPingIndex = pingRange.begin();
         PingIndex endPingIndex = pingRange.end();
         // if there are stored scatters to the left of beginIndex, insert a vertical boundary
         PingIndex beforeBeginIndex = getInterpretationSettings().getDataFileSet().getPingIndexOrNullExcludingEnd(beginPingIndex.getPingNumber() - 1);
         if (beforeBeginIndex != null && interpretationSummary.getStoredPings().contains(beforeBeginIndex)) {
            getRegionManager().addVerticalDivider(beginPingIndex, false);
         }
         // if there are stored scatters to the right of endIndex, insert a vertical boundary
         if (interpretationSummary.getStoredPings().contains(endPingIndex)) {
            getRegionManager().addVerticalDivider(endPingIndex, false);
         }
      });

      return true;
   }

   private void deleteStoringSettings() {
      findGridColumnIntervals().forEach(gridColumnInterval -> {
         getRegionManager().getStoringConfigManager().remove(gridColumnInterval.pingRange());
      });
      if (getLSSS().getSurveyManager().isOpen()) {
         getLSSS().getSurveyManager().save();
      }
   }

   private void inheritInterpretation(int sourceChannel, int destinationChannel) {
      if (sourceChannel == -1) {
         return;
      }
      for (Region region : getRegionManager().getVisibleRegions()) {
         region.getInterpretation().inheritInterpretation(sourceChannel, destinationChannel);
      }
      for (Region region : getSelectedRegions()) {
         region.getInterpretation().inheritInterpretation(sourceChannel, destinationChannel);
      }
   }

   void inheritVisibleInterpretation(Collection<Integer> channelsToInitialize) {
      int currentChannel = getInterpretationSettings().getChannel();

      Multimap<Region, Integer> uninitializedRegions = getUninitializedRegions(channelsToInitialize);
      for (Map.Entry<Region, Collection<Integer>> entry : uninitializedRegions.asMap().entrySet()) {
         Region region = entry.getKey();

         if (!region.getInterpretation().hasAtLeastOneInitializedChannelInterpretation()) {
            region.getInterpretation().getChannelInterpretation(currentChannel).setInitialized(true);
         }

         region.getInterpretation().inheritInterpretation(currentChannel, currentChannel);
         assert region.getInterpretation().getChannelInterpretation(currentChannel).isInitialized();

         for (int channel : entry.getValue()) {
            region.getInterpretation().inheritInterpretation(currentChannel, channel);
            assert region.getInterpretation().getChannelInterpretation(channel).isInitialized();
         }
      }
   }

   Multimap<Region, Integer> getUninitializedRegions(Collection<Integer> channelsToInitialize) {
      Multimap<Region, Integer> uninitializedRegions = HashMultimap.create();
      for (Region region : getRegionManager().getVisibleRegions()) {
         for (Integer channel : channelsToInitialize) {
            ChannelInterpretation channelInterpretation = region.getInterpretation().getChannelInterpretation(channel);
            if (!channelInterpretation.isInitialized()) {
               uninitializedRegions.put(region, channel);
            }
         }
      }
      return uninitializedRegions;
   }

   public Set<Integer> getChannelsToStore() {
      return kHzsToChannels(getInterpretationSettings().getDataFileSet(), frequencies.getValue());
   }

   static Set<Integer> kHzsToChannels(DataFileSet dataFileSet, Collection<Integer> kHzs) {
      Set<Integer> channels = new HashSet<>();
      List<RawFileTransducer> transducers = dataFileSet.getRawFileConfiguration().getTransducers();
      for (int channelIndex = 0; channelIndex < transducers.size(); channelIndex++) {
         RawFileTransducer transducer = transducers.get(channelIndex);
         if (kHzs.contains(transducer.getKHz())) {
            channels.add(channelIndex + 1);
         }
      }
      return channels;
   }

   Stream<GridColumnInterval> storableGridColumnIntervals(GridColumnInterval gridColumnInterval) {
      RangeSet<PingIndex> exclusions = getRegionManager().getExclusionManager().getExclusions();
      Stream<GridColumnInterval> gridColumnIntervals;
      if (exclusions.containsAny(gridColumnInterval.pingRange())) {
         RangeSet<PingIndex> subPingRanges = new ArrayRangeSet<>();
         subPingRanges.add(gridColumnInterval.pingRange());
         exclusions.forEach(subPingRanges::remove);
         GridConf gridConf = getConfigurationManager().getGridConf();
         PingMapping pingMapping = gridConf.horizontalGridUnit.getValue();
         double minHorizontalGridSize = gridConf.minHorizontalGridSize.getDoubleValue();
         gridColumnIntervals = subPingRanges.stream()
               .map(subPingRange -> {
                  DoubleRange subValueRange = DoubleRange.of(
                        gridColumnInterval.valueRange().clamp(pingMapping.valueOf(subPingRange.begin())),
                        gridColumnInterval.valueRange().clamp(pingMapping.valueOf(subPingRange.end())));
                  return new GridColumnInterval(subValueRange.getSize(), subValueRange, PingRange.of(subPingRange));
               })
               .filter(interval -> interval.horizontalSize() >= minHorizontalGridSize);
      } else {
         gridColumnIntervals = Stream.of(gridColumnInterval);
      }
      return gridColumnIntervals
            .filter(interval -> interpretationSummary.getStoredPings().containsNone(interval.pingRange())
                  && getInterpretationSettings().getDataFileSet().getMissingPings().containsNone(interval.pingRange())
            );
   }

   float getTotalSa(boolean bubbleCorrected) {
      float totalSa = regionIntegrationModule.get().getTotalSa(IntegrationArea.TOTAL);
      if (bubbleCorrected) {
         totalSa *= getRegionManager().getBubbleCorrectionManager().getBubbleCorrection(getInterpretationSettings().getPingRange());
      }
      return totalSa;
   }

   float getRegionSa(boolean bubbleCorrected) {
      float regionSa = regionIntegrationModule.get().getSa(IntegrationArea.TOTAL);
      if (bubbleCorrected) {
         regionSa *= getRegionManager().getBubbleCorrectionManager().getBubbleCorrection(getInterpretationSettings().getPingRange());
      }
      return regionSa;
   }

   float getRegionSa(ConditionalPingMask excludeMask, int channel, boolean bubbleCorrected) {
      float regionSa = regionIntegrationModule.get().calculateSa(getSelectedRegions(), channel, excludeMask);
      if (bubbleCorrected) {
         regionSa *= getRegionManager().getBubbleCorrectionManager().getBubbleCorrection(getInterpretationSettings().getPingRange());
      }
      return regionSa;
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private void updateStoreDelete() {
      canStoreSaved = findSavedStoreInputs().findFirst().isPresent();
      canStoreNew = findNewStoreInputs().findFirst().isPresent();

      viewHolder.ifView(InterpretationModuleView::updateStoreButtons);
   }

   private void selectedRegionsChanged() {
      for (Region region : getSelectedRegions()) {
         region.getInterpretation().inheritInterpretation(getInterpretationSettings().getChannel(), getInterpretationSettings().getChannel());
      }

      viewHolder.ifView(view -> {
         view.updateAcousticCategoryButtons();
         view.interpretationChanged();
      });
   }

   @Override
   public PojoData getPojoData() {
      return GuiUtils.getNowOrWait(() -> {
         PojoData.Builder builder = PojoData.newBuilder(getPersistentName());
         viewHolder.getView().addPojoData(builder);
         return builder.build();
      });
   }

   public enum StoreAction {
      AUTO, ONLY_PREVIOUSLY_STORED, ALL
   }

   public enum DeleteAction {
      AUTO, KEEP_STORING_SETTINGS, ALL
   }
}
