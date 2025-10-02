package no.imr.lsss.framework;

import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFilterModuleConfig;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.PingLoadingStrategy;
import no.imr.korona.data.datamanager.PingSampler;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.region.Region;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.korona.viewer.variables.PerPingSettings;
import no.imr.korona.viewer.variables.raw.RelativeFrequencyResponseVariable;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.survey.GridConf;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.concurrent.ExecutorObservation;
import no.imr.tools.concurrent.ObservingExecutor;
import no.imr.tools.concurrent.SerialExecutor;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.ListenableProperty;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.Listeners;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.range.DoubleRange;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.SwingDelayer;
import no.imr.tools.swing.WorkerDialog;
import no.marec.lsss.api.util.observing.Subscription;
import org.jspecify.annotations.Nullable;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public final class InterpretationSettings {
   private final LSSS lsss;

   private final NavigationHistory navigationHistory;

   private boolean interactiveMode;

   private final ArgChangeManager<DataFileSet> dataFileChangeManager = new ArgChangeManager<>();
   private final ListenableProperty<Boolean> workFilesLoading = new ListenableProperty<>(false);
   private final ChangeManager reloadChangeManager = new ChangeManager();
   private final ChangeManager cancelChangeManager = new ChangeManager();
   private volatile boolean cancelled;

   private PingMapping pingMapping = PingMapping.DISTANCE;
   private final ArgChangeManager<PingMapping> pingMappingChangeManager = new ArgChangeManager<>();

   private DataLoadingMode dataLoadingMode = DataLoadingMode.BROWSE;
   private final ArgChangeManager<DataLoadingMode> dataLoadingModeChangeManager = new ArgChangeManager<>();

   private DoubleRange valueRange = DoubleRange.of(0, 0);
   private final InterpretationEchogramPingSettings pingSettings = new InterpretationEchogramPingSettings(this);
   private final ArgChangeManager<PingRange> pingRangeChangeManager = new ArgChangeManager<>();

   private final InterpretationZSettings.Pelagic pelagicZSettings;
   private final InterpretationZSettings.Bottom bottomZSettings;

   private final EchogramSettings echogramSettings = new EchogramSettings();
   private final MapSettings mapSettings;

   private final PingSampler pingSampler;
   private int sampledPingCount;

   private final InterpretationEchogramPingSettings zoomPingSettings = new InterpretationEchogramPingSettings(this);
   private final PingSampler zoomRangePingSampler;

   private final InterpretationEchogramPingSettings nextPingSettings = new InterpretationEchogramPingSettings(this);
   private final PingSampler nextSegmentPingSampler;

   private final InterpretationEchogramPingSettings otherPingSettings = new InterpretationEchogramPingSettings(this);
   private final PingSampler otherDataPingSampler;

   private int channel = 1;
   private float lastFrequency;
   private final ArgChangeManager<Integer> channelChangeManager = new ArgChangeManager<>();

   private final ArgChangeManager<Event> eventChangeManager = new ArgChangeManager<>();

   private final Mouseover mouseover = new Mouseover(this);

   private final ColorConverterContainer colorConverterContainer = new ColorConverterContainer();

   private final ExecutorObservation executorObservation = new ExecutorObservation();
   private final ObservingExecutor observingExecutor = new ObservingExecutor(Exec.FORK_JOIN_POOL, executorObservation);

   private boolean skipThresholdUpdates;

   private BroadbandNotchFilterModuleConfig broadbandNotchFilterModuleConfig = new BroadbandNotchFilterModuleConfig();
   private final ArgChangeManager<BroadbandNotchFilterModuleConfig> broadbandNotchFilterModuleConfigArgChangeManager = new ArgChangeManager<>();

   public InterpretationSettings(LSSS lsss) {
      this.lsss = lsss;

      interactiveMode = lsss.getLsssConfig().visible;

      navigationHistory = new NavigationHistory(this, lsss.getPackageManager().lsssPackage);
      pingSampler = new PingSampler(lsss.getDataManager(), pingSettings);
      nextSegmentPingSampler = new PingSampler(lsss.getDataManager(), nextPingSettings);
      zoomRangePingSampler = new PingSampler(lsss.getDataManager(), zoomPingSettings);
      otherDataPingSampler = new PingSampler(lsss.getDataSetManager().getOtherDataManager(), otherPingSettings);

      pelagicZSettings = new InterpretationZSettings.Pelagic(this, lsss.getDataManager());
      bottomZSettings = new InterpretationZSettings.Bottom(this, lsss.getDataManager());

      mapSettings = new MapSettings(lsss);
   }

   public void setup() {
      lsss.getConfigurationManager().getSurveyMiscConf().mainFrequency.addListenerAndNotify(frequency -> {
         RelativeFrequencyResponseVariable variable = colorConverterContainer.getContinuousVariable(RelativeFrequencyResponseVariable.class);
         variable.setReferenceKHz(Utils.hzToKHz(frequency));
      });
      colorConverterContainer.getSV().getSettings().setPerPingSettings(new PerPingSettings() {
         @Override
         public FloatRange getClipRange(PingIndex pingIndex) {
            return lsss.getRegionManager().getThresholdManager().getLogSvRange(pingIndex);
         }

         @Override
         public NavigableSet<Float> getLowerThresholds() {
            return lsss.getRegionManager().getThresholdManager().getLowerThresholds(getPingRange());
         }

         @Override
         public NavigableSet<Float> getUpperThresholds() {
            return lsss.getRegionManager().getThresholdManager().getUpperThresholds(getPingRange());
         }

         @Override
         public boolean varyingClipAbove() {
            return lsss.getRegionManager().getThresholdManager().getUpperThresholdActive(getPingRange()).size() > 1;
         }
      });
      Listener updateSvSettingsListener = () -> {
         PingRange pingRange = getPingRange();
         if (pingRange.isEmpty()) {
            colorConverterContainer.getSV().getSettings().setRange(lsss.getRegionManager().getRegionConfiguration().getDefaultThresholds());
            return;
         }

         try {
            skipThresholdUpdates = true;
            ContinuousVariableSettings svSettings = colorConverterContainer.getSV().getSettings();
            svSettings.setMin(lsss.getRegionManager().getThresholdManager().getMinWritableLowerThreshold(pingRange));
            svSettings.setMax(lsss.getRegionManager().getThresholdManager().getMaxInteractiveUpperThreshold(pingRange));
            svSettings.getChangeManager().notifyListeners();
         } finally {
            skipThresholdUpdates = false;
         }
      };
      updateSvSettingsListener.addTo(
            pingRangeChangeManager,
            lsss.getSurveyManager().getChangeManager(),
            lsss.getRegionManager().getThresholdManager().getChangeManager()
      );
      colorConverterContainer.getSV().getSettings().getChangeManager().addListener(new Listener() {
         private boolean previousClip;
         private float previousMin;
         private float previousMax;

         {
            updatePreviousSettings();
         }

         @Override
         public void listen() {
            if (!skipThresholdUpdates) {
               ContinuousVariableSettings settings = colorConverterContainer.getSV().getSettings();
               Boolean clip = previousClip != settings.isClipAbove() ? settings.isClipAbove() : null;
               Float min = previousMin != settings.getRange().min() ? settings.getRange().min() : null;
               Float max = previousMax != settings.getRange().max() ? settings.getRange().max() : null;
               lsss.getRegionManager().getThresholdManager().set(getPingRange(), clip, min, max);
            }
            updatePreviousSettings();
         }

         private void updatePreviousSettings() {
            ContinuousVariableSettings settings = colorConverterContainer.getSV().getSettings();
            previousClip = settings.isClipAbove();
            previousMin = settings.getRange().min();
            previousMax = settings.getRange().max();
         }
      });

      lsss.getSurveyManager().getChangeManager().addListener(() -> {
         if (lsss.getSurveyManager().isOpen()) {
            lastFrequency = 0;
         }
      });

      Listener.of(this::requestPings).addTo(
            lsss.getConfigurationManager().getAppMiscConf().preload,
            lsss.getConfigurationManager().getAppMiscConf().preloadPreprocessed
      );

      mapSettings.setup(lsss);
   }

   public void close() {
      cancelAll();
   }

   public NavigationHistory getNavigationHistory() {
      return navigationHistory;
   }

   public boolean isInteractiveMode() {
      return interactiveMode;
   }

   public void setInteractiveMode(boolean interactiveMode) {
      if (!lsss.getLsssConfig().visible) {
         return;
      }
      this.interactiveMode = interactiveMode;
   }

   public void doNonInteractively(Runnable task) {
      boolean savedInteractiveMode = interactiveMode;
      try {
         setInteractiveMode(false);
         task.run();
      } finally {
         setInteractiveMode(savedInteractiveMode);
      }
   }

   public EchogramPingSettings getPingSettings() {
      return pingSettings;
   }

   public InterpretationZSettings.Pelagic getPelagicZSettings() {
      return pelagicZSettings;
   }

   public InterpretationZSettings.Bottom getBottomZSettings() {
      return bottomZSettings;
   }

   public EchogramSettings getEchogramSettings() {
      return echogramSettings;
   }

   public MapSettings getMapSettings() {
      return mapSettings;
   }

   public PingSampler getPingSampler() {
      return pingSampler;
   }

   public int getChannel() {
      return channel;
   }

   public float getFrequency() {
      return getDataFileSet().getFrequency(channel);
   }

   public void setChannel(int channel) {
      channel = Math.max(1, channel);
      if (this.channel == channel) {
         return;
      }
      this.channel = channel;
      float frequency = getDataFileSet().getFrequency(channel);
      if (frequency > 0) {
         lastFrequency = frequency;
      }
      channelChangeManager.notifyListeners(channel);
   }

   public ArgChangeManager<Integer> getChannelChangeManager() {
      return channelChangeManager;
   }

   public void shiftChannel(int shift) {
      int n = getDataFileSet().getRawFileConfiguration().getTransducerCount();
      int channelIndex = channel - 1;
      channelIndex = Utils.mod(channelIndex + shift, n);
      setChannel(channelIndex + 1);
   }

   public ColorConverterContainer getColorConverterContainer() {
      return colorConverterContainer;
   }

   public DataLoadingMode getDataLoadingMode() {
      return dataLoadingMode;
   }

   public void setDataLoadingMode(DataLoadingMode mode) {
      if (mode == dataLoadingMode) {
         return;
      }
      if (interactiveMode
            && mode == DataLoadingMode.DETAIL
            && getPingRange().getPingCount() > lsss.getConfigurationManager().getAppMiscConf().maxPings.getIntValue()) {
         int answer = GuiUtils.getNowOrWait(() -> {
            return JOptionPane.showConfirmDialog(lsss.getFrame(),
                  getPingRange().getPingCount() + " pings will be loaded.\nSwitch to DETAIL mode?",
                  "Confirm DETAIL mode",
                  JOptionPane.YES_NO_OPTION);
         });
         if (answer != JOptionPane.YES_OPTION) {
            return;
         }
      }

      cancelPingRequests();
      dataLoadingMode = mode;
      requestPings();
      dataLoadingModeChangeManager.notifyListeners(mode);
   }

   public ArgChangeManager<DataLoadingMode> getDataLoadingModeChangeManager() {
      return dataLoadingModeChangeManager;
   }

   public PingMapping getPingMapping() {
      return pingMapping;
   }

   public void setPingMapping(PingMapping pingMapping) {
      if (this.pingMapping == pingMapping) {
         return;
      }
      cancelPingRequests();
      this.pingMapping = pingMapping;
      valueRange = pingMapping.toValueRange(getPingRange());
      pingSettings.update();
      requestPings();
      pingMappingChangeManager.notifyListeners(pingMapping);
   }

   public ArgChangeManager<PingMapping> getPingMappingChangeManager() {
      return pingMappingChangeManager;
   }

   public DataFileSet getDataFileSet() {
      return lsss.getDataManager().getDataFileSet();
   }

   public ArgChangeManager<DataFileSet> getDataFileChangeManager() {
      return dataFileChangeManager;
   }

   public ListenableProperty<Boolean> getWorkFilesLoading() {
      return workFilesLoading;
   }

   public ChangeManager getReloadChangeManager() {
      return reloadChangeManager;
   }

   public ChangeManager getCancelChangeManager() {
      return cancelChangeManager;
   }

   public boolean isCancelled() {
      return cancelled;
   }

   public void dataFilesAboutToChange() {
      mouseover.setFrozen(false);
      mouseover.setPos();
      cancelAll();
   }

   /**
    * Should be called when the set of data files has changed.
    * Resets navigation history and interpretation.
    */
   public void dataFilesChanged() {
      lsss.getRegionManager().setupDefaultBoundaries();
      pelagicZSettings.reset();
      bottomZSettings.reset(lsss.getConfigurationManager().getGridConf());
      mapSettings.setBoundingBox(this, getDataFileSet());
      mapSettings.getExtendedSurveyLine().reset();
      dataFileChangeManager.notifyListeners(getDataFileSet());
      float frequency = lastFrequency > 0 ? lastFrequency : lsss.getConfigurationManager().getSurveyMiscConf().mainFrequency.getFloatValue();
      setChannel(getDataFileSet().firstChannelClosestTo(frequency, Float.POSITIVE_INFINITY));
   }

   public void resetNavigation() {
      setPingRange(getDataFileSet().getTotalRange());
      navigationHistory.reset();
      if (interactiveMode && !getPingRange().isEmpty()) {
         maybeChangePingMapping();
         maybeChangePelagicMode();
      }
   }

   private void maybeChangePingMapping() {
      if (valueRange.isEmpty() && pingMapping == PingMapping.DISTANCE) {
         int answer = GuiUtils.getNowOrWait(() -> {
            return JOptionPane.showConfirmDialog(lsss.getFrame(),
                  "Distance is degenerated.\nSwitch to ping number as x-axis in echogram?",
                  "Change ping mapping",
                  JOptionPane.YES_NO_OPTION);
         });
         if (answer == JOptionPane.YES_OPTION) {
            setPingMapping(PingMapping.NUMBER);
         }
      }
   }

   private void maybeChangePelagicMode() {
      BooleanParameter pelagicMode = lsss.getConfigurationManager().getSurveyMiscConf().pelagicMode;
      if (pelagicMode.getBooleanValue()) {
         return;
      }
      double maxDepth = getDataFileSet().getDataFiles().stream()
            .flatMap(dataFile -> dataFile.getBot0Datagrams().stream())
            .flatMapToDouble(bot0Datagram -> Arrays.stream(bot0Datagram.getChannelDepths()))
            .max()
            .orElse(0);
      if (maxDepth == 0) {
         int answer = GuiUtils.getNowOrWait(() -> {
            return JOptionPane.showConfirmDialog(lsss.getFrame(),
                  "All bottom depths are zero.\nSwitch to pelagic mode?",
                  "Switch to pelagic mode?",
                  JOptionPane.YES_NO_OPTION);
         });
         if (answer == JOptionPane.YES_OPTION) {
            pelagicMode.setBooleanValue(true);
         }
      }
   }

   /**
    * Should be called when the set of data files has changed.
    * Preserves navigation history and interpretation.
    */
   public void dataFilesUpdated() {
      dataFileChangeManager.notifyListeners(getDataFileSet());
   }

   public PingRange getPingRange() {
      return pingSettings.getPingRange();
   }

   public DoubleRange getValueRange() {
      return valueRange;
   }

   public void setValueRange(DoubleRange range) {
      setRange(valueRangeToNonEmptyPingRange(range), range);
   }

   public void setPingRange(PingRange pingRange) {
      pingRange = nonEmptyPingRange(pingRange);
      setRange(pingRange, pingMapping.toValueRange(pingRange));
   }

   private void setEmptyPingRange() {
      setRange(PingRange.EMPTY_RANGE, DoubleRange.of(0, 0));
   }

   private void setRange(PingRange pingRange, DoubleRange range) {
      DoubleRange totalValueRange = pingMapping.toValueRange(getDataFileSet().getTotalRange());
      if (!totalValueRange.isEmpty() && !range.isEmpty() && !totalValueRange.intersects(range)) {
         return;
      }
      range = nonDegeneratedValueRange(totalValueRange.clamp(range));
      if (getPingRange().equals(pingRange) && valueRange.equals(range)) {
         return;
      }
      if (interactiveMode
            && dataLoadingMode == DataLoadingMode.DETAIL
            && pingRange.getPingCount() > getPingRange().getPingCount()
            && pingRange.getPingCount() > lsss.getConfigurationManager().getAppMiscConf().maxPings.getIntValue()) {
         int answer = GuiUtils.getNowOrWait(() -> {
            return JOptionPane.showConfirmDialog(lsss.getFrame(),
                  "About to load " + pingRange.getPingCount() + " pings.\nSwitch to BROWSE mode?",
                  "Switch to BROWSE mode?",
                  JOptionPane.YES_NO_OPTION);
         });
         if (answer == JOptionPane.YES_OPTION) {
            setDataLoadingMode(DataLoadingMode.BROWSE);
         }
      }

      cancelPingRequests();
      zoomPingSettings.setPingRange(PingRange.EMPTY_RANGE);
      pingSettings.setPingRange(pingRange);
      valueRange = range;
      navigationHistory.addCheckPoint();
      requestPings();
      pingRangeChangeManager.notifyListeners(pingRange);
   }

   public ArgChangeManager<PingRange> getPingRangeChangeManager() {
      return pingRangeChangeManager;
   }

   private PingRange valueRangeToPingRange(DoubleRange range) {
      PingIndex begin = getDataFileSet().getContainingPingIndex(range.begin(), pingMapping);
      if (begin == null) {
         if (getDataFileSet().getTotalRange().isEmpty()) {
            return PingRange.EMPTY_RANGE;
         }
         begin = getDataFileSet().getClosestPingIndex(range.begin(), pingMapping);
      }

      PingIndex end = getDataFileSet().getContainingPingIndex(range.end(), pingMapping);
      if (end == null) {
         end = getDataFileSet().getClosestPingIndex(range.end(), pingMapping);
      }

      return PingRange.of(begin, end);
   }

   private PingRange valueRangeToNonEmptyPingRange(DoubleRange range) {
      return nonEmptyPingRange(valueRangeToPingRange(range));
   }

   private PingRange nonEmptyPingRange(PingRange pingRange) {
      if (!pingRange.isEmpty()) {
         return pingRange;
      }
      PingIndex a = pingRange.begin();
      PingIndex b = pingRange.end();
      if (a.getPingNumber() == getDataFileSet().getTotalRange().end().getPingNumber()) {
         a = getDataFileSet().previousOrNull(b);
         if (a == null) {
            return pingRange;
         }
      } else {
         b = getDataFileSet().nextOrNull(a);
         if (b == null) {
            return pingRange;
         }
      }
      return PingRange.of(a, b);
   }

   /**
    * Goes to the PingRange before the current one.
    */
   public void gotoPreviousPingRange() {
      setValueRange(nextValueRange(-1));
   }

   /**
    * Goes to the PingRange after the current one.
    */
   public void gotoNextPingRange() {
      setValueRange(nextValueRange(1));
   }

   /**
    * Goes to the ping range of a preferred size and alignment closest to the current range.
    */
   public void gotoPreferredSize(double size) {
      GridConf gridConf = lsss.getConfigurationManager().getGridConf();
      PingMapping preferredPingMapping = gridConf.horizontalGridUnit.getValue();
      DoubleRange totalValueRange = preferredPingMapping.toValueRange(getDataFileSet().getTotalRange());
      DoubleRange range = DataUtils.convertPingMappingRange(getDataFileSet(), pingMapping, valueRange, preferredPingMapping);
      long index = (long) Math.floor(range.begin() / size);
      if (index * size < totalValueRange.begin() && (index + 2) * size <= totalValueRange.end()) {
         // This beginning of this interval is small && The end of the next interval is not too big => Use next interval
         index++;
      } else if ((index + 1) * size > totalValueRange.end() && (index - 1) * size >= totalValueRange.begin()) {
         // This end of this interval is too big && The beginning of the previous interval is not too small => Use previous interval
         index--;
      }
      range = createValueRange(index, size);
      range = DataUtils.convertPingMappingRange(getDataFileSet(), preferredPingMapping, range, pingMapping);
      setValueRange(range);

      if (gridConf.automaticDetailMode.getBooleanValue()) {
         setDataLoadingMode(DataLoadingMode.DETAIL);
      }
   }

   private static DoubleRange createValueRange(long index, double size) {
      return DoubleRange.of(index * size, (index + 1) * size);
   }

   private DoubleRange nextValueRange(int direction) {
      if (valueRange.isEmpty()) {
         return valueRange;
      }
      Double gridInterval = getSnappedGridInterval();
      if (gridInterval != null) {
         PingMapping gridPingMapping = lsss.getConfigurationManager().getGridConf().horizontalGridUnit.getValue();
         DoubleRange gridValueRange = DataUtils.convertPingMappingRange(getDataFileSet(), pingMapping, valueRange, gridPingMapping);
         long index = Math.round(gridValueRange.begin() / gridInterval);
         DoubleRange nextGridValueRange = createValueRange(index + direction, gridInterval);
         return DataUtils.convertPingMappingRange(getDataFileSet(), gridPingMapping, nextGridValueRange, pingMapping);
      } else {
         return valueRange.shift(direction);
      }
   }

   private @Nullable Double getSnappedGridInterval() {
      GridConf gridConf = lsss.getConfigurationManager().getGridConf();
      List<Double> intervals = new ArrayList<>(gridConf.preferredHorizontalSizes.getValue());
      intervals.add(gridConf.horizontalGridSize.getDoubleValue());
      intervals.sort(Comparator.reverseOrder());

      DoubleRange range = DataUtils.convertPingMappingRange(getDataFileSet(), pingMapping, valueRange, gridConf.horizontalGridUnit.getValue());
      for (double interval : intervals) {
         if (range.begin() == Math.round(range.begin() / interval) * interval &&
               range.end() == Math.round(range.end() / interval) * interval) {
            return interval;
         }
      }

      return null;
   }

   void zoomHorizontally(double referenceValue, double zoomFactor) {
      double newSize = valueRange.getSize() / zoomFactor;
      double fraction = valueRange.valueToFraction(referenceValue);
      setValueRange(getZoomedValueRange(referenceValue, newSize, fraction));
   }

   private DoubleRange nonDegeneratedValueRange(DoubleRange range) {
      PingRange pingRange = valueRangeToPingRange(range);
      if (pingRange.isEmpty()) {
         range = pingMapping.toValueRange(nonEmptyPingRange(pingRange));
      }
      return range;
   }

   private static DoubleRange getZoomedValueRange(double referenceValue, double size, double leftFraction) {
      double begin = referenceValue - size * leftFraction;
      double end = referenceValue + size * (1 - leftFraction);
      return DoubleRange.of(begin, end);
   }

   public PingIndex getCenter() {
      return getDataFileSet().getClosestPingIndex(valueRange.getCenter(), pingMapping);
   }

   public void setCenter(PingIndex pingIndex) {
      shiftPingRange(pingIndex, 0.5);
   }

   public void setBegin(PingIndex pingIndex) {
      shiftPingRange(pingIndex, 0);
   }

   public void setEnd(PingIndex pingIndex) {
      shiftPingRange(pingIndex, 1);
   }

   private void shiftPingRange(PingIndex pingIndex, double leftFraction) {
      double value = pingMapping.valueOf(pingIndex);
      setValueRange(getZoomedValueRange(value, valueRange.getSize(), leftFraction));
   }

   public void shiftPingRange(double shift) {
      setValueRange(valueRange.shift(shift));
   }

   int getSampledPingCount() {
      return sampledPingCount;
   }

   public void setSampledPingCount(int count) {
      count = Math.max(2, count);
      if (sampledPingCount == count) {
         return;
      }
      cancelPingRequests();
      sampledPingCount = count;
      pingSettings.update();
      requestPings();
   }

   public void setZoomRange(PingRange zoomRange) {
      SwingDelayer.invokeLater(this, () -> {
         if (!zoomPingSettings.getPingRange().equals(zoomRange)) {
            cancelPingRequests();
            zoomPingSettings.setPingRange(zoomRange);
            requestPings();
         }
      });
   }

   public ObservingExecutor getObservingExecutor() {
      return observingExecutor;
   }

   public ExecutorObservation getExecutorObservation() {
      return executorObservation;
   }

   public SerialExecutor createObservingSerialExecutor() {
      return new SerialExecutor(observingExecutor);
   }

   public Listener createCoalescingListener(Runnable listener) {
      return Listeners.coalescingInExecutor(observingExecutor, listener);
   }

   public void waitUntilFinished() {
      new WorkerDialog(lsss.getFrame(), "Waiting for computations...")
            .startWithoutCancel(() -> {
               pingSampler.waitForPingRequest();

               CountDownLatch countDownLatch = new CountDownLatch(1);
               Listener countDownListener = () -> {
                  if (executorObservation.getCount() == 0) {
                     countDownLatch.countDown();
                  }
               };
               Subscription subscription = executorObservation.getChangeManager().subscribe(countDownListener);
               try {
                  countDownListener.listen();
                  countDownLatch.await();
               } finally {
                  subscription.unsubscribe();
               }
            });
   }

   public void cancelAll() {
      cancelled = true;
      getDataFileSet().discardLoadedData();
      setEmptyPingRange();
      cancelPingRequests();
      cancelChangeManager.notifyListeners();
      waitUntilFinished();
      cancelled = false;
   }

   private void cancelPingRequests() {
      zoomRangePingSampler.cancelPingRequest();
      pingSampler.cancelPingRequest();
      nextSegmentPingSampler.cancelPingRequest();
      otherDataPingSampler.cancelPingRequest();

      zoomRangePingSampler.waitForPingRequest();
      pingSampler.waitForPingRequest();
      nextSegmentPingSampler.waitForPingRequest();
      otherDataPingSampler.waitForPingRequest();
   }

   private void requestPings() {
      PingLoadingStrategy pingLoadingStrategy = lsss.getConfigurationManager().getAppMiscConf().pingLoading.getValue();

      zoomRangePingSampler.requestPings(pingLoadingStrategy);

      if (dataLoadingMode == DataLoadingMode.BROWSE) {
         pingSampler.setPingSettings(pingSettings);
      } else {
         pingSampler.setPingSettings(new DetailedModeEchogramPingSettings(this, getPingRange()));
      }
      pingSampler.requestPings(pingLoadingStrategy);

      if (lsss.getConfigurationManager().getAppMiscConf().preload.getBooleanValue()) {
         nextPingSettings.setPingRange(valueRangeToNonEmptyPingRange(nextValueRange(1)));
      } else {
         nextPingSettings.setPingRange(PingRange.EMPTY_RANGE);
      }
      nextSegmentPingSampler.requestPings(pingLoadingStrategy);

      if (lsss.getConfigurationManager().getAppMiscConf().preloadPreprocessed.getBooleanValue() &&
            !lsss.getDataSetManager().getOtherDataManager().getDataFileSet().isEmpty()) {
         otherPingSettings.setPingRange(getPingRange());
      } else {
         otherPingSettings.setPingRange(PingRange.EMPTY_RANGE);
      }
      otherDataPingSampler.requestPings(pingLoadingStrategy);
   }

   public void reloadData() {
      Set<Integer> selectedObjectNumbers = lsss.getRegionManager().getSelectedRegions().stream()
            .map(Region::getObjectNumber)
            .collect(Collectors.toSet());
      NavigationHistory.NavigationState currentState = navigationHistory.getCurrentState();
      lsss.getConfigurationManager().getDataConf().getAllDataConfs().forEach(dataConf -> {
         dataConf.checkForUpdates(true);
         dataConf.triggerReloadDataOnApply();
         dataConf.apply();
      });
      currentState.apply();
      lsss.getRegionManager().selectRegions(region -> selectedObjectNumbers.contains(region.getObjectNumber()));
   }

   public void reloadAllData() {
      reloadData();
      reloadChangeManager.notifyListeners();
   }

   public void recompute() {
      navigationHistory.doWithNoAddCheckPoint(() -> {
         PingRange pingRange = getPingRange();
         setEmptyPingRange();
         getDataFileSet().discardLoadedData(); // Necessary in case discarding zero bottom changes
         setPingRange(pingRange);
      });
   }

   public void sendEvent(String name, Object pojoValue) {
      eventChangeManager.notifyListeners(new Event(name, pojoValue));
   }

   public ArgChangeManager<Event> getEventChangeManager() {
      return eventChangeManager;
   }

   public Mouseover mouseover() {
      return mouseover;
   }

   public BroadbandNotchFilterModuleConfig getBroadbandNotchFilterModuleConfig() {
      return broadbandNotchFilterModuleConfig;
   }

   public void setBroadbandNotchFilterModuleConfig(BroadbandNotchFilterModuleConfig broadbandNotchFilterModuleConfig) {
      this.broadbandNotchFilterModuleConfig = broadbandNotchFilterModuleConfig;
      broadbandNotchFilterModuleConfigArgChangeManager.notifyListeners(broadbandNotchFilterModuleConfig);
   }

   public ArgChangeManager<BroadbandNotchFilterModuleConfig> getBroadbandNotchFilterModuleConfigArgChangeManager() {
      return broadbandNotchFilterModuleConfigArgChangeManager;
   }

   public record Event(String name, Object pojoValue) {
   }
}
