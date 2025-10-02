package no.imr.lsss.modules.interpretation;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.range.DoubleRange;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.stream.LongStream;
import java.util.stream.Stream;

final class GridIndexFinder {
   private final DataFileSet dataFileSet;
   private final double horizontalResolution;
   private final PingMapping pingMapping;

   private final long beginGridX;
   private final long endGridX;

   GridIndexFinder(DataFileSet dataFileSet, PingRange pingRange, double horizontalResolution, PingMapping pingMapping) {
      this.dataFileSet = dataFileSet;
      this.horizontalResolution = horizontalResolution;
      this.pingMapping = pingMapping;

      long x0 = (long) Math.floor(pingIndexToGridIndex(pingRange.begin()));
      PingIndex beginPingIndex = gridIndexToPingIndex(x0);
      if (beginPingIndex == null || beginPingIndex.getPingNumber() < pingRange.begin().getPingNumber()) {
         x0++;
      }
      beginGridX = x0;

      long x1 = (long) Math.ceil(pingIndexToGridIndex(pingRange.end()));
      PingIndex endPingIndex = gridIndexToPingIndex(x1);
      if (endPingIndex == null || endPingIndex.getPingNumber() > pingRange.end().getPingNumber()) {
         x1--;
      }
      endGridX = x1;
   }

   private double pingIndexToGridIndex(PingIndex pingIndex) {
      return pingMapping.valueOf(pingIndex) / horizontalResolution;
   }

   private @Nullable PingIndex gridIndexToPingIndex(long gridX) {
      return dataFileSet.getContainingPingIndex(gridX * horizontalResolution, pingMapping);
   }

   private @Nullable GridColumnInterval gridIndexToGridColumnInterval(long gridX) {
      PingIndex beginPingIndex = gridIndexToPingIndex(gridX);
      PingIndex endPingIndex = gridIndexToPingIndex(gridX + 1);
      if (beginPingIndex == null || endPingIndex == null) {
         // Grid column not completely contained in data.
         return null;
      }
      PingRange pingRange = PingRange.of(beginPingIndex, endPingIndex);
      if (pingRange.isEmpty()) {
         // Entire grid column inside one single ping.
         return null;
      }
      DoubleRange valueRange = DoubleRange.of(gridX * horizontalResolution, (gridX + 1) * horizontalResolution);
      return new GridColumnInterval(horizontalResolution, valueRange, pingRange);
   }

   Stream<GridColumnInterval> gridColumnIntervals() {
      return LongStream.range(beginGridX, endGridX)
            .mapToObj(this::gridIndexToGridColumnInterval)
            .filter(Objects::nonNull);
   }
}
