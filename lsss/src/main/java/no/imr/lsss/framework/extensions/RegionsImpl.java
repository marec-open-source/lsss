package no.imr.lsss.framework.extensions;

import no.imr.lsss.LSSS;
import no.imr.tools.Utils;
import no.imr.tools.range.FloatRangeSet;
import no.marec.lsss.api.data.Ping;
import no.marec.lsss.api.data.PingIndex;
import no.marec.lsss.api.regions.Region;
import no.marec.lsss.api.regions.RegionChangeEvent;
import no.marec.lsss.api.regions.Regions;
import no.marec.lsss.api.util.FloatRange;
import no.marec.lsss.api.util.observing.Observable;
import no.marec.lsss.api.util.observing.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

final class RegionsImpl implements Regions {
   private final LSSS lsss;

   RegionsImpl(LSSS lsss) {
      this.lsss = lsss;
   }

   @Override
   public ObservableValue<? extends Collection<? extends Region>> selected() {
      return lsss.getRegionManager().selectedRegions();
   }

   @Override
   public void setSelected(Collection<? extends Region> regions) {
      lsss.getRegionManager().replaceSelectedRegions(new HashSet<>(regions)::contains);
   }

   @Override
   public Observable<? extends RegionChangeEvent> changed() {
      return lsss.getRegionManager().getRegionDefinitionChangeManager();
   }

   @Override
   public Observable<? extends Collection<? extends Region>> deleted() {
      return lsss.getRegionManager().getRegionDeletedChangeManager();
   }

   @Override
   public Observable<? extends Region> labelsChanged() {
      return lsss.getRegionManager().getLabelsChangeManager();
   }

   @Override
   public List<? extends FloatRange> depthRanges(Region region, Ping ping, int channel) {
      if (!(region instanceof no.imr.korona.region.Region r)) {
         return List.of();
      }
      if (!(ping instanceof no.imr.korona.data.ping.Ping p)) {
         return List.of();
      }
      return lsss.getRegionManager().getDepthRangesForChannel(r, p, channel).getFloatRanges();
   }

   @Override
   public void finishedEditingInterpretations() {
      lsss.getRegionManager().getInterpretationChangeManager().notifyListeners(this);
   }

   @Override
   public Observable<?> interpretationChanged() {
      return lsss.getRegionManager().getInterpretationChangeManager();
   }

   @Override
   public @Nullable Region createSchool(Map<PingIndex, List<FloatRange>> mask) {
      NavigableMap<no.imr.korona.data.ping.PingIndex, FloatRangeSet> m = new TreeMap<>();
      mask.forEach((k, v) -> {
         if (!(k instanceof no.imr.korona.data.ping.PingIndex pingIndex)) {
            return;
         }
         List<no.imr.tools.range.FloatRange> depthRanges = Utils.getAllOfType(v, no.imr.tools.range.FloatRange.class).toList();
         FloatRangeSet depthRangeSet = FloatRangeSet.of(depthRanges);
         if (!depthRangeSet.isEmpty()) {
            m.put(pingIndex, depthRangeSet);
         }
      });
      return lsss.getRegionManager().addSchool(m);
   }
}
