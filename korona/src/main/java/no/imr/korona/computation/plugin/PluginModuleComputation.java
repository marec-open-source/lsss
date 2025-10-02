package no.imr.korona.computation.plugin;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.computation.plugin.impl.ApiPing;
import no.imr.korona.computation.plugin.impl.ApiPingBuffer;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.compile.CompileException;
import no.marec.api.korona.ModuleComputation;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

final class PluginModuleComputation extends GeneralPingModuleComputation {
   private final ModuleComputation moduleComputation;
   private final ApiPingBuffer pingBuffer;

   PluginModuleComputation(PluginModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);

      try {
         moduleComputation = PluginModule.compile(module.implementation.getValue());
      } catch (CompileException e) {
         // Compilability ensured by parameter constraint.
         throw new ShouldNotHappenException(e);
      }
      pingBuffer = new ApiPingBuffer(moduleComputation.getPingBufferRadius());
   }

   @Override
   protected @Nullable Ping generateOutput() throws IOException {
      while (true) {
         if (getAsyncHandle().isCancelled()) {
            return null;
         }

         Ping ping = inputPing();
         if (ping != null) {
            ApiPing apiPing = new ApiPing(ping);
            pingBuffer.add(apiPing);
            if (!pingBuffer.isBufferingComplete()) {
               continue;
            }
         } else {
            if (pingBuffer.isDone()) {
               return null;
            }
         }

         ApiPing centerPing = pingBuffer.getPing(0);
         moduleComputation.compute(pingBuffer);
         centerPing.applyValidArray();
         pingBuffer.advanceCenter();
         return centerPing.getPing();
      }
   }
}
