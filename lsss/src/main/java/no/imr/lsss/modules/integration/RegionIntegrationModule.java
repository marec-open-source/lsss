package no.imr.lsss.modules.integration;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.region.ConditionalPingMask;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseDataModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.misc.ToFloatFunction;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Integrates over the selected regions.
 * This module does not display any graphics, but other modules may use its results by registering via {@link #getRegionIntegrationChangeManager()}.
 */
public final class RegionIntegrationModule extends BaseDataModule {
   private RegionIntegrationData internalData = new RegionIntegrationData();
   private RegionIntegrationData publishedData = internalData; // Separate published data to avoid flickering when zooming/resizing by e.g. texts in VerticalLineOverlay
   private final Listener refreshListener = newCoalescingExecListener(this::refresh);
   private final ChangeManager regionIntegrationChangeManager = new ChangeManager();

   public RegionIntegrationModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   public ChangeManager getRegionIntegrationChangeManager() {
      return regionIntegrationChangeManager;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(newCoalescingExecListener(this::recompute), List.of(
            getInterpretationSettings().getPingRangeChangeManager(),
            getInterpretationSettings().getPelagicZSettings().getZoomedChangeManager(),
            getInterpretationSettings().getBottomZSettings().getZoomedChangeManager(),
            getConfigurationManager().getGridConf().horizontalGridUnit,
            getInterpretationSettings().getChannelChangeManager(),
            getRegionManager().getThresholdManager().getChangeManager()
      ));

      registry.add(getInterpretationSettings().getPingSampler().getNewPingsChangeManager(), newExecListener(this::processPings));
      registry.add(getRegionManager().selectedRegions(), refreshListener);
      registry.add(getRegionManager().getRegionDeletedChangeManager(), newExecListener(regions -> {
         internalData.regionMap.keySet().removeAll(regions);
         refreshListener.listen();
      }));
      registry.add(getRegionManager().getRegionDefinitionChangeManager(), newExecListener(regionEvent -> {
         PingRange pingRange = regionEvent.pingRange();
         for (Region region : regionEvent.regions()) {
            RegionCache regionCache = internalData.regionMap.get(region);
            if (regionCache != null) {
               regionCache.getPingMap().subMap(pingRange.begin(), pingRange.end()).clear();
            }
         }
         refreshListener.listen();
      }));

      //---

      recompute();
   }

   @Override
   protected void onDisable() {
      internalData = new RegionIntegrationData();
      publishedData = internalData;
   }

   private void recompute() {
      internalData = new RegionIntegrationData();
      refreshListener.listen();
   }

   private void refresh() {
      internalData = new RegionIntegrationData(internalData.regionMap);
      processPings(getInterpretationSettings().getPingSampler().getAvailablePings());
   }

   private void processPings(List<Ping> pings) {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      PingMapping pingMapping = getConfigurationManager().getGridConf().horizontalGridUnit.getValue();
      int channel = getInterpretationSettings().getChannel();

      PingRange allVisibleRegionPingRange = PingRange.EMPTY_RANGE;
      PingRange selectedVisibleRegionPingRange = PingRange.EMPTY_RANGE;

      List<Ping> unprocessedPings = getUnprocessedPings(pings);

      for (Region region : getRegionManager().getVisibleRegions()) {
         PingRange visibleRegionPingRange = region.getPingRange().intersection(pingRange);
         if (visibleRegionPingRange.isEmpty()) {
            continue;
         }

         allVisibleRegionPingRange = allVisibleRegionPingRange.union(visibleRegionPingRange);
         if (region.isSelected()) {
            selectedVisibleRegionPingRange = selectedVisibleRegionPingRange.union(visibleRegionPingRange);
         }

         RegionCache regionCache = internalData.regionMap.get(region);
         if (regionCache == null) {
            regionCache = new RegionCache();
            internalData.regionMap.put(region, regionCache);
         }

         for (Ping ping : unprocessedPings) {
            PingIndex pingIndex = ping.getPingIndex();

            if (!visibleRegionPingRange.containsPingNumber(pingIndex.getPingNumber())) {
               continue;
            }

            PowerData powerData = ping.getPowerData(channel);
            if (powerData == null || getRegionManager().getExclusionManager().isExcluded(pingIndex)) {
               // Putting null in as PingCache in PingMap will make the integration line horizontal.
               regionCache.getPingMap().put(pingIndex, null);
               putNullIfAbsent(region, pingIndex);
               continue;
            }

            PingCache pingCache = regionCache.getPingMap().get(pingIndex);
            if (pingCache == null) {
               FloatRange svRange = getRegionManager().getThresholdManager().getLinearSvRange(pingIndex);
               FloatRangeSet depthRanges = getRegionManager().getDepthRangesForChannel(region, ping, powerData.getChannel());
               pingCache = createPingCache(pingIndex, powerData, svRange, depthRanges);
               regionCache.getPingMap().put(pingIndex, pingCache);
            }

            internalData.allRegionCache.getOrCreatePingCache(pingIndex).accumulate(pingCache);
            if (region.isSelected()) {
               internalData.selectedRegionCache.getOrCreatePingCache(pingIndex).accumulate(pingCache);
            }
         }

         regionCache.updateCurve(visibleRegionPingRange, pingMapping);

         // Ensure null at end of schools, or else curve will not be horizontal between two selected schools:
         putNullIfAbsent(region, visibleRegionPingRange.end());
      }

      internalData.allRegionCache.updateCurve(allVisibleRegionPingRange, pingMapping);
      internalData.selectedRegionCache.updateCurve(selectedVisibleRegionPingRange, pingMapping);

      publishedData = internalData;
      regionIntegrationChangeManager.notifyListeners();
   }

   private List<Ping> getUnprocessedPings(List<Ping> pings) {
      List<Ping> unprocessedPings = new ArrayList<>(pings.size());
      for (Ping ping : pings) {
         // map.get == null and not !map.containsKey will include nulls added with putNullIfAbsent.
         if (internalData.allRegionCache.getPingMap().get(ping.getPingIndex()) == null) {
            unprocessedPings.add(ping);
         }
      }
      return unprocessedPings;
   }

   private void putNullIfAbsent(Region region, PingIndex pingIndex) {
      internalData.allRegionCache.putNullIfAbsent(pingIndex);
      if (region.isSelected()) {
         internalData.selectedRegionCache.putNullIfAbsent(pingIndex);
      }
   }

   private PingCache createPingCache(PingIndex pingIndex, PowerData powerData, FloatRange svRange, FloatRangeSet depthRanges) {
      FloatRange pelagicDepthRange = getInterpretationSettings().getPelagicZSettings().getDepthRange(pingIndex);
      FloatRange bottomDepthRange = getInterpretationSettings().getBottomZSettings().getDepthRange(pingIndex);

      double verticallyIntegratedSvTotal = 0;
      double verticallyIntegratedSvPelagic = 0;
      double verticallyIntegratedSvBottom = 0;

      for (FloatRange depthRange : depthRanges) {
         verticallyIntegratedSvTotal += powerData.getVerticalIntegralSv(depthRange, svRange);
         verticallyIntegratedSvPelagic += powerData.getVerticalIntegralSv(depthRange.intersection(pelagicDepthRange), svRange);
         verticallyIntegratedSvBottom += powerData.getVerticalIntegralSv(depthRange.intersection(bottomDepthRange), svRange);
      }

      return new PingCache((float) verticallyIntegratedSvTotal, (float) verticallyIntegratedSvPelagic, (float) verticallyIntegratedSvBottom);
   }

   /**
    * Returns the s<sub>A</sub> integration curve for the visible part of the currently selected region(s).
    *
    * @return the s<sub>A</sub> integration curve
    */
   public List<IntegrationCurvePoint> getSaCurve() {
      return publishedData.selectedRegionCache.getCurve();
   }

   public List<IntegrationCurvePoint> getSaCurve(Region region) {
      RegionCache regionCache = publishedData.regionMap.get(region);
      return regionCache != null ? regionCache.getCurve() : List.of();
   }

   /**
    * Returns s<sub>A</sub> for the visible part of all region(s).
    *
    * @param integrationArea the area of integration
    * @return s<sub>A</sub>
    */
   public float getTotalSa(IntegrationArea integrationArea) {
      return getSa(publishedData.allRegionCache, integrationArea);
   }

   /**
    * Returns s<sub>A</sub> for the visible part of the currently selected region(s).
    *
    * @param integrationArea the area of integration
    * @return s<sub>A</sub>
    */
   public float getSa(IntegrationArea integrationArea) {
      return getSa(publishedData.selectedRegionCache, integrationArea);
   }

   /**
    * Returns accumulated s<sub>A</sub> for the visible part of a region.
    *
    * @param region          a region
    * @param integrationArea the area of integration
    * @return s<sub>A</sub>
    */
   public float getSa(Region region, IntegrationArea integrationArea) {
      return getSa(region, getInterpretationSettings().getPingRange(), integrationArea);
   }

   /**
    * Returns accumulated s<sub>L</sub> for the visible part of a region.
    *
    * @param region          a region
    * @param integrationArea the area of integration
    * @return s<sub>L</sub>
    */
   public float getSL(Region region, IntegrationArea integrationArea) {
      return getSL(region, getInterpretationSettings().getPingRange(), integrationArea);
   }

   /**
    * Returns s<sub>A</sub> for the visible part of a region in a ping range.
    *
    * @param region          a region
    * @param pingRange       a ping range
    * @param integrationArea the area of integration
    * @return s<sub>A</sub>
    */
   public float getSa(Region region, PingRange pingRange, IntegrationArea integrationArea) {
      if (pingRange.isEmpty()) {
         return 0;
      }
      RegionCache regionCache = publishedData.regionMap.get(region);
      if (regionCache == null) {
         return 0;
      }
      List<IntegrationCurvePoint> curve = regionCache.getCurve();
      if (curve.isEmpty()) {
         return 0;
      }

      int i = 0;
      IntegrationCurvePoint firstCurvePoint = null;
      for (; i < curve.size() - 1; i++) {
         if (curve.get(i + 1).pingIndex().getPingNumber() > pingRange.begin().getPingNumber()) {
            firstCurvePoint = curve.get(i);
            break;
         }
      }
      if (firstCurvePoint == null) {
         return 0;
      }

      IntegrationCurvePoint lastCurvePoint = null;
      for (; i < curve.size() - 1; i++) {
         if (curve.get(i + 1).pingIndex().getPingNumber() >= pingRange.end().getPingNumber()) {
            lastCurvePoint = curve.get(i + 1);
            break;
         }
      }
      if (lastCurvePoint == null) {
         lastCurvePoint = curve.getLast();
      }

      return getSa(lastCurvePoint, firstCurvePoint, integrationArea);
   }

   /**
    * Returns s<sub>L</sub> for the visible part of a region in a ping range.
    *
    * @param region          a region
    * @param pingRange       a ping range
    * @param integrationArea the area of integration
    * @return s<sub>L</sub>
    */
   public float getSL(Region region, PingRange pingRange, IntegrationArea integrationArea) {
      if (pingRange.isEmpty()) {
         return 0;
      }
      RegionCache regionCache = publishedData.regionMap.get(region);
      if (regionCache == null) {
         return 0;
      }
      List<IntegrationCurvePoint> curve = regionCache.getCurve();
      if (curve.isEmpty()) {
         return 0;
      }

      int i = 0;
      IntegrationCurvePoint firstCurvePoint = null;
      for (; i < curve.size() - 1; i++) {
         if (curve.get(i + 1).pingIndex().getPingNumber() >= pingRange.begin().getPingNumber()) {
            firstCurvePoint = curve.get(i);
            break;
         }
      }
      if (firstCurvePoint == null) {
         return 0;
      }

      IntegrationCurvePoint lastCurvePoint = null;
      for (; i < curve.size() - 1; i++) {
         if (curve.get(i + 1).pingIndex().getPingNumber() >= pingRange.end().getPingNumber()) {
            lastCurvePoint = curve.get(i + 1);
            break;
         }
      }
      if (lastCurvePoint == null) {
         lastCurvePoint = curve.getLast();
      }

      return getSL(lastCurvePoint, firstCurvePoint, integrationArea);
   }

   private static float getSa(RegionCache regionCache, IntegrationArea integrationArea) {
      List<IntegrationCurvePoint> curve = regionCache.getCurve();
      if (curve.isEmpty()) {
         return 0;
      }

      IntegrationCurvePoint firstCurvePoint = curve.getFirst();
      IntegrationCurvePoint lastCurvePoint = curve.getLast();
      return getSa(lastCurvePoint, firstCurvePoint, integrationArea);
   }

   private static float getSa(IntegrationCurvePoint lastCurvePoint, IntegrationCurvePoint firstCurvePoint, IntegrationArea integrationArea) {
      float sL = getSL(lastCurvePoint, firstCurvePoint, integrationArea);
      float distance = lastCurvePoint.accumulatedDistance() - firstCurvePoint.accumulatedDistance();
      if (distance == 0) {
         return 0;
      }

      return sL / distance;
   }

   private static float getSL(IntegrationCurvePoint lastCurvePoint, IntegrationCurvePoint firstCurvePoint, IntegrationArea integrationArea) {
      return lastCurvePoint.getHorizontallyIntegratedSv(integrationArea) - firstCurvePoint.getHorizontallyIntegratedSv(integrationArea);
   }

   /**
    * Calculates the s<sub>A</sub> for a region using only some of the pixels.
    * The integral is based on the pings currently available.
    *
    * @param regions     regions
    * @param channel     a channel number
    * @param excludeMask a mask for excluded samples
    * @return s<sub>A</sub> for the included pixels in the region in the visible ping range
    */
   public float calculateSa(Collection<Region> regions, int channel, ConditionalPingMask excludeMask) {
      RegionCache regionCache = new RegionCache();
      PingRange pingRange = RegionManager.getPingRange(regions).intersection(getInterpretationSettings().getPingRange());
      for (Ping ping : getInterpretationSettings().getPingSampler().getAvailablePings()) {
         if (pingRange.contains(ping)) {
            PowerData powerData = ping.getPowerData(channel);
            PingIndex pingIndex = ping.getPingIndex();
            if (powerData != null) {
               FloatRange svRange = getRegionManager().getThresholdManager().getLinearSvRange(pingIndex);
               FloatRangeSet mask = excludeMask.getMask(ping);
               for (Region region : regions) {
                  FloatRangeSet depthRanges = getRegionManager().getDepthRangesForChannel(region, ping, powerData.getChannel()).subtract(mask);
                  PingCache pingCache = createPingCache(pingIndex, powerData, svRange, depthRanges);
                  regionCache.getOrCreatePingCache(pingIndex).accumulate(pingCache);
               }
            } else {
               regionCache.getPingMap().put(pingIndex, null);
            }
         }
      }

      PingMapping pingMapping = getConfigurationManager().getGridConf().horizontalGridUnit.getValue();
      regionCache.updateCurve(pingRange, pingMapping);
      return getSa(regionCache, IntegrationArea.TOTAL);
   }

   public List<IntegrationCurvePoint> calculateSa(Collection<Region> regions, ToFloatFunction<Region> weight) {
      RegionCache totalRegionCache = new RegionCache();
      PingRange pingRange = PingRange.EMPTY_RANGE;
      for (Region region : regions) {
         RegionCache regionCache = publishedData.regionMap.get(region);
         if (regionCache == null) {
            continue;
         }
         float regionWeight = weight.applyAsFloat(region);
         PingRange regionPingRange = region.getPingRange();
         pingRange = pingRange.union(regionPingRange);
         for (IntegrationCurvePoint point : regionCache.getCurve()) {
            PingCache pingCache = point.pingCache();
            if (pingCache != null) {
               totalRegionCache.getOrCreatePingCache(point.pingIndex()).accumulate(pingCache, regionWeight);
            }
         }
         totalRegionCache.putNullIfAbsent(pingRange.end());
      }
      PingMapping pingMapping = getConfigurationManager().getGridConf().horizontalGridUnit.getValue();
      totalRegionCache.updateCurve(pingRange, pingMapping);
      return totalRegionCache.getCurve();
   }

   @Override
   public void stressTestValidation() {
      Set<Region> regions = new HashSet<>(publishedData.regionMap.keySet());
      getRegionManager().regionStream().forEach(regions::remove);
      assert regions.isEmpty();
   }
}
