package no.imr.lsss.modules.ts;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.region.Region;
import no.imr.tools.listening.ChangeManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.stream.Stream;

public interface BaseTsModule {
   ChangeManager getTSDetectionChangeManager();

   default NavigableMap<PingIndex, ? extends BaseTsPingCache> getTSData(Region region) {
      return Collections.emptyNavigableMap();
   }

   default Stream<NavigableMap<PingIndex, ? extends BaseTsData>> getSelectedTracks() {
      return Stream.empty();
   }

   Map<PingIndex, ? extends Collection<? extends BaseTsData>> getTargetsForPoint(List<Region> regions, EchogramPoint echogramPoint, int channel);
}
