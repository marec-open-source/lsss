package no.imr.lsss.modules.integration;

import no.imr.korona.region.Region;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class RegionIntegrationData {
   final RegionCache selectedRegionCache = new RegionCache();
   final RegionCache allRegionCache = new RegionCache();
   final Map<Region, RegionCache> regionMap;

   RegionIntegrationData() {
      regionMap = new ConcurrentHashMap<>();
   }

   RegionIntegrationData(Map<Region, RegionCache> regionMap) {
      this.regionMap = regionMap;
   }
}
