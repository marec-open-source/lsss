package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.EchogramRectangle;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.region.storing.StoringConfigManager;
import no.imr.tools.UnionList;
import no.imr.tools.Utils;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.ListenableProperty;
import no.imr.tools.misc.SelectionAction;
import no.imr.tools.misc.ThrowingRunnable;
import no.imr.tools.misc.ToFloatFunction;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeSet;
import no.imr.tools.range.RangeUtils;
import no.imr.tools.xml.XmlUtils;
import no.marec.lsss.api.util.GeoPoint;
import no.marec.lsss.api.util.observing.ObservableValue;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class RegionManager {
   private static final String XML_REGION_INTERPRETATION = "regionInterpretation";
   public static final String XML_TIME_RANGE = "timeRange";
   private static final String XML_START_TIME = "start";
   private static final String XML_NUMBER_OF_PINGS = "numberOfPings";

   private final RegionConfiguration regionConfiguration;
   private final LayerManager layerManager = new LayerManager(this);
   private final SchoolManager schoolManager = new SchoolManager(this);
   private final MaskingManager maskingManager = new MaskingManager(this);
   private ConditionalPingMask conditionalPingMask = ConditionalPingMask.EMPTY;

   private final ExclusionManager exclusionManager = new ExclusionManager(this);
   private final BubbleCorrectionManager bubbleCorrectionManager = new BubbleCorrectionManager(this);
   private final ThresholdManager thresholdManager = new ThresholdManager(this);
   private final StoringConfigManager storingConfigManager = new StoringConfigManager(this);

   private final ListenableProperty<List<Region>> selectedRegions = new ListenableProperty<>(List.of());
   private final ArgChangeManager<ConditionalPingMask> conditionalPingMaskChangeManager = new ArgChangeManager<>();
   private final ArgChangeManager<RegionEvent> regionBoundaryChangeManager = new ArgChangeManager<>();
   private final ArgChangeManager<RegionEvent> regionDefinitionChangeManager = new ArgChangeManager<>();
   private final ArgChangeManager<Collection<? extends Region>> regionDeletedChangeManager = new ArgChangeManager<>();
   private final ArgChangeManager<Region> labelsChangeManager = new ArgChangeManager<>();
   private final ArgChangeManager<EchogramSelection> echogramSelectionChangeManager = new ArgChangeManager<>();

   private boolean skipNotifications;
   private @Nullable EchogramPoint selectedRegionsReferencePoint;

   private final ArgChangeManager<Object> interpretationChangeManager = new ArgChangeManager<>();

   public RegionManager(RegionConfiguration regionConfiguration) {
      this.regionConfiguration = regionConfiguration;
   }

   public void close() {
      schoolManager.cancelEdit();
   }

   public ChangeManager getUndoChangeManager() {
      return schoolManager.getUndoChangeManager();
   }

   public ArgChangeManager<Object> getInterpretationChangeManager() {
      return interpretationChangeManager;
   }

   public RegionConfiguration getRegionConfiguration() {
      return regionConfiguration;
   }

   public PingContainer getPingContainer() {
      return regionConfiguration.getPingContainer();
   }

   public LayerManager getLayerManager() {
      return layerManager;
   }

   public SchoolManager getSchoolManager() {
      return schoolManager;
   }

   public MaskingManager getMaskingManager() {
      return maskingManager;
   }

   public ThresholdManager getThresholdManager() {
      return thresholdManager;
   }

   public StoringConfigManager getStoringConfigManager() {
      return storingConfigManager;
   }

   public boolean isSkipNotifications() {
      return skipNotifications;
   }

   public void setSkipNotifications(boolean skipNotifications) {
      this.skipNotifications = skipNotifications;
      if (!skipNotifications) {
         notifyAllListeners();
      }
   }

   PingRange getVisiblePingRange() {
      return regionConfiguration.getVisiblePingRange();
   }

   public void setNextRegionSelected() {
      shiftSelectedRegion(1);
   }

   public void setPreviousRegionSelected() {
      shiftSelectedRegion(-1);
   }

   private void shiftSelectedRegion(int step) {
      Comparator<Region> regionComparator = this::compareRegions;

      List<Layer> visibleLayers = layerManager.getLayersIntersectingPingRange(getVisiblePingRange());
      visibleLayers.sort(regionComparator);

      List<School> visibleSchools = schoolManager.getSchoolsIntersectingPingRange(getVisiblePingRange());
      visibleSchools.sort(regionComparator);

      List<Region> visibleRegions = new UnionList<>(visibleSchools, visibleLayers);

      Region currentRegion = null;
      List<School> selectedVisibleSchools = visibleSchools.stream().filter(School::isSelected).toList();
      if (!selectedVisibleSchools.isEmpty()) {
         currentRegion = step < 0 ? selectedVisibleSchools.getFirst() : selectedVisibleSchools.getLast();
      }
      if (currentRegion == null) {
         List<Layer> selectedVisibleLayers = visibleLayers.stream().filter(Layer::isSelected).toList();
         if (!selectedVisibleLayers.isEmpty()) {
            currentRegion = step < 0 ? selectedVisibleLayers.getFirst() : selectedVisibleLayers.getLast();
         }
      }

      Region regionToSelect;
      if (currentRegion == null) {
         Region nextRegion = findNextRegion(visibleRegions, selectedRegionsReferencePoint);
         if (nextRegion == null) {
            regionToSelect = visibleRegions.getFirst();
         } else if (step > 0) {
            regionToSelect = nextRegion;
         } else {
            regionToSelect = Utils.shift(visibleRegions, nextRegion, -1);
         }
      } else {
         regionToSelect = Utils.shift(visibleRegions, currentRegion, step);
      }
      replaceSelectedRegions(regionToSelect);
   }

   private int compareRegions(Region region1, Region region2) {
      if (region1 instanceof School != region2 instanceof School) {
         return region1 instanceof School ? -1 : 1;
      }
      PingIndex p1 = region1.getPingRange().begin();
      PingIndex p2 = region2.getPingRange().begin();
      int pingComparison = p1.compareTo(p2);
      if (pingComparison != 0) {
         return pingComparison;
      }
      return compareDepths(region1.getRepresentativeMinDepth(p1), region2.getRepresentativeMinDepth(p2));
   }

   private int compareDepths(float depth1, float depth2) {
      return regionConfiguration.getDataConfiguration().isSeabedMounted()
            ? Float.compare(depth2, depth1)
            : Float.compare(depth1, depth2);
   }

   private @Nullable Region findNextRegion(Collection<Region> regions, @Nullable EchogramPoint point) {
      if (point != null) {
         for (Region region : regions) {
            PingIndex pingIndex = region.getPingRange().begin();
            int pingComparison = pingIndex.compareTo(point.pingIndex());
            if (pingComparison > 0 || pingComparison == 0 && compareDepths(region.getRepresentativeMinDepth(pingIndex), point.depth()) > 0) {
               return region;
            }
         }
      }
      return null;
   }

   public ObservableValue<List<Region>> selectedRegions() {
      return selectedRegions;
   }

   public ArgChangeManager<RegionEvent> getRegionDefinitionChangeManager() {
      return regionDefinitionChangeManager;
   }

   public ArgChangeManager<Collection<? extends Region>> getRegionDeletedChangeManager() {
      return regionDeletedChangeManager;
   }

   public ArgChangeManager<Region> getLabelsChangeManager() {
      return labelsChangeManager;
   }

   public ArgChangeManager<EchogramSelection> getEchogramSelectionChangeManager() {
      return echogramSelectionChangeManager;
   }

   void notifyRegionListenersSelectedRegions() {
      if (skipNotifications) {
         return;
      }
      List<Region> regions = Stream.concat(
                  layerManager.getSelectedRegions().stream(),
                  schoolManager.getSelectedRegions().stream())
            .toList();
      if (!regions.isEmpty()) {
         selectedRegionsReferencePoint = regions.stream()
               .max(this::compareRegions)
               .map(region -> new EchogramPoint(region.getPingRange().begin(), region.getRepresentativeMinDepth(region.getPingRange().begin())))
               .orElse(null);
      }
      selectedRegions.setValue(regions);
   }

   private void notifyRegionListenersRegionChanged(RegionEvent regionEvent) {
      if (skipNotifications) {
         return;
      }
      regionDefinitionChangeManager.notifyListeners(regionEvent);
   }

   void notifyRegionListenersRegionDeleted(Region region) {
      notifyRegionListenersRegionsDeleted(List.of(region));
   }

   void notifyRegionListenersRegionsDeleted(Collection<? extends Region> regions) {
      if (skipNotifications) {
         return;
      }
      regionDeletedChangeManager.notifyListeners(regions);
   }

   public void toggleSelection(Collection<? extends Region> regions) {
      for (Region region : regions) {
         if (region.isSelected()) {
            deselectRegionWithoutNotify(region);
         } else {
            selectRegionWithoutNotify(region);
         }
      }
      notifyRegionListenersSelectedRegions();
   }

   public void selectRegions(Predicate<? super Region> predicate) {
      List<Region> regions = regionStream()
            .filter(predicate)
            .toList();
      selectRegions(regions);
   }

   public void selectRegions(Collection<? extends Region> regions) {
      boolean didModify = false;
      for (Region region : regions) {
         didModify |= selectRegionWithoutNotify(region);
      }
      if (didModify) {
         notifyRegionListenersSelectedRegions();
      }
   }

   private boolean selectRegionWithoutNotify(Region region) {
      return switch (region) {
         case Layer layer -> layerManager.selectRegionWithoutNotify(layer);
         case School school -> schoolManager.selectRegionWithoutNotify(school);
      };
   }

   private boolean deselectRegionWithoutNotify(Region region) {
      return switch (region) {
         case Layer layer -> layerManager.deselectRegionWithoutNotify(layer);
         case School school -> schoolManager.deselectRegionWithoutNotify(school);
      };
   }

   public void deselectRegions(Predicate<? super Region> predicate) {
      List<Region> regions = getSelectedRegions().stream()
            .filter(predicate)
            .toList();
      deselectRegions(regions);
   }

   public void deselectRegions(Collection<? extends Region> regions) {
      boolean didModify = false;
      for (Region region : regions) {
         didModify |= deselectRegionWithoutNotify(region);
      }
      if (didModify) {
         notifyRegionListenersSelectedRegions();
      }
   }

   public void doEchogramSelection(EchogramSelection echogramSelection) {
      List<Region> regions = getIntersectingRegions(echogramSelection.echogramRectangle());
      doSelection(regions, echogramSelection.action());
      echogramSelectionChangeManager.notifyListeners(echogramSelection);
   }

   public void doSelection(List<Region> regions, SelectionAction action) {
      switch (action) {
         case ADD -> selectRegions(regions);
         case REPLACE -> replaceSelectedRegions(regions);
         case TOGGLE -> toggleSelection(regions);
      }
   }

   private List<Region> getIntersectingRegions(EchogramRectangle echogramRectangle) {
      return getVisibleRegions().parallelStream()
            .filter(region -> {
               PingRange intersectingPingRange = region.getPingRange().intersection(echogramRectangle.pingRange());
               return getPingContainer().getPingIndexStream(intersectingPingRange).anyMatch(pingIndex -> {
                  FloatRange selectedDepthRange = echogramRectangle.depthRange(pingIndex);
                  return getNonMaskedRegionDepthRanges(region, pingIndex).intersects(selectedDepthRange);
               });
            })
            .toList();
   }

   public List<Region> getGeoIntersectingRegions(Rectangle2D geoRect) {
      return getVisibleRegions().parallelStream()
            .filter(region -> {
               PingRange intersectingPingRange = region.getPingRange().intersection(getVisiblePingRange());
               return getPingContainer().getPingIndexStream(intersectingPingRange).anyMatch(pingIndex -> {
                  GeoPoint geoPoint = pingIndex.getGeographicalPosition();
                  return geoPoint != null && geoRect.contains(geoPoint);
               });
            })
            .toList();
   }

   public void replaceSelectedRegions(Region region) {
      replaceSelectedRegions(List.of(region));
   }

   public void replaceSelectedRegions(Collection<? extends Region> regions) {
      Set<Layer> layers = new HashSet<>();
      Set<School> schools = new HashSet<>();
      for (Region region : regions) {
         switch (region) {
            case Layer layer -> layers.add(layer);
            case School school -> schools.add(school);
         }
      }
      layerManager.replaceSelectedRegions(layers);
      schoolManager.replaceSelectedRegions(schools);
   }

   public void replaceSelectedRegions(Predicate<? super Region> predicate) {
      replaceSelectedRegions(List.of());
      selectRegions(predicate);
   }

   public List<Region> getSelectedRegions() {
      return selectedRegions.getValue();
   }

   public List<Region> getVisibleRegions() {
      return intersectingRegions(getVisiblePingRange()).toList();
   }

   private Stream<Region> intersectingRegions(PingRange pingRange) {
      return Stream.concat(
            layerManager.layersIntersectingPingRange(pingRange),
            schoolManager.schoolsIntersectingPingRange(pingRange)
      );
   }

   public List<School> getVisibleSchools() {
      return schoolManager.getSchoolsIntersectingPingRange(getVisiblePingRange());
   }

   public Stream<Region> regionStream() {
      return Stream.concat(
            layerManager.getLayers().stream(),
            schoolManager.getSchools().stream());
   }

   /**
    * Returns either a school or a layer. Schools are prioritized.
    *
    * @param echogramPoint an echogram point
    * @return a school or layer containing the echogram point
    */
   public @Nullable Region getRegion(EchogramPoint echogramPoint) {
      School school = schoolManager.getRegion(echogramPoint);
      if (school != null) {
         return school;
      }
      return layerManager.getRegion(echogramPoint);
   }

   public static PingRange getPingRange(Collection<? extends Region> regions) {
      PingRange pingRange = PingRange.EMPTY_RANGE;
      for (Region region : regions) {
         pingRange = pingRange.union(region.getPingRange());
      }
      return pingRange;
   }

   public FloatRange getDepthRange(Collection<? extends Region> regions) {
      FloatRange regionDepthRange = FloatRange.EMPTY_RANGE;
      for (Region region : regions) {
         regionDepthRange = regionDepthRange.union(getDepthRange(region));
      }
      return regionDepthRange;
   }

   public FloatRange getDepthRange(Region region) {
      PingRange pingRange = region.getPingRange();
      PingContainer pingContainer = getPingContainer();
      // Clamping range to the current range in the data file set. When changing data files,
      // there may be an intermediate state where the region range extend outside the data file range.
      pingRange = pingRange.intersection(pingContainer.getTotalRange());
      return pingContainer.getPingIndexStream(pingRange)
            .map(pingIndex -> region.getDepthRanges(pingIndex).getBoundingRange())
            .reduce(FloatRange.EMPTY_RANGE, FloatRange::union);
   }

   public void setConditionalPingMask(ConditionalPingMask conditionalPingMask) {
      this.conditionalPingMask = conditionalPingMask;
      notifyConditionalPingMask(conditionalPingMask);
   }

   private void notifyConditionalPingMask(ConditionalPingMask conditionalPingMask) {
      if (skipNotifications) {
         return;
      }
      conditionalPingMaskChangeManager.notifyListeners(conditionalPingMask);
      notifyAllRegionsInPingRange(getPingContainer().getTotalRange());
   }

   public ConditionalPingMask getConditionalPingMask() {
      return conditionalPingMask;
   }

   public ArgChangeManager<ConditionalPingMask> getConditionalPingMaskChangeManager() {
      return conditionalPingMaskChangeManager;
   }

   public float getRepresentativeUpperDepth(Region region, PingIndex pingIndex) {
      if (regionConfiguration.getDataConfiguration().isSeabedMounted()) {
         return region.getRepresentativeMaxDepth(pingIndex);
      } else {
         return region.getRepresentativeMinDepth(pingIndex);
      }
   }

   /**
    * Depth ranges with masking applied.
    *
    * @param region  the region
    * @param ping    the ping to get depth ranges for
    * @param channel the channel to get depth ranges for. If channel &lt; 0, no channel-dependent masking will be applied
    * @return a collection of depth ranges
    */
   public FloatRangeSet getDepthRangesForChannel(Region region, Ping ping, int channel) {
      if (exclusionManager.isExcluded(ping.getPingIndex())) {
         return FloatRangeSet.of();
      }
      FloatRangeSet depthRanges = getNonMaskedRegionDepthRanges(region, ping.getPingIndex());
      if (depthRanges.isEmpty()) {
         return FloatRangeSet.of();
      }
      // Remove masking
      return depthRanges.subtract(getMaskedDepthRanges(ping, channel));
   }

   /**
    * Depth ranges for a region with no masking applied.
    *
    * @param region    the region
    * @param pingIndex the ping index to get depth ranges for
    * @return depth ranges
    */
   public FloatRangeSet getNonMaskedRegionDepthRanges(Region region, PingIndex pingIndex) {
      return switch (region) {
         case Layer layer -> {
            FloatRange depthRange = layer.getDepthRange(pingIndex);
            if (depthRange.isEmpty()) {
               yield FloatRangeSet.of();
            }
            long pingNumber = pingIndex.getPingNumber();
            FloatRangeSet result = FloatRangeSet.of(depthRange);
            // Subtract depth range for schools
            for (School school : schoolManager.getSchools()) {
               if (school.getPingRange().containsPingNumber(pingNumber)) {
                  result = result.subtract(school.getDepthRanges(pingIndex));
               }
            }
            yield result;
         }
         case School school -> school.getDepthRanges(pingIndex);
      };
   }

   public Map<Region, FloatRangeSet> getNonMaskedRegionDepthRanges(PingIndex pingIndex) {
      List<FloatRange> allSchoolRanges = new ArrayList<>();
      Map<Region, FloatRangeSet> result = new HashMap<>();
      schoolManager.getSchools().stream()
            .filter(school -> school.getPingRange().contains(pingIndex))
            .forEach(school -> {
               FloatRangeSet schoolRanges = school.getDepthRanges(pingIndex);
               allSchoolRanges.addAll(schoolRanges.getFloatRanges());
               result.put(school, schoolRanges);
            });
      FloatRangeSet allSchoolRangeSet = FloatRangeSet.of(allSchoolRanges);
      layerManager.getLayers().stream()
            .filter(layer -> layer.getPingRange().contains(pingIndex))
            .forEach(layer -> {
               FloatRangeSet layerRangeSet = layer.getDepthRanges(pingIndex).subtract(allSchoolRangeSet);
               result.put(layer, layerRangeSet);
            });
      return result;
   }

   public FloatRangeSet getMaskedDepthRanges(Ping ping, int channel) {
      if (exclusionManager.isExcluded(ping.getPingIndex())) {
         return FloatRangeSet.of(FloatRange.ALL);
      }
      return maskingManager.getMask(channel).get(ping.getPingIndex())
            .add(conditionalPingMask.getMask(ping));
   }

   public ExclusionManager getExclusionManager() {
      return exclusionManager;
   }

   public BubbleCorrectionManager getBubbleCorrectionManager() {
      return bubbleCorrectionManager;
   }

   public void addHorizontalLayerBoundary(PingIndex pingIndex, ToFloatFunction<PingIndex> pingIndexToDepth) {
      EchogramPoint point = new EchogramPoint(pingIndex, pingIndexToDepth.applyAsFloat(pingIndex));
      LayerAndBoundaryPair<CurveBoundary> layerAndBoundaryPair = layerManager.addCurveBoundary(point, IdentityDepthTransform.INSTANCE);
      if (layerAndBoundaryPair != null) {
         CurveBoundary boundary = layerAndBoundaryPair.boundary();
         layerManager.editBoundary(boundary.getPingRange(), pingIndexToDepth, boundary);
      }
   }

   public void addHorizontalDivider(EchogramPoint point, DepthTransform depthTransform) {
      layerManager.addHorizontalDivider(point, depthTransform);
   }

   public boolean isReadOnlyIncludingEnd(PingIndex pingIndex) {
      return regionConfiguration.getReadOnlyPings().stream()
            .anyMatch(range -> range.containsIncludingEnd(pingIndex));
   }

   boolean isReadOnlyExcludingBegin(PingIndex pingIndex) {
      return regionConfiguration.getReadOnlyPings().stream()
            .anyMatch(range -> range.containsExcludingBegin(pingIndex));
   }

   public boolean isReadOnly(PingIndex pingIndex) {
      return regionConfiguration.getReadOnlyPings().contains(pingIndex);
   }

   public boolean isReadOnly(Range<PingIndex> pingRange) {
      return regionConfiguration.getReadOnlyPings().containsAny(pingRange);
   }

   public RangeSet<PingIndex> writeablePingRanges(Range<PingIndex> pingRange) {
      return RangeUtils.toComplement(regionConfiguration.getReadOnlyPings(), pingRange);
   }

   public @Nullable PingIndex toWritablePingIndex(@Nullable EchogramPoint echogramPoint, PingIndex referencePingIndex, Range<PingIndex> pingRange) {
      for (Range<PingIndex> writeablePingRange : writeablePingRanges(pingRange)) {
         if (writeablePingRange.containsIncludingEnd(referencePingIndex)) {
            if (echogramPoint != null) {
               return writeablePingRange.clamp(echogramPoint.pingIndex());
            }
         }
      }
      return null;
   }

   public void addVerticalDivider(PingIndex pingIndex) {
      if (isReadOnlyExcludingBegin(pingIndex)) {
         throw new IllegalEditException();
      }
      addVerticalDivider(pingIndex, false, true);
   }

   private void addVerticalDivider(PingIndex pingIndex, boolean inheritVisitedStatus, boolean splitSchools) {
      layerManager.addVerticalDivider(pingIndex, inheritVisitedStatus);
      if (splitSchools) {
         schoolManager.addVerticalDivider(pingIndex, inheritVisitedStatus);
      }
   }

   /**
    * Isolates a range by adding vertical dividers.
    *
    * @param pingRange    the range to isolate
    * @param splitSchools whether schools should be split
    * @see #addVerticalDivider(PingIndex)
    */
   public void isolate(Range<PingIndex> pingRange, boolean splitSchools) {
      if (!pingRange.isEmpty()) {
         addVerticalDivider(pingRange.begin(), true, splitSchools);
         addVerticalDivider(pingRange.end(), true, splitSchools);
      }
   }

   private void notifySchoolAdded(School school) {
      // Notify about the layers affected
      PingRange schoolRange = school.getPingRange();
      notifyLayersInPingRange(schoolRange);
   }

   public @Nullable School addSchool(EchogramPoint startPoint, EchogramPoint endPoint, DepthTransform depthTransform) {
      for (PingIndex pingIndex : List.of(startPoint.pingIndex(), endPoint.pingIndex())) {
         if (isReadOnlyExcludingBegin(pingIndex)) {
            throw new IllegalEditException();
         }
      }
      School school = schoolManager.addSchoolNew(startPoint, endPoint, depthTransform);
      if (school == null) {
         return null;
      }
      notifySchoolAdded(school);
      return school;
   }

   public @Nullable School addSchool(NavigableMap<PingIndex, FloatRangeSet> schoolMask) {
      for (PingIndex pingIndex : schoolMask.keySet()) {
         if (isReadOnly(pingIndex)) {
            throw new IllegalEditException();
         }
      }
      School school = schoolManager.addSchoolNew(schoolMask);
      if (school == null) {
         return null;
      }
      notifySchoolAdded(school);
      return school;
   }

   public @Nullable School addSchool(List<EchogramPoint> points, DepthTransform depthTransform) {
      for (EchogramPoint echogramPoint : points) {
         if (isReadOnlyExcludingBegin(echogramPoint.pingIndex())) {
            throw new IllegalEditException();
         }
      }
      School school = schoolManager.addSchool(points, depthTransform);
      if (school == null) {
         return null;
      }
      notifySchoolAdded(school);
      return school;
   }

   void redoAddSchool(School school) {
      schoolManager.addSchoolWithoutUndoEdit(school);
      PingRange schoolRange = school.getPingRange();
      notifyLayersInPingRange(schoolRange);
   }

   void redoDeleteSchool(School school) {
      PingRange schoolRange = school.getPingRange();
      schoolManager.deleteSchoolWithoutUndoEdit(school);
      notifyLayersInPingRange(schoolRange);
   }

   public void deleteSchool(School school) {
      if (school.isReadOnly()) {
         throw new IllegalEditException();
      }
      deleteSchoolWithoutReadOnlyCheck(school);
   }

   private void deleteSchoolWithoutReadOnlyCheck(School school) {
      PingRange schoolRange = school.getPingRange();
      schoolManager.addEdit(schoolManager.deleteSchool(school));
      notifyLayersInPingRange(schoolRange);
   }

   void deleteSchoolWithoutUndo(School school) {
      PingRange schoolRange = school.getPingRange();
      schoolManager.deleteSchoolWithoutUndoEdit(school);
      notifyLayersInPingRange(schoolRange);
   }

   public void setSchoolMask(School school, NavigableMap<PingIndex, FloatRangeSet> schoolMask) {
      PingRange editedPingRange = schoolManager.setSchoolMask(school, schoolMask);
      notifyLayersInPingRange(editedPingRange);
   }

   public List<String> mergeSelectedSchools() {
      Set<School> schools = schoolManager.selectedRegions;
      int schoolCount = schools.size(); // Save this before modifying selected regions
      if (schoolCount < 2) {
         return List.of("Select at least two schools");
      }
      List<School> writableSchools = schools.stream()
            .filter(InterpretationContainer::isWritable)
            .toList();
      School result = schoolManager.mergeSchools(writableSchools);
      if (result != null) {
         selectRegions(List.of(result));
      }
      if (writableSchools.size() < schoolCount) {
         return List.of("Cannot merge read-only schools");
      }
      return List.of();
   }

   public List<String> deleteSelectedSchools() {
      Set<String> errors = new TreeSet<>();
      for (School school : schoolManager.getSelectedRegions()) {
         if (school.isReadOnly()) {
            errors.add("Cannot delete read-only schools");
            continue;
         }
         deleteSchoolWithoutReadOnlyCheck(school);
      }
      return List.copyOf(errors);
   }

   public ArgChangeManager<RegionEvent> getRegionBoundaryChangeManager() {
      return regionBoundaryChangeManager;
   }

   void notifyRegionBoundaryChanged(PingRange pingRange, Region region) {
      notifyRegionBoundaryChanged(pingRange, List.of(region));
   }

   void notifyRegionBoundaryChanged(PingRange pingRange, Collection<? extends Region> regions) {
      if (skipNotifications) {
         return;
      }
      RegionEvent regionEvent = new RegionEvent(pingRange, regions);
      regionBoundaryChangeManager.notifyListeners(regionEvent);
      notifyRegionListenersRegionChanged(regionEvent);
   }

   void notifyAllRegionsInPingRange(PingRange pingRange) {
      if (skipNotifications) {
         return;
      }
      RegionEvent regionEvent = new RegionEvent(pingRange, intersectingRegions(pingRange).toList());
      notifyRegionListenersRegionChanged(regionEvent);
   }

   void notifyLayersInPingRange(PingRange pingRange) {
      if (skipNotifications) {
         return;
      }
      RegionEvent regionEvent = new RegionEvent(pingRange, layerManager.layersIntersectingPingRange(pingRange).toList());
      notifyRegionListenersRegionChanged(regionEvent);
   }

   public boolean canUndo() {
      return schoolManager.canUndo();
   }

   public void undo() {
      if (!canUndo()) {
         return;
      }
      PingRange adjustRange = schoolManager.undo();
      notifyLayersInPingRange(adjustRange);
   }

   public boolean canRedo() {
      return schoolManager.canRedo();
   }

   public void redo() {
      if (!canRedo()) {
         return;
      }
      PingRange adjustRange = schoolManager.redo();
      notifyLayersInPingRange(adjustRange);
   }

   public void setupDefaultBoundaries() {
      setupDefaultBoundaries(regionConfiguration.initialUpperDepth(), regionConfiguration.initialLowerDepth());
   }

   public void setupDefaultBoundaries(ToFloatFunction<PingIndex> upperDepth, ToFloatFunction<PingIndex> lowerDepth) {
      replaceSelectedRegions(List.of());
      PingRange totalRange = getPingContainer().getTotalRange();
      exclusionManager.clear();
      maskingManager.clear();
      bubbleCorrectionManager.clear();
      bubbleCorrectionManager.reset(totalRange);
      thresholdManager.clear();
      thresholdManager.reset(totalRange);
      storingConfigManager.clear();
      layerManager.setupInitialLayerBoundaries(upperDepth, lowerDepth);
      schoolManager.removeAllSchools();
      selectedRegionsReferencePoint = null;
   }

   public void reset(PingRange pingRange) {
      reset(pingRange, regionConfiguration.initialUpperDepth(), regionConfiguration.initialLowerDepth());
   }

   public void reset(PingRange pingRange, ToFloatFunction<PingIndex> upperDepth, ToFloatFunction<PingIndex> lowerDepth) {
      if (isReadOnly(pingRange)) {
         throw new IllegalEditException();
      }
      if (pingRange.isEmpty()) {
         return;
      }
      if (pingRange.equals(getPingContainer().getTotalRange())) {
         setupDefaultBoundaries(upperDepth, lowerDepth);
         return;
      }
      addVerticalDivider(pingRange.begin());
      addVerticalDivider(pingRange.end());
      exclusionManager.reset(pingRange);
      maskingManager.reset(pingRange);
      bubbleCorrectionManager.reset(pingRange);
      thresholdManager.reset(pingRange);
      storingConfigManager.remove(pingRange);

      replaceSelectedRegions(schoolManager.getSchoolsIntersectingPingRange(pingRange));
      deleteSelectedSchools();
      replaceSelectedRegions(layerManager.getLayersIntersectingPingRange(pingRange));
      layerManager.mergeSelectedLayers();

      Layer layer = layerManager.getLayersIntersectingPingRange(pingRange).getFirst();
      layer.getInterpretation().reset();

      // First edit the lower boundary to make sure it does not block editing of the upper boundary:
      layerManager.editBoundary(pingRange, __ -> Float.POSITIVE_INFINITY, layer.getLowerCurveBoundaries().getFirst());
      layerManager.editBoundary(pingRange, upperDepth, layer.getUpperCurveBoundaries().getFirst());
      layerManager.editBoundary(pingRange, lowerDepth, layer.getLowerCurveBoundaries().getFirst());
   }

   public static Element rangeToXml(Range<PingIndex> range) {
      return DocumentHelper.createElement(XML_TIME_RANGE)
            .addAttribute(XML_START_TIME, Double.toString(PingMapping.TIME.valueOf(range.begin())))
            .addAttribute(XML_NUMBER_OF_PINGS, Integer.toString(PingRange.of(range).getPingCount()));
   }

   public static PingRange xmlToRange(Element element, PingContainer pingContainer) throws WorkFileException {
      double startTime = Double.parseDouble(element.attributeValue(XML_START_TIME));
      int numberOfPings = Integer.parseInt(element.attributeValue(XML_NUMBER_OF_PINGS));
      PingIndex startPing = pingContainer.getClosestPingIndex(startTime, PingMapping.TIME);
      PingIndex endPing = pingContainer.getPingIndexOrNull(startPing.getPingNumber() + numberOfPings);
      if (endPing == null) {
         throw new WorkFileException("The ping range is not contained in the data files");
      }
      return PingRange.of(startPing, endPing);
   }

   public Element toXml(PingRange pingRange) {
      Element interpretation = DocumentHelper.createElement(XML_REGION_INTERPRETATION)
            .addAttribute(XmlUtils.VERSION, WorkFile.XML_NEWEST_VERSION);

      interpretation.add(rangeToXml(pingRange));
      interpretation.add(exclusionManager.toXml(pingRange));
      interpretation.add(bubbleCorrectionManager.toXml(pingRange));
      interpretation.add(maskingManager.toXml(pingRange));
      interpretation.add(thresholdManager.toXml(pingRange));
      interpretation.add(storingConfigManager.toXml(pingRange));
      interpretation.add(layerManager.toXml(pingRange));
      interpretation.add(schoolManager.toXml(pingRange));

      return interpretation;
   }

   public void fromXml(Element element) throws WorkFileException {
      if (!XmlUtils.getVersion(element).equals(WorkFile.XML_NEWEST_VERSION)) {
         throw new WorkFileException("Wrong version number in work file");
      }
      PingRange totalRange = getPingContainer().getTotalRange();
      PingRange pingRange = xmlToRange(element.element(XML_TIME_RANGE), getPingContainer());
      if (!totalRange.contains(pingRange)) {
         throw new WorkFileException("Work file ping range, " + pingRange + ", not contained in data ping range, " + totalRange);
      }
      tryFromXml(() -> layerManager.fromXml(element, pingRange), "layers");
      tryFromXml(() -> schoolManager.fromXml(element, pingRange), "schools");
      tryFromXml(() -> exclusionManager.fromXml(element), "exclusions");
      tryFromXml(() -> bubbleCorrectionManager.fromXml(element), "bubble correction");
      tryFromXml(() -> maskingManager.fromXml(element, pingRange.begin()), "masking");
      tryFromXml(() -> thresholdManager.fromXml(element), "thresholds");
      tryFromXml(() -> storingConfigManager.fromXml(element), "database storing");

      if (totalRange.begin().getPingNumber() < pingRange.begin().getPingNumber()) {
         // Work xml is missing something at the beginning.
         Layer layer = new Layer(this);
         layerManager.getIntersectingVerticalBoundaries(PingRange.ofSinglePing(pingRange.begin(), getPingContainer())).forEach(layer::addVerticalBoundary);
         LayerConnector upperConnector = new LayerConnector(new EchogramPoint(totalRange.begin(), 0));
         LayerConnector lowerConnector = new LayerConnector(new EchogramPoint(totalRange.begin(), 0));
         layer.addVerticalBoundary(new VerticalBoundary(upperConnector, lowerConnector));
         layer.addUpperBoundary(new CurveBoundary(upperConnector, layerManager.getUpperBoundary(pingRange.begin()).getStartConnector()));
         layer.addLowerBoundary(new CurveBoundary(lowerConnector, layerManager.getBottomBoundary(pingRange.begin()).getStartConnector()));
         layerManager.getLayers().add(layer);
         reset(PingRange.ofUnsorted(pingRange.begin(), totalRange.begin()));
      }
      if (pingRange.end().getPingNumber() < totalRange.end().getPingNumber()) {
         // Work xml is missing something at the end.
         Layer layer = new Layer(this);
         layerManager.getIntersectingVerticalBoundaries(PingRange.ofSinglePing(pingRange.end(), getPingContainer())).forEach(layer::addVerticalBoundary);
         LayerConnector upperConnector = new LayerConnector(new EchogramPoint(totalRange.end(), 0));
         LayerConnector lowerConnector = new LayerConnector(new EchogramPoint(totalRange.end(), 0));
         layer.addVerticalBoundary(new VerticalBoundary(upperConnector, lowerConnector));
         PingIndex lastPingIndex = getPingContainer().previousOrSame(pingRange.end());
         layer.addUpperBoundary(new CurveBoundary(layerManager.getUpperBoundary(lastPingIndex).getEndConnector(), upperConnector));
         layer.addLowerBoundary(new CurveBoundary(layerManager.getBottomBoundary(lastPingIndex).getEndConnector(), lowerConnector));
         layerManager.getLayers().add(layer);
         reset(PingRange.ofUnsorted(pingRange.end(), totalRange.end()));
      }
   }

   private static void tryFromXml(ThrowingRunnable<Exception> task, String what) throws WorkFileException {
      try {
         task.run();
      } catch (Exception e) {
         throw new WorkFileException("Error parsing " + what + ": " + e, e);
      }
   }

   public void join(RegionManager left, RegionManager right) {
      layerManager.join(left.layerManager, right.layerManager);
      schoolManager.join(left.schoolManager, right.schoolManager);
      exclusionManager.join(left.exclusionManager, right.exclusionManager);
      bubbleCorrectionManager.join(left.bubbleCorrectionManager, right.bubbleCorrectionManager);
      maskingManager.join(left.maskingManager, right.maskingManager);
      thresholdManager.join(left.thresholdManager, right.thresholdManager);
      storingConfigManager.join(left.storingConfigManager, right.storingConfigManager);
   }

   private void notifyAllListeners() {
      notifyRegionListenersSelectedRegions();
      PingRange pingRange = getPingContainer().getTotalRange();
      List<Region> regions = regionStream().toList();
      RegionEvent regionEvent = new RegionEvent(pingRange, regions);
      regionBoundaryChangeManager.notifyListeners(regionEvent);
      regionDefinitionChangeManager.notifyListeners(regionEvent);
      conditionalPingMaskChangeManager.notifyListeners(conditionalPingMask);
      exclusionManager.notifyListeners(pingRange);
      maskingManager.notifyListeners(pingRange);
      bubbleCorrectionManager.notifyListeners();
      thresholdManager.notifyListeners();
      storingConfigManager.notifyListeners();
   }
}
