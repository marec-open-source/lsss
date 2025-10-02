package no.imr.tools.range;

public final class CopyOnWriteRangeSet<K extends Comparable<? super K>> extends RangeMapBasedRangeSet<K> {
   public CopyOnWriteRangeSet() {
      super(new CopyOnWriteRangeMap<>());
   }
}
