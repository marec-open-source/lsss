package no.imr.tools.range;

/**
 * Default implementation of a range.
 */
public final class DefaultRange<E extends Comparable<? super E>> extends Range<E> {
   public DefaultRange(E begin, E end) {
      super(begin, end);
   }
}
