package no.imr.korona.data.util;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.track.SegmentData;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Removes {@link ChannelData} with zero bottom depth.
 */
public final class DiscardZeroDepthSegmentData extends SegmentData {
   private final SegmentData segmentData;
   private final BooleanSupplier discard;

   public DiscardZeroDepthSegmentData(SegmentData segmentData, BooleanSupplier discard) {
      this.segmentData = segmentData;
      this.discard = discard;
   }

   public SegmentData getSegmentData() {
      return segmentData;
   }

   @Override
   public List<? extends PingIndex> getPingIndices() {
      return segmentData.getPingIndices();
   }

   @Override
   public @Nullable WrapAround getWrapAround() {
      return segmentData.getWrapAround();
   }

   @Override
   public @Nullable String getInfo() {
      return segmentData.getInfo();
   }

   @Override
   public List<Bot0Datagram> getBot0Datagrams() {
      return segmentData.getBot0Datagrams();
   }

   @Override
   public void close() throws IOException {
      segmentData.close();
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return segmentData.getPingConfiguration();
   }

   @Override
   public PingData loadPingData(PingIndex pingIndex, AsyncHandle asyncHandle) throws IOException {
      PingData pingData = segmentData.loadPingData(pingIndex, asyncHandle);
      if (discard.getAsBoolean()) {
         discardZeroDepth(pingIndex, pingData);
      }
      return pingData;
   }

   private void discardZeroDepth(PingIndex pingIndex, PingData pingData) {
      double[] channelDepths = getBot0Datagram(pingIndex).getChannelDepths();
      @Nullable ChannelData[] channelDatas = pingData.getChannelDatas();
      for (int channelIndex = 0; channelIndex < channelDepths.length; channelIndex++) {
         if (channelDepths[channelIndex] == 0) {
            ChannelData channelData = channelDatas[channelIndex];
            if (channelData != null) {
               pingData.remove(channelData);
               pingData.add(new DiscardedChannelData(channelData));
            }
         }
      }
   }

   @Override
   public void onLoadPingData(Ping ping, PingData pingData) {
      segmentData.onLoadPingData(ping, pingData);
   }
}
