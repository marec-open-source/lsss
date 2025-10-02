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

import java.util.function.LongSupplier;

/**
 * Converts ping indexes between the LSSS data set and another data set.
 */
public final class PingIndexConverter {
   private final LSSS lsss;
   private final DataManager otherDataManager;
   private final LongSupplier ntDateOffset;

   public PingIndexConverter(LSSS lsss, DataManager otherDataManager, LongSupplier ntDateOffset) {
      this.lsss = lsss;
      this.otherDataManager = otherDataManager;
      this.ntDateOffset = ntDateOffset;
   }

   private DataFileSet getLsssDataFileSet() {
      return lsss.getDataManager().getDataFileSet();
   }

   private DataFileSet getOtherDataFileSet() {
      return otherDataManager.getDataFileSet();
   }

   public LongSupplier getNTDateOffset() {
      return ntDateOffset;
   }

   public long lsssNTDateToOtherNTDate(long lsssNTDate) {
      return lsssNTDate - ntDateOffset.getAsLong();
   }

   public Range<Long> lsssNTDateToOtherNTDate(Range<Long> lsssNTDateRange) {
      return new DefaultRange<>(
            lsssNTDateToOtherNTDate(lsssNTDateRange.begin()),
            lsssNTDateToOtherNTDate(lsssNTDateRange.end()));
   }

   public long otherNTDateToLsssNTDate(long otherNTDate) {
      return otherNTDate + ntDateOffset.getAsLong();
   }

   public PingIndex lsssToClosestOther(PingIndex lsssPingIndex) {
      long otherNTDate = lsssNTDateToOtherNTDate(lsssPingIndex.getNTDate());
      return getOtherDataFileSet().getClosestPingIndex(PingMapping.ntDateToTimeValue(otherNTDate), PingMapping.TIME);
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
      long otherNTDate = lsssNTDateToOtherNTDate(lsssPingIndex.getNTDate());
      return getOtherDataFileSet().getContainingPingIndex(PingMapping.ntDateToTimeValue(otherNTDate), PingMapping.TIME);
   }

   public PingIndex otherToClosestLsss(PingIndex otherPingIndex) {
      long lsssNTDate = otherNTDateToLsssNTDate(otherPingIndex.getNTDate());
      return getLsssDataFileSet().getClosestPingIndex(PingMapping.ntDateToTimeValue(lsssNTDate), PingMapping.TIME);
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
      long lsssNTDate = otherNTDateToLsssNTDate(otherPingIndex.getNTDate());
      return getLsssDataFileSet().getContainingPingIndex(PingMapping.ntDateToTimeValue(lsssNTDate), PingMapping.TIME);
   }
}
