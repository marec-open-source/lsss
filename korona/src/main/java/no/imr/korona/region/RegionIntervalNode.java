package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.range.FloatRangeSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

sealed interface RegionIntervalNode<R extends Region> {
   FloatRangeSet depthRanges(PingIndex pingIndex);

   Stream<R> regions(PingIndex pingIndex);

   record Leaf<R extends Region>(
         List<R> regions
   ) implements RegionIntervalNode<R> {

      @Override
      public FloatRangeSet depthRanges(PingIndex pingIndex) {
         return regions.stream()
               .map(region -> region.getDepthRanges(pingIndex))
               .reduce(FloatRangeSet.of(), FloatRangeSet::add);
      }

      @Override
      public Stream<R> regions(PingIndex pingIndex) {
         return regions.stream()
               .filter(region -> region.contains(pingIndex));
      }
   }

   record Branch<R extends Region>(
         RegionIntervalNode<R> left,
         PingIndex pivot,
         RegionIntervalNode<R> right
   ) implements RegionIntervalNode<R> {

      @Override
      public FloatRangeSet depthRanges(PingIndex pingIndex) {
         return leftOrRight(pingIndex).depthRanges(pingIndex);
      }

      @Override
      public Stream<R> regions(PingIndex pingIndex) {
         return leftOrRight(pingIndex).regions(pingIndex);
      }

      private RegionIntervalNode<R> leftOrRight(PingIndex pingIndex) {
         return pingIndex.getPingNumber() < pivot.getPingNumber() ? left : right;
      }
   }

   static <R extends Region> RegionIntervalNode<R> build(PingContainer pingContainer, PingRange pingRange, Collection<R> regions) {
      // Using Exec.FORK_JOIN_POOL might lead to a deadlock.

      if (pingRange.getPingCount() < 100 || regions.size() < 20) {
         return new Leaf<>(List.copyOf(regions));
      }
      List<R> leftRegions = new ArrayList<>();
      List<R> rightRegions = new ArrayList<>();
      PingIndex pivot = pingContainer.getPingIndex((pingRange.begin().getPingNumber() + pingRange.end().getPingNumber()) / 2);
      PingRange leftPingRange = PingRange.of(pingRange.begin(), pivot);
      PingRange rightPingRange = PingRange.of(pivot, pingRange.end());
      for (R region : regions) {
         if (region.intersectsPingRange(leftPingRange)) {
            leftRegions.add(region);
         }
         if (region.intersectsPingRange(rightPingRange)) {
            rightRegions.add(region);
         }
      }
      RegionIntervalNode<R> leftNode = build(pingContainer, leftPingRange, leftRegions);
      RegionIntervalNode<R> rightNode = build(pingContainer, rightPingRange, rightRegions);
      return new Branch<>(leftNode, pivot, rightNode);
   }
}
