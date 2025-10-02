package no.imr.tools.range;

/**
 * Implementation of a RangeSet using a {@link ArrayRangeMap}.
 */
public final class ArrayRangeSet<K extends Comparable<? super K>> extends RangeMapBasedRangeSet<K> {
   public ArrayRangeSet() {
      super(new ArrayRangeMap<>());
   }

   public ArrayRangeSet(Range<K> range) {
      this();

      add(range);
   }
}
