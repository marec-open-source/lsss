package no.imr.korona.data.util.mask;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Queue;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class GrowEngine {
   private final PingContainer pingContainer;
   private final Function<PingIndex, List<FloatRange>> mutableDepthRangeExtractor;
   private final Queue<Seed> seeds = new ArrayDeque<>();
   private Predicate<PingIndex> usablePings = _ -> true;
   private Consumer<Integer> pingCountListener = Utils.emptyConsumer();

   public GrowEngine(PingContainer pingContainer, Function<PingIndex, FloatRangeSet> depthRangeExtractor) {
      this.pingContainer = pingContainer;
      mutableDepthRangeExtractor = pingIndex -> {
         // Create a new mutable list since the growing will remove depth ranges.
         return new ArrayList<>(depthRangeExtractor.apply(pingIndex).getFloatRanges());
      };
   }

   public GrowEngine setUsablePings(Predicate<PingIndex> usablePings) {
      this.usablePings = usablePings;
      return this;
   }

   public GrowEngine addSeed(PingIndex pingIndex, FloatRange depthRange) {
      seeds.add(new Seed(pingIndex, depthRange));
      return this;
   }

   public GrowEngine addSeed(PingIndex pingIndex, List<FloatRange> depthRanges) {
      for (FloatRange depthRange : depthRanges) {
         seeds.add(new Seed(pingIndex, depthRange));
      }
      return this;
   }

   public GrowEngine setPingCountListener(Consumer<Integer> pingCountListener) {
      this.pingCountListener = pingCountListener;
      return this;
   }

   public Optional<NavigableMap<PingIndex, FloatRangeSet>> growSchoolMask(AsyncHandle asyncHandle) {
      int pingCount = 0;
      NavigableMap<PingIndex, FloatRangeSet> schoolMask = new TreeMap<>();
      Map<PingIndex, List<FloatRange>> candidateDepthRangesCache = new HashMap<>();
      while (!seeds.isEmpty()) {
         if (asyncHandle.isCancelled()) {
            return Optional.empty();
         }
         Seed seed = seeds.remove();
         List<FloatRange> candidateDepthRanges = candidateDepthRangesCache.computeIfAbsent(seed.pingIndex(), mutableDepthRangeExtractor);
         List<FloatRange> acceptedDepthRanges = new ArrayList<>();
         for (Iterator<FloatRange> iterator = candidateDepthRanges.iterator(); iterator.hasNext(); ) {
            FloatRange candidateDepthRange = iterator.next();
            if (candidateDepthRange.intersects(seed.depthRange())) {
               iterator.remove();
               acceptedDepthRanges.add(candidateDepthRange);
               addSeedForNeighbouringPing(seed.pingIndex(), -1, candidateDepthRange);
               addSeedForNeighbouringPing(seed.pingIndex(), 1, candidateDepthRange);
            }
         }
         if (!acceptedDepthRanges.isEmpty()) {
            schoolMask.merge(seed.pingIndex(), FloatRangeSet.of(acceptedDepthRanges), FloatRangeSet::add);
            if (schoolMask.size() > pingCount) {
               pingCount = schoolMask.size();
               pingCountListener.accept(pingCount);
            }
         }
      }
      return Optional.of(schoolMask);
   }

   private void addSeedForNeighbouringPing(PingIndex pingIndex, int step, FloatRange depthRange) {
      PingIndex nextPingIndex = pingContainer.getPingIndexOrNullExcludingEnd(pingIndex.getPingNumber() + step);
      if (nextPingIndex != null && usablePings.test(nextPingIndex)) {
         seeds.add(new Seed(nextPingIndex, depthRange));
      }
   }
}
