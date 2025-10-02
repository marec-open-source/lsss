package no.imr.korona.region;

import no.imr.korona.data.datamanager.DataManager;

final class RegionManagerTestUtils {
   private RegionManagerTestUtils() {
   }

   static RegionManager createTestRegionManager(DataManager dataManager) {
      return new RegionManager(new SimpleRegionConfiguration(dataManager));
   }
}
