package no.imr.korona.data.util;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingReader;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

public final class CancellablePingReader extends ForwardingPingReader {
   private final AsyncHandle readerAsyncHandle;

   public CancellablePingReader(PingReader pingReader, AsyncHandle readerAsyncHandle) {
      super(pingReader);

      this.readerAsyncHandle = readerAsyncHandle;
   }

   @Override
   public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
      if (readerAsyncHandle.isCancelled()) {
         return null;
      } else {
         return super.nextPing(asyncHandle);
      }
   }
}
