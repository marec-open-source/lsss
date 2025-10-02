package no.imr.korona.data.ping;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.tools.concurrent.AsyncHandle;

import java.io.IOException;

/**
 * The source for a {@link ReloadablePing}.
 */
public interface ReloadablePingSource {
   PingConfiguration getPingConfiguration();

   Bot0Datagram getBot0Datagram(PingIndex pingIndex);

   /**
    * Loads a specified ping data.
    *
    * @param pingIndex   the index of the ping data to load
    * @param asyncHandle a handle that can be used to cancel the loading
    * @return the loaded ping data, possibly incomplete if cancelled
    * @throws IOException if some IO error occurs
    */
   PingData loadPingData(PingIndex pingIndex, AsyncHandle asyncHandle) throws IOException;

   default boolean isDataLoadingCancelled() {
      return false;
   }

   default void onLoadPingData(Ping ping, PingData pingData) {
   }
}
