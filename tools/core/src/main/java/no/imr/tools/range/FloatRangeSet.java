package no.imr.tools.range;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A set of float ranges.
 *
 * @see FloatRange
 */
public final class FloatRangeSet implements Iterable<FloatRange> {
   private static final FloatRangeSet EMPTY = new FloatRangeSet(List.of());

   private static final Comparator<FloatRange> FLOAT_RANGE_COMPARATOR = (o1, o2) -> Float.compare(o1.min(), o2.min());

   private final List<FloatRange> floatRanges;

   /**
    * Private constructor.
    *
    * @param ranges immutable list of non-empty, disjoint, sorted ranges
    */
   private FloatRangeSet(List<FloatRange> ranges) {
      floatRanges = ranges;
   }

   public static FloatRangeSet of() {
      return EMPTY;
   }

   public static FloatRangeSet of(FloatRange range) {
      if (range.isEmpty()) {
         return EMPTY;
      }
      return new FloatRangeSet(List.of(range));
   }

   public static FloatRangeSet of(List<FloatRange> ranges) {
      return switch (ranges.size()) {
         case 0 -> EMPTY;
         case 1 -> of(ranges.getFirst());
         default -> {
            if (containsNonEmptyDisjointSortedRanges(ranges)) {
               yield ofNonEmptyDisjointSortedRanges(ranges);
            }
            List<FloatRange> sorted = new ArrayList<>(ranges.size());
            for (FloatRange range : ranges) {
               if (!range.isEmpty()) {
                  sorted.add(range);
               }
            }
            sorted.sort(FLOAT_RANGE_COMPARATOR);
            List<FloatRange> result = new ArrayList<>(sorted.size());
            int i = 0;
            while (i < sorted.size()) {
               FloatRange r = sorted.get(i++);
               while (i < sorted.size() && sorted.get(i).min() <= r.max()) {
                  r = r.union(sorted.get(i++));
               }
               result.add(r);
            }
            yield ofNonEmptyDisjointSortedRanges(result);
         }
      };
   }

   static boolean containsNonEmptyDisjointSortedRanges(List<FloatRange> ranges) {
      FloatRange previous = null;
      for (FloatRange range : ranges) {
         if (range.isEmpty() || (previous != null && previous.max() >= range.min())) {
            return false;
         }
         previous = range;
      }
      return true;
   }

   private static FloatRangeSet ofNonEmptyDisjointSortedRanges(List<FloatRange> ranges) {
      return switch (ranges.size()) {
         case 0 -> EMPTY;
         case 1 -> new FloatRangeSet(List.of(ranges.get(0)));
         case 2 -> new FloatRangeSet(List.of(ranges.get(0), ranges.get(1)));
         case 3 -> new FloatRangeSet(List.of(ranges.get(0), ranges.get(1), ranges.get(2)));
         case 4 -> new FloatRangeSet(List.of(ranges.get(0), ranges.get(1), ranges.get(2), ranges.get(3)));
         default -> new FloatRangeSet(List.copyOf(ranges));
      };
   }

   @Override
   public String toString() {
      return floatRanges.stream()
            .map(FloatRange::toString)
            .collect(Collectors.joining(", ", "{", "}"));
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      return obj instanceof FloatRangeSet that
            && floatRanges.equals(that.floatRanges);
   }

   @Override
   public int hashCode() {
      return floatRanges.hashCode();
   }

   @Override
   public Iterator<FloatRange> iterator() {
      return floatRanges.iterator();
   }

   public List<FloatRange> getFloatRanges() {
      return floatRanges;
   }

   public boolean isEmpty() {
      return floatRanges.isEmpty();
   }

   public FloatRange getBoundingRange() {
      return switch (floatRanges.size()) {
         case 0 -> FloatRange.EMPTY_RANGE;
         case 1 -> floatRanges.getFirst();
         default -> FloatRange.of(floatRanges.getFirst().min(), floatRanges.getLast().max());
      };
   }

   public FloatRangeSet add(FloatRange range) {
      return add(of(range));
   }

   public FloatRangeSet add(FloatRangeSet otherRangeSet) {
      if (isEmpty()) {
         return otherRangeSet;
      }
      if (otherRangeSet.isEmpty()) {
         return this;
      }
      List<FloatRange> ranges1 = floatRanges;
      List<FloatRange> ranges2 = otherRangeSet.getFloatRanges();
      List<FloatRange> result = new ArrayList<>(ranges1.size() + ranges2.size());
      int i1 = 0;
      int i2 = 0;
      while (true) {
         if (i1 == ranges1.size()) {
            result.addAll(ranges2.subList(i2, ranges2.size()));
            break;
         }
         if (i2 == ranges2.size()) {
            result.addAll(ranges1.subList(i1, ranges1.size()));
            break;
         }
         FloatRange r = ranges1.get(i1).min() < ranges2.get(i2).min()
               ? ranges1.get(i1++)
               : ranges2.get(i2++);
         while (true) {
            if (i1 < ranges1.size() && ranges1.get(i1).min() <= r.max()) {
               r = r.union(ranges1.get(i1++));
               continue;
            }
            if (i2 < ranges2.size() && ranges2.get(i2).min() <= r.max()) {
               r = r.union(ranges2.get(i2++));
               continue;
            }
            break;
         }
         result.add(r);
      }
      return ofNonEmptyDisjointSortedRanges(result);
   }

   public FloatRangeSet expandEachRange(float delta) {
      List<FloatRange> ranges = floatRanges;
      return switch (ranges.size()) {
         case 0 -> this;
         case 1 -> of(ranges.getFirst().expand(delta));
         default -> {
            if (delta < 0) {
               List<FloatRange> shrunkRanges = ranges.stream()
                     .map(r -> r.expand(delta))
                     .filter(Predicate.not(FloatRange::isEmpty))
                     .toList();
               yield ofNonEmptyDisjointSortedRanges(shrunkRanges);
            }
            List<FloatRange> result = new ArrayList<>(ranges.size());
            FloatRange r = ranges.getFirst().expand(delta);
            for (int i = 1; i < ranges.size(); i++) {
               FloatRange next = ranges.get(i).expand(delta);
               if (r.max() >= next.min()) {
                  r = r.union(next);
               } else {
                  result.add(r);
                  r = next;
               }
            }
            result.add(r);
            yield ofNonEmptyDisjointSortedRanges(result);
         }
      };
   }

   public FloatRangeSet xor(FloatRangeSet otherRangeSet) {
      FloatRangeSet union = add(otherRangeSet);
      FloatRangeSet intersection = intersection(otherRangeSet);
      return union.subtract(intersection);
   }

   public FloatRangeSet subtract(FloatRange range) {
      return subtract(of(range));
   }

   public FloatRangeSet subtract(FloatRangeSet otherRangeSet) {
      if (isEmpty() || otherRangeSet.isEmpty()) {
         return this;
      }
      List<FloatRange> ranges1 = floatRanges;
      List<FloatRange> ranges2 = otherRangeSet.getFloatRanges();
      List<FloatRange> result = new ArrayList<>(ranges1.size());
      FloatRange r = ranges1.getFirst();
      int i1 = 1;
      int i2 = 0;
      while (true) {
         if (i2 == ranges2.size()) {
            result.add(r);
            result.addAll(ranges1.subList(i1, ranges1.size()));
            break;
         }
         FloatRange r2 = ranges2.get(i2);
         if (r2.max() <= r.min()) {
            i2++;
            continue;
         }
         if (r2.min() >= r.max()) {
            result.add(r);
         } else {
            if (r.min() < r2.min()) {
               result.add(FloatRange.of(r.min(), r2.min()));
            }
            if (r2.max() < r.max()) {
               r = FloatRange.of(r2.max(), r.max());
               i2++;
               continue;
            }
         }
         if (i1 == ranges1.size()) {
            break;
         }
         r = ranges1.get(i1++);
      }
      return ofNonEmptyDisjointSortedRanges(result);
   }

   public FloatRangeSet complement() {
      List<FloatRange> result = new ArrayList<>(floatRanges.size() + 1);
      float lower = Float.NEGATIVE_INFINITY;
      for (FloatRange range : floatRanges) {
         if (lower < range.min()) {
            result.add(FloatRange.of(lower, range.min()));
         }
         lower = range.max();
      }
      if (lower < Float.POSITIVE_INFINITY) {
         result.add(FloatRange.of(lower, Float.POSITIVE_INFINITY));
      }
      return ofNonEmptyDisjointSortedRanges(result);
   }

   public FloatRangeSet intersection(FloatRange range) {
      return intersection(of(range));
   }

   public FloatRangeSet intersection(FloatRangeSet otherRangeSet) {
      if (isEmpty() || otherRangeSet.isEmpty()) {
         return EMPTY;
      }
      return subtract(otherRangeSet.complement());
   }

   public boolean intersects(FloatRange range) {
      for (FloatRange floatRange : floatRanges) {
         if (floatRange.intersects(range)) {
            return true;
         }
      }
      return false;
   }

   public boolean intersects(FloatRangeSet otherRangeSet) {
      List<FloatRange> ranges = floatRanges;
      if (ranges.isEmpty()) {
         return false;
      }
      int i = 0;
      FloatRange r = ranges.getFirst();
      for (FloatRange otherRange : otherRangeSet) {
         while (r.max() <= otherRange.min()) {
            i++;
            if (i == ranges.size()) {
               return false;
            }
            r = ranges.get(i);
         }
         if (r.min() < otherRange.max()) {
            return true;
         }
      }
      return false;
   }

   public boolean contains(float value) {
      for (FloatRange floatRange : floatRanges) {
         if (floatRange.contains(value)) {
            return true;
         }
      }
      return false;
   }

   public boolean contains(FloatRange range) {
      for (FloatRange floatRange : floatRanges) {
         if (floatRange.contains(range)) {
            return true;
         }
      }
      return false;
   }

   public boolean contains(FloatRangeSet otherRangeSet) {
      List<FloatRange> ranges = floatRanges;
      if (ranges.isEmpty()) {
         return otherRangeSet.isEmpty();
      }
      int i = 0;
      FloatRange r = ranges.getFirst();
      for (FloatRange otherRange : otherRangeSet) {
         while (r.max() < otherRange.max()) {
            i++;
            if (i == ranges.size()) {
               return false;
            }
            r = ranges.get(i);
         }
         if (otherRange.min() < r.min()) {
            return false;
         }
      }
      return true;
   }

   public float clamp(float value) {
      float d = Float.MAX_VALUE;
      float bestClamped = Float.NaN;
      for (FloatRange floatRange : floatRanges) {
         float clamped = floatRange.clamp(value);
         float dist = Math.abs(clamped - value);
         if (dist < d) {
            d = dist;
            bestClamped = clamped;
         }
      }
      return bestClamped;
   }
}
