package no.imr.korona.computation;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public abstract class BaseBufferedPingModule extends GeneralPingModule {
   protected BaseBufferedPingModule() {
   }

   public abstract static class BaseBufferedPingModuleComputation extends GeneralPingModuleComputation {
      private int pingsToBuffer = 0;
      private final Deque<Ping> pingOutputQueue = new ArrayDeque<>();

      protected BaseBufferedPingModuleComputation(BaseBufferedPingModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);
      }

      public @Nullable Ping pollFirstPingInBuffer() throws IOException {
         // Build up output queue until pingOutputQueue.size() == pingsToBuffer. (or next datagram == null)
         buildQueue();

         return pingOutputQueue.pollFirst();
      }

      private void buildQueue() throws IOException {
         while (pingOutputQueue.size() < pingsToBuffer + 1) {
            Ping ping = inputPing();
            if (ping == null) {
               break;
            }
            pingOutputQueue.addLast(ping);
         }
      }

      public void setPingsToBuffer(int pingsToBuffer) {
         this.pingsToBuffer = pingsToBuffer;
      }

      public Deque<Ping> getPingOutputQueue() {
         return pingOutputQueue;
      }
   }
}
