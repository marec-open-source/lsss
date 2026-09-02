package no.imr.korona.data.ping;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.tools.Max;
import no.imr.tools.Min;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;
import no.imr.tools.time.TimeUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.NavigableMap;

/**
 * Represents a half open interval of consecutive pings.
 * A ping range includes the lower limit, but excludes the upper limit.
 */
public final class PingRange extends Range<PingIndex> implements no.marec.lsss.api.data.PingRange {
   /**
    * An instance of the empty PingRange.
    */
   public static final PingRange EMPTY_RANGE = new PingRange(EmptyPingIndex.INSTANCE, EmptyPingIndex.INSTANCE);

   private PingRange(PingIndex begin, PingIndex end) {
      super(begin, end);
   }

   public static PingRange of(PingIndex a, PingIndex b) {
      return a.getPingNumber() <= b.getPingNumber() ? new PingRange(a, b) : EMPTY_RANGE;
   }

   public static PingRange of(Range<PingIndex> range) {
      return new PingRange(range.begin(), range.end());
   }

   public static PingRange ofUnsorted(PingIndex a, PingIndex b) {
      if (a.getPingNumber() <= b.getPingNumber()) {
         return new PingRange(a, b);
      } else {
         return new PingRange(b, a);
      }
   }

   public static PingRange ofSinglePing(PingIndex pingIndex, PingContainer pingContainer) {
      return new PingRange(pingIndex, pingContainer.nextOrSame(pingIndex));
   }

   public static PingRange from(Collection<PingIndex> pingIndices, PingContainer pingContainer) {
      PingRangeBuilder builder = new PingRangeBuilder();
      pingIndices.forEach(builder::add);
      return builder.build(pingContainer);
   }

   public static PingRange from(NavigableMap<PingIndex, ?> map, PingContainer pingContainer) {
      if (map.isEmpty()) {
         return EMPTY_RANGE;
      }
      PingIndex first = map.firstKey();
      PingIndex last = map.lastKey();
      return of(first, pingContainer.nextOrSame(last));
   }

   @Override
   public String toString() {
      return "[" + begin().getPingNumber() + ", " + end().getPingNumber() + ")";
   }

   public int getPingCount() {
      return (int) (end().getPingNumber() - begin().getPingNumber());
   }

   public double getVesselDistance() {
      return end().getVesselDistance() - begin().getVesselDistance();
   }

   public Range<Instant> toTimeRange() {
      return new DefaultRange<>(begin().getInstant(), end().getInstant());
   }

   public double getSeconds() {
      return TimeUtils.toSeconds(begin().getInstant(), end().getInstant());
   }

   public Duration getDuration() {
      return begin().getInstant().until(end().getInstant());
   }

   public String getDurationString() {
      return TimeUtils.getDurationString(getDuration());
   }

   public boolean contains(double value, PingMapping pingMapping) {
      return value >= pingMapping.valueOf(begin()) && value < pingMapping.valueOf(end());
   }

   public boolean contains(Ping ping) {
      return containsPingNumber(ping.getPingNumber());
   }

   public boolean containsPingNumber(long pingNumber) {
      return pingNumber >= begin().getPingNumber() && pingNumber < end().getPingNumber();
   }

   public boolean containsInstant(Instant instant) {
      return instant.compareTo(begin().getInstant()) >= 0 && instant.compareTo(end().getInstant()) < 0;
   }

   public boolean intersectsVesselDistanceRange(double beginVesselDistance, double endVesselDistance) {
      double a = Math.max(begin().getVesselDistance(), beginVesselDistance);
      double b = Math.min(end().getVesselDistance(), endVesselDistance);
      return a < b;
   }

   public PingRange intersection(Range<PingIndex> pingRange) {
      PingIndex a = Max.of(begin(), pingRange.begin());
      PingIndex b = Min.of(end(), pingRange.end());
      return of(a, b);
   }

   /**
    * Returns the union with another range. If there is a gap between the two ranges then the returned range
    * will also contain that gap.
    *
    * @param pingRange another range
    * @return the union
    */
   public PingRange union(PingRange pingRange) {
      if (isEmpty()) {
         return pingRange;
      }
      if (pingRange.isEmpty()) {
         return this;
      }
      PingIndex a = Min.of(begin(), pingRange.begin());
      PingIndex b = Max.of(end(), pingRange.end());
      return of(a, b);
   }
}
