package no.imr.korona.data.track;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.ReloadablePingSource;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * Interface to loading the ping data of a {@link Segment}.
 */
public abstract class SegmentData implements ReloadablePingSource, Closeable {
   protected SegmentData() {
   }

   public RawFileConfiguration getRawFileConfiguration() {
      return getPingConfiguration().getRawFileConfiguration();
   }

   /**
    * Returns all ping indices in this segment.
    *
    * @return all ping indices
    */
   public abstract List<? extends PingIndex> getPingIndices();

   public List<PingItem> getOtherIdxPingItems() {
      return List.of();
   }

   public abstract @Nullable WrapAround getWrapAround();

   /**
    * Info about this segment.
    *
    * @return an HTML string
    */
   public @Nullable String getInfo() {
      return null;
   }

   /**
    * Returns all Bot0Datagrams in this segment.
    *
    * @return all Bot0Datagrams in this segment
    */
   public abstract List<Bot0Datagram> getBot0Datagrams();

   /**
    * Loads a specified ping.
    *
    * @param pingIndex   the index of the ping to load
    * @param asyncHandle a handle that can be used to cancel the loading
    * @return the loaded ping, possibly incomplete if cancelled
    * @throws IOException if some IO error occurs
    */
   public Ping loadPing(PingIndex pingIndex, AsyncHandle asyncHandle) throws IOException {
      PingData pingData = loadPingData(pingIndex, asyncHandle);
      return new DefaultPing(pingIndex, getBot0Datagram(pingIndex), pingData);
   }

   /**
    * Converts a ping number to a ping index number relative to the start of this segment.
    *
    * @param pingNumber a ping number
    * @return the corresponding index number relative to the start of this segment
    */
   public int pingNumberToIndex(long pingNumber) {
      return (int) (pingNumber - getPingIndices().getFirst().getPingNumber());
   }

   /**
    * Returns the Bot0Datagram for a given PingIndex.
    *
    * @param pingIndex a ping index
    * @return the Bot0Datagram
    */
   @Override
   public Bot0Datagram getBot0Datagram(PingIndex pingIndex) {
      int i = pingNumberToIndex(pingIndex.getPingNumber());
      if (i < 0 || i >= getPingIndices().size()) {
         throw new IllegalArgumentException("Ping " + pingIndex.getPingNumber());
      }
      return getBot0Datagrams().get(i);
   }
}
