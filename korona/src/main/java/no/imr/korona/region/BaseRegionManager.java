package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

abstract class BaseRegionManager<T extends Region> {
   private final RegionManager regionManager;
   final Set<T> selectedRegions = ConcurrentHashMap.newKeySet();

   BaseRegionManager(RegionManager regionManager) {
      this.regionManager = regionManager;
   }

   RegionManager getRegionManager() {
      return regionManager;
   }

   PingContainer getPingContainer() {
      return regionManager.getPingContainer();
   }

   PingRange getVisiblePingRange() {
      return regionManager.getVisiblePingRange();
   }

   void replaceSelectedRegions(T region) {
      replaceSelectedRegions(Set.of(region));
   }

   void replaceSelectedRegions(Set<T> regions) {
      if (replaceSelectedRegionsWithoutNotify(regions)) {
         regionManager.notifyRegionListenersSelectedRegions();
      }
   }

   void selectRegion(T region) {
      selectRegions(List.of(region));
   }

   void selectRegions(Collection<T> regions) {
      if (selectRegionsWithoutNotify(regions)) {
         regionManager.notifyRegionListenersSelectedRegions();
      }
   }

   void deselectRegion(T region) {
      deselectRegions(List.of(region));
   }

   void deselectRegions(Collection<T> regions) {
      if (deselectRegionsWithoutNotify(regions)) {
         regionManager.notifyRegionListenersSelectedRegions();
      }
   }

   private boolean replaceSelectedRegionsWithoutNotify(Set<T> regions) {
      if (selectedRegions.equals(regions)) {
         return false;
      }
      deselectRegionsWithoutNotify(selectedRegions);
      selectRegionsWithoutNotify(regions);
      return true;
   }

   private boolean selectRegionsWithoutNotify(Collection<T> regions) {
      boolean didModify = false;
      for (T region : regions) {
         didModify |= selectRegionWithoutNotify(region);
      }
      return didModify;
   }

   boolean selectRegionWithoutNotify(T region) {
      if (selectedRegions.add(region)) {
         region.setSelected(true);
         return true;
      } else {
         return false;
      }
   }

   private boolean deselectRegionsWithoutNotify(Collection<T> regions) {
      boolean didModify = false;
      for (T region : regions) {
         didModify |= deselectRegionWithoutNotify(region);
      }
      return didModify;
   }

   boolean deselectRegionWithoutNotify(T region) {
      if (selectedRegions.remove(region)) {
         region.setSelected(false);
         return true;
      } else {
         return false;
      }
   }

   public Set<T> getSelectedRegions() {
      return selectedRegions;
   }

   abstract Set<T> getRegions();

   public @Nullable T getRegion(EchogramPoint point) {
      for (T region : getRegions()) {
         if (region.contains(point)) {
            return region;
         }
      }
      return null;
   }
}
