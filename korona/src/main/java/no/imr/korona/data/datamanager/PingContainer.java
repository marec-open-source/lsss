package no.imr.korona.data.datamanager;

import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.range.Range;
import org.jspecify.annotations.Nullable;

import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Interface for data structures containing pings.
 */
public interface PingContainer {
   PingConfiguration getPingConfiguration();

   default RawFileConfiguration getRawFileConfiguration() {
      return getPingConfiguration().getRawFileConfiguration();
   }

   default int getTransducerCount() {
      return getRawFileConfiguration().getTransducerCount();
   }

   default int firstChannelClosestTo(float frequency) {
      return getRawFileConfiguration().firstChannelClosestTo(frequency);
   }

   default int firstChannelClosestTo(float frequency, float maxDiff) {
      return getRawFileConfiguration().firstChannelClosestTo(frequency, maxDiff);
   }

   /**
    * Get the frequency for a channel.
    *
    * @param channel a channel
    * @return the frequency in Hz, or -1 if not available
    */
   default float getFrequency(int channel) {
      RawFileConfiguration rawFileConfiguration = getRawFileConfiguration();
      if (channel < 1 || channel > rawFileConfiguration.getTransducerCount()) {
         return -1;
      } else {
         return rawFileConfiguration.getTransducers().get(channel - 1).getFrequency();
      }
   }

   default <T extends PingItem> @Nullable T getConfigurationItem(Class<T> clazz) {
      return getPingConfiguration().getConfigurationItem(clazz);
   }

   PingRange getTotalRange();

   default @Nullable PingIndex getPingIndexOrNull(long pingNumber) {
      PingIndex pingIndex = getPingIndexOrNullExcludingEnd(pingNumber);
      if (pingIndex != null) {
         return pingIndex;
      }
      PingIndex end = getTotalRange().end();
      if (pingNumber == end.getPingNumber()) {
         return end;
      }
      return null;
   }

   default @Nullable PingIndex getPingIndexOrNullExcludingEnd(long pingNumber) {
      return getContainingPingIndex(pingNumber, PingMapping.NUMBER);
   }

   default PingIndex getPingIndexClamped(long pingNumber) {
      PingIndex pingIndex = getPingIndexOrNullExcludingEnd(pingNumber);
      if (pingIndex != null) {
         return pingIndex;
      }
      PingRange totalRange = getTotalRange();
      return pingNumber < totalRange.begin().getPingNumber()
            ? totalRange.begin()
            : totalRange.end();
   }

   default PingIndex getPingIndex(long pingNumber) {
      PingIndex pingIndex = getPingIndexOrNull(pingNumber);
      if (pingIndex == null) {
         throw new IllegalArgumentException(Long.toString(pingNumber));
      }
      return pingIndex;
   }

   default @Nullable PingIndex previousOrNull(PingIndex pingIndex) {
      return getPingIndexOrNull(pingIndex.getPingNumber() - 1);
   }

   default PingIndex previousOrSame(PingIndex pingIndex) {
      PingIndex next = previousOrNull(pingIndex);
      return next != null ? next : pingIndex;
   }

   default @Nullable PingIndex nextOrNull(PingIndex pingIndex) {
      return getPingIndexOrNull(pingIndex.getPingNumber() + 1);
   }

   default PingIndex nextOrSame(PingIndex pingIndex) {
      PingIndex next = nextOrNull(pingIndex);
      return next != null ? next : pingIndex;
   }

   PingIndex getClosestPingIndex(double value, PingMapping pingMapping);

   default PingIndex getClosestPingIndex(PingIndex reference, double distance, PingMapping pingMapping) {
      return getClosestPingIndex(pingMapping.valueOf(reference) + distance, pingMapping);
   }

   @Nullable PingIndex getContainingPingIndex(double value, PingMapping pingMapping);

   default @Nullable PingIndex getContainingPingIndex(PingIndex reference, double distance, PingMapping pingMapping) {
      return getContainingPingIndex(pingMapping.valueOf(reference) + distance, pingMapping);
   }

   default PingIndex getClampedContainingPingIndex(double value, PingMapping pingMapping) {
      PingIndex pingIndex = getContainingPingIndex(value, pingMapping);
      if (pingIndex != null) {
         return pingIndex;
      }
      PingRange totalRange = getTotalRange();
      if (value < pingMapping.valueOf(totalRange.begin())) {
         return totalRange.begin();
      }
      return previousOrSame(totalRange.end());
   }

   default Iterable<PingIndex> getPingIndices() {
      return getPingIndices(getTotalRange());
   }

   default Iterable<PingIndex> getPingIndices(Range<PingIndex> pingRange) {
      return () -> getPingIndexStream(pingRange).iterator();
   }

   default Stream<PingIndex> getPingIndexStream(Range<PingIndex> pingRange) {
      if (pingRange.isEmpty()) {
         return Stream.empty();
      }
      return LongStream.range(pingRange.begin().getPingNumber(), pingRange.end().getPingNumber())
            .mapToObj(this::getPingIndex);
   }
}
