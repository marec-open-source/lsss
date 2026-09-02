package no.imr.lsss.util;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.LSSS;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.LongSupplier;

/**
 * Converts ping indexes between the LSSS data set and another data set.
 */
public final class PingIndexConverter {
   private final LSSS lsss;
   private final DataManager otherDataManager;
   private final LongSupplier timeOffsetAsNanos;

   public PingIndexConverter(LSSS lsss, DataManager otherDataManager, LongSupplier timeOffsetAsNanos) {
      this.lsss = lsss;
      this.otherDataManager = otherDataManager;
      this.timeOffsetAsNanos = timeOffsetAsNanos;
   }

   private DataFileSet getLsssDataFileSet() {
      return lsss.getDataManager().getDataFileSet();
   }

   private DataFileSet getOtherDataFileSet() {
      return otherDataManager.getDataFileSet();
   }

   public LongSupplier getTimeOffsetAsNanos() {
      return timeOffsetAsNanos;
   }

   public Instant lsssTimeToOtherTime(Instant lsssTime) {
      return lsssTime.minusNanos(timeOffsetAsNanos.getAsLong());
   }

   public Range<Instant> lsssTimeToOtherTime(Range<Instant> lsssTimeRange) {
      return new DefaultRange<>(
            lsssTimeToOtherTime(lsssTimeRange.begin()),
            lsssTimeToOtherTime(lsssTimeRange.end())
      );
   }

   public Instant otherTimeToLsssTime(Instant otherTime) {
      return otherTime.plusNanos(timeOffsetAsNanos.getAsLong());
   }

   public PingIndex lsssToClosestOther(PingIndex lsssPingIndex) {
      Instant otherTime = lsssTimeToOtherTime(lsssPingIndex.getInstant());
      return getOtherDataFileSet().getClosestPingIndex(PingMapping.instantToTimeValue(otherTime), PingMapping.TIME);
   }

   public PingRange lsssToClosestOther(Range<PingIndex> lsssPingRange) {
      if (lsssPingRange.isEmpty()) {
         return PingRange.EMPTY_RANGE;
      }
      PingIndex otherBegin = lsssToClosestOther(lsssPingRange.begin());
      PingIndex otherEnd = lsssToClosestOther(lsssPingRange.end());
      if (otherEnd.equals(otherBegin)) {
         otherEnd = getOtherDataFileSet().nextOrSame(otherEnd);
      }
      return PingRange.of(otherBegin, otherEnd);
   }

   public @Nullable PingIndex lsssToContainingOther(PingIndex lsssPingIndex) {
      Instant otherTime = lsssTimeToOtherTime(lsssPingIndex.getInstant());
      return getOtherDataFileSet().getContainingPingIndex(PingMapping.instantToTimeValue(otherTime), PingMapping.TIME);
   }

   public PingIndex otherToClosestLsss(PingIndex otherPingIndex) {
      Instant lsssTime = otherTimeToLsssTime(otherPingIndex.getInstant());
      return getLsssDataFileSet().getClosestPingIndex(PingMapping.instantToTimeValue(lsssTime), PingMapping.TIME);
   }

   public PingRange otherToClosestLsss(Range<PingIndex> otherPingRange) {
      if (otherPingRange.isEmpty()) {
         return PingRange.EMPTY_RANGE;
      }
      PingIndex lsssBegin = otherToClosestLsss(otherPingRange.begin());
      PingIndex lsssEnd = otherToClosestLsss(otherPingRange.end());
      if (lsssEnd.equals(lsssBegin)) {
         lsssEnd = getLsssDataFileSet().nextOrSame(lsssEnd);
      }
      return PingRange.of(lsssBegin, lsssEnd);
   }

   public @Nullable PingIndex otherToContainingLsss(PingIndex otherPingIndex) {
      Instant lsssTime = otherTimeToLsssTime(otherPingIndex.getInstant());
      return getLsssDataFileSet().getContainingPingIndex(PingMapping.instantToTimeValue(lsssTime), PingMapping.TIME);
   }
}
