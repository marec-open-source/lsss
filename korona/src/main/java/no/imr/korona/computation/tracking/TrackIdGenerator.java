package no.imr.korona.computation.tracking;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import org.jspecify.annotations.Nullable;

/**
 * Creates id based on ping, sample and channel.
 * <p>
 * The 32-bit id is composed as
 * <pre>
 *    ppppppuuuuuuuusssssssssssssssccc   &lt;- 32 bits
 * </pre>
 * where
 * <ul>
 * <li>The p bits are ping index reversed relative to beginning of the file</li>
 * <li>The s bits are the sample index</li>
 * <li>The c bits are the channel index (0 based)</li>
 * <li>The u bits are unused</li>
 * </ul>
 */
public final class TrackIdGenerator {
   private final int channel;
   private final long firstPingNumber;
   private final int channelShift;

   TrackIdGenerator(int channel, @Nullable Ping firstPing) {
      this.channel = channel;
      if (firstPing != null) {
         firstPingNumber = firstPing.getPingNumber();
         channelShift = computeChannelShift(firstPing.getRawFileConfiguration());
      } else {
         firstPingNumber = 0;
         channelShift = 0;
      }
   }

   public TrackIdGenerator(int channel, PingIndex firstPingIndex, RawFileConfiguration rawFileConfiguration) {
      this.channel = channel;
      firstPingNumber = firstPingIndex.getPingNumber();
      channelShift = computeChannelShift(rawFileConfiguration);
   }

   private static int computeChannelShift(RawFileConfiguration rawFileConfiguration) {
      return Integer.highestOneBit(rawFileConfiguration.getTransducerCount() - 1);
   }

   public int nextId(Ping ping, float range) {
      ChannelData channelData = ping.getChannelData(channel);
      int sampleIndex = channelData != null ? channelData.rangeToSampleIndex(range) : 0;
      return nextId(ping.getPingNumber(), sampleIndex);
   }

   public int nextId(long pingNumber, int sampleIndex) {
      int p = (int) (pingNumber - firstPingNumber);
      return Integer.reverse(p) | (sampleIndex << channelShift) | (channel - 1);
   }
}
