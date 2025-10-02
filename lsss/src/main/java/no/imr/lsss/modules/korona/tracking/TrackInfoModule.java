package no.imr.lsss.modules.korona.tracking;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import no.imr.korona.data.datagrams.TTC0Datagram;
import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.region.Region;
import no.imr.korona.region.School;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.BaseSystemWorkFileManager;
import no.imr.lsss.modules.BaseDataModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.korona.DataFilesCache;
import no.imr.lsss.modules.korona.DataObjectLoader;
import no.imr.lsss.modules.ts.BaseTsData;
import no.imr.lsss.modules.ts.BaseTsModule;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class TrackInfoModule extends BaseDataModule implements BaseTsModule {
   private ImmutableSet<TrackId> originalValidIds = ImmutableSet.of();
   private ImmutableSet<TrackId> validIds = ImmutableSet.of();
   private final ArgChangeManager<Set<TrackId>> validIdsChangeManager = new ArgChangeManager<>();
   private final LoadingCache<EchogramModule, EchogramTrackData> echogramTrackData = CacheBuilder.newBuilder()
         .build(new CacheLoader<>() {
            @Override
            public EchogramTrackData load(EchogramModule key) {
               return new EchogramTrackData(TrackInfoModule.this, key);
            }
         });

   private final Map<TrackId, TrackInfo> originalTrackInfos = new ConcurrentHashMap<>();
   private final Map<TrackId, TrackInfo> trackInfos = new ConcurrentHashMap<>();
   private final ArgChangeManager<Optional<TrackInfo>> trackInfoChangeManager = new ArgChangeManager<>();
   private final DataFilesCache<TrackInfo> trackInfoCache = new DataFilesCache<>();
   private DataObjectLoader<TrackInfo> trackInfoLoader;
   private final TrackEditing trackEditing = new TrackEditing(this);
   private final TrackLabelling trackLabelling = new TrackLabelling(this);
   private final TrackSelection trackSelection = new TrackSelection(this);

   private final ChangeManager tsDetectionChangeManager = new ChangeManager();

   public TrackInfoModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      trackInfoLoader = TrackInfoLoader.make(getInterpretationSettings().getExecutorObservation(), DataFileSet.empty(), trackInfoCache, Utils.emptyConsumer());
      BaseSystemWorkFileManager workFileManager = moduleInfo.plugin().getWorkFileManager();
      workFileManager.addWorkFileExtra(trackLabelling.getWorkFileExtra());
      workFileManager.addWorkFileExtra(trackEditing.getWorkFileExtra());

      validIdsChangeManager.addListener(tsDetectionChangeManager);
      trackSelection.getChangeManager().addListener(tsDetectionChangeManager);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(newCoalescingExecListener(this::recompute), List.of(
            getInterpretationSettings().getWorkFilesLoading(),
            getInterpretationSettings().getDataFileChangeManager()
      ));
      registry.add(getInterpretationSettings().getPingRangeChangeManager(), newCoalescingExecListener(this::setPriorityPingRange));
      registry.add(getInterpretationSettings().getCancelChangeManager(), newCoalescingExecListener(() -> trackInfoLoader.cancel()));
      registry.add(getRegionManager().getEchogramSelectionChangeManager(), trackSelection::doEchogramSelection);
      registry.add(getLSSS().getInterpretationSummary().getChangeManager(), newCoalescingExecListener(() -> trackEditing.getUndoManager().discardAllEdits()));

      //---

      setPriorityPingRange(getInterpretationSettings().getPingRange());
      recompute();
   }

   private void setPriorityPingRange(PingRange pingRange) {
      trackInfoLoader.setPriorityPingRange(pingRange);
   }

   public void waitForTrackInfoLoader(AsyncHandle asyncHandle) {
      while (!trackInfoLoader.getAsyncHandle().isFinished()) {
         asyncHandle.sleep(100);
      }
   }

   public Set<TrackId> getValidIds() {
      return validIds;
   }

   public ArgChangeManager<Set<TrackId>> getValidIdsChangeManager() {
      return validIdsChangeManager;
   }

   EchogramTrackData getEchogramTrackData(EchogramModule echogramModule) {
      return echogramTrackData.getUnchecked(echogramModule);
   }

   public Map<TrackId, TrackInfo> getTrackInfos() {
      return trackInfos;
   }

   public ArgChangeManager<Optional<TrackInfo>> getTrackInfoChangeManager() {
      return trackInfoChangeManager;
   }

   public TrackEditing getTrackEditing() {
      return trackEditing;
   }

   public TrackLabelling getTrackLabelling() {
      return trackLabelling;
   }

   public TrackSelection getTrackSelection() {
      return trackSelection;
   }

   private void recompute() {
      if (getInterpretationSettings().getWorkFilesLoading().getValue()) {
         return;
      }
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      originalValidIds = getValidTrackIds(dataFileSet);
      validIds = originalValidIds;
      loadTrackInfos();
      trackEditing.setDataFileSet(dataFileSet);
      updateFromEditing();
   }

   void updateFromEditing() {
      if (originalValidIds.isEmpty()) {
         return;
      }

      Set<TrackId> validTrackIds = new HashSet<>(originalValidIds);
      validTrackIds.removeAll(trackEditing.getReplacedTracks());
      validTrackIds.addAll(trackEditing.getNewTracks().keySet());
      validIds = ImmutableSet.copyOf(validTrackIds);

      trackInfos.clear();
      trackInfos.putAll(originalTrackInfos);
      trackInfos.keySet().removeAll(trackEditing.getReplacedTracks());
      trackInfos.putAll(trackEditing.getNewTracks());

      validIdsChangeManager.notifyListeners(validIds);
   }

   private ImmutableSet<TrackId> getValidTrackIds(DataFileSet dataFileSet) {
      ImmutableSet.Builder<TrackId> validTrackIds = ImmutableSet.builder();
      for (DataFile dataFile : dataFileSet.getDataFiles()) {
         if (getInterpretationSettings().isCancelled()) {
            return ImmutableSet.of();
         }
         Ping lastPing = dataFile.getLastPing();
         lastPing.getPingItems(TTC0Datagram.class)
               .flatMapToInt(ttc0Datagram -> Arrays.stream(ttc0Datagram.getValidIds()))
               .mapToObj(id -> new TrackId(dataFile, id))
               .forEach(validTrackIds::add);
      }
      return validTrackIds.build();
   }

   private void loadTrackInfos() {
      trackInfoLoader.cancel();
      originalTrackInfos.clear();
      trackInfos.clear();
      trackInfoChangeManager.notifyListeners(Optional.empty());
      if (getInterpretationSettings().isCancelled()) {
         return;
      }
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      trackInfoLoader = TrackInfoLoader.make(getInterpretationSettings().getExecutorObservation(), dataFileSet, trackInfoCache, trackInfo -> {
         TrackId trackId = trackInfo.trackId();
         originalTrackInfos.put(trackId, trackInfo);
         if (!trackEditing.getReplacedTracks().contains(trackId)) {
            trackInfos.put(trackId, trackInfo);
         }
         trackInfoChangeManager.notifyListeners(Optional.of(trackInfo));
      });
      trackInfoLoader.setPriorityPingRange(getInterpretationSettings().getPingRange());
   }

   @Override
   public ChangeManager getTSDetectionChangeManager() {
      return tsDetectionChangeManager;
   }

   @Override
   public Stream<NavigableMap<PingIndex, ? extends BaseTsData>> getSelectedTracks() {
      Set<PingIndex> requestedPingIndices = new HashSet<>(getInterpretationSettings().getPingSampler().getRequestedPingIndices());
      PingRange pingRange = getInterpretationSettings().getPingRange();
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      return trackSelection.getSelectedTrackIds().stream()
            .map(trackInfos::get)
            .filter(Objects::nonNull)
            .map(trackInfo -> {
               NavigableMap<PingIndex, BaseTsData> track = new TreeMap<>();
               dataFileSet.getPingIndexStream(trackInfo.pingRange().intersection(pingRange))
                     .filter(requestedPingIndices::contains)
                     .forEach(pingIndex -> {
                        Ping ping = dataFileSet.getPing(pingIndex);
                        trackEditing.getTrackBorders(ping, getInterpretationSettings().getChannel())
                              .filter(trackBorder -> trackBorder.trackId().equals(trackInfo.trackId()))
                              .filter(TrackBorder::useAngles)
                              .forEach(trackBorder -> track.put(pingIndex, trackBorder));
                     });
               return track;
            });
   }

   @Override
   public Map<PingIndex, ? extends Collection<? extends BaseTsData>> getTargetsForPoint(List<Region> regions, EchogramPoint echogramPoint, int channel) {
      List<TrackBorder> trackBorders = trackEditing.getTrackBorders(getInterpretationSettings().getDataFileSet().getPing(echogramPoint.pingIndex()), channel)
            .filter(trackBorder -> trackSelection.getSelectedTrackIds().contains(trackBorder.trackId()))
            .filter(TrackBorder::useAngles)
            .filter(trackBorder -> trackBorder.depthRange().contains(echogramPoint.depth()))
            .toList();
      return Map.of(echogramPoint.pingIndex(), trackBorders);
   }

   public void createSchoolsAndSelect(Stream<TrackId> trackIds) {
      List<School> schools = trackIds
            .filter(this::isTrackWritable)
            .map(this::createSchool)
            .filter(Objects::nonNull)
            .toList();
      getRegionManager().replaceSelectedRegions(schools);
   }

   private @Nullable School createSchool(TrackId trackId) {
      TrackInfo trackInfo = trackInfos.get(trackId);
      if (trackInfo == null) {
         return null;
      }
      NavigableMap<PingIndex, FloatRangeSet> mask = new TreeMap<>();
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      for (PingIndex pingIndex : dataFileSet.getPingIndices(trackInfo.pingRange())) {
         Ping ping = dataFileSet.getPing(pingIndex);
         List<FloatRange> depthRanges = trackEditing.getTrackBorders(ping, getInterpretationSettings().getChannel())
               .filter(trackBorder -> trackBorder.trackId().equals(trackId))
               .map(TrackBorder::depthRange)
               .toList();
         FloatRangeSet depthRangeSet = FloatRangeSet.of(depthRanges);
         mask.put(pingIndex, depthRangeSet);
      }
      return getRegionManager().addSchool(mask);
   }

   boolean isTrackReadOnly(TrackId trackId) {
      TrackInfo trackInfo = trackInfos.get(trackId);
      return trackInfo != null && getRegionManager().isReadOnly(trackInfo.pingRange());
   }

   boolean isTrackWritable(TrackId trackId) {
      return !isTrackReadOnly(trackId);
   }
}
