package no.imr.korona.region;

import no.imr.korona.data.ping.PingRange;
import no.marec.lsss.api.regions.RegionChangeEvent;

import java.util.Collection;
import java.util.List;

/**
 * Information given to listeners of {@link RegionManager#getRegionDefinitionChangeManager()}.
 */
public record RegionEvent(
      PingRange pingRange,
      Collection<? extends Region> regions
) implements RegionChangeEvent {

   public RegionEvent {
      regions = List.copyOf(regions); // Defensive copy. Could this be eliminated?
   }

   @Override
   public String toString() {
      return pingRange.getPingCount() + " pings, " + regions.size() + " regions";
   }
}
