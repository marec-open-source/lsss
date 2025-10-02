package no.imr.korona.data.ping;

import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;

/**
 * A source for getting pings.
 */
public interface PingSource extends Closeable {
   PingConfiguration getPingConfiguration();

   /**
    * Returns the next ping.
    *
    * @param asyncHandle for cancelling the creation of the next ping
    * @return the next ping or {@code null} if no more pings are available
    * @throws IOException if some IO error occurs
    */
   @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException;
}
